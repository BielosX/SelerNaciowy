package org.selernaciowy.netty;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.selernaciowy.HttpPathSegment;
import org.selernaciowy.HttpRequestMethod;
import org.selernaciowy.annotations.PathParam;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@ChannelHandler.Sharable
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
    private final HttpPathSegment root = new HttpPathSegment();
    private final Gson gson = new Gson();
    private final ConversionService conversionService = DefaultConversionService.getSharedInstance();
    private final static Map<HttpMethod, HttpRequestMethod> methodsMapping = Map.of(
            HttpMethod.GET, HttpRequestMethod.GET,
            HttpMethod.POST, HttpRequestMethod.POST
    );

    public void registerMethod(HttpRequestMethod requestMethod,
                               List<String> pathSegments,
                               Method method,
                               Object invoker) {
        root.registerMethod(requestMethod, pathSegments, method, invoker);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @SneakyThrows
    private Object invoke(HttpPathSegment.InvokerAndMethod invokerAndMethod,
                                 Map<String, String> pathParams) {
        Parameter[] methodParams = invokerAndMethod.method().getParameters();
        Object[] resolvedParams = new Object[methodParams.length];
        for (int idx = 0; idx < methodParams.length; idx++) {
            Parameter methodParameter = methodParams[idx];
            PathParam pathParam = methodParameter.getAnnotation(PathParam.class);
            if (pathParam != null) {
                String paramName;
                if (pathParam.value().equals("")) {
                    paramName = methodParameter.getName();
                } else {
                    paramName = pathParam.value();
                }
                String paramValue = pathParams.get(paramName);
                resolvedParams[idx] = conversionService.convert(paramValue, methodParameter.getType());
            }
        }
        return invokerAndMethod.method().invoke(invokerAndMethod.invoker(), resolvedParams);
    }

    private static FullHttpResponse notFound(HttpVersion version) {
        return new DefaultFullHttpResponse(version, HttpResponseStatus.NOT_FOUND);
    }

    private static FullHttpResponse emptyResponse(HttpVersion version) {
        return new DefaultFullHttpResponse(version, HttpResponseStatus.NO_CONTENT);
    }

    private static FullHttpResponse withBody(HttpVersion version, String body) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(version,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bodyBytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.length);
        return response;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest request) {
            List<String> segments = Arrays.stream(request.uri().split("/"))
                    .filter(str -> !str.isBlank())
                    .toList();
            HttpRequestMethod requestMethod = methodsMapping.get(request.method());
            Map<String,String> pathParams = new HashMap<>();
            ChannelFuture future = root.findMapping(requestMethod, segments, pathParams)
                    .map(invokerAndMethod -> {
                        Object result = invoke(invokerAndMethod, pathParams);
                        Class<?> returnType = invokerAndMethod.method().getReturnType();
                        if (returnType.equals(void.class) || returnType.equals(Void.class)) {
                            return ctx.write(emptyResponse(request.protocolVersion()));
                        } else {
                            String responseString = gson.toJson(result);
                            return ctx.write(withBody(request.protocolVersion(), responseString));
                        }
                    })
                    .orElseGet(() -> ctx.write(notFound(request.protocolVersion())));
            future.addListener(ChannelFutureListener.CLOSE);
            ReferenceCountUtil.release(msg);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
