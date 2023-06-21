package org.selernaciowy.netty;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
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
import org.selernaciowy.HttpHeaders;
import org.selernaciowy.HttpPathSegment;
import org.selernaciowy.HttpRequestMethod;
import org.selernaciowy.annotations.PathParam;
import org.selernaciowy.annotations.QueryParam;
import org.selernaciowy.annotations.RequestBody;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private void setUriParam(Object[] methodParams,
                                    int idx,
                                    Map<String,String> params,
                                    Parameter methodParameter,
                                    Annotation annotation) {
        if (annotation != null) {
            String paramName;
            Method valueMethod = annotation.annotationType().getMethod("value");
            String value = (String)valueMethod.invoke(annotation);
            if (value.equals("")) {
                paramName = methodParameter.getName();
            } else {
                paramName = value;
            }
            String paramValue = params.get(paramName);
            methodParams[idx] = conversionService.convert(paramValue, methodParameter.getType());
        }
    }

    @SneakyThrows
    private Object invoke(HttpPathSegment.InvokerAndMethod invokerAndMethod,
                          Map<String, String> pathParams,
                          ByteBuf content,
                          Map<String,String> queryParams,
                          HttpHeaders headers) {
        Parameter[] methodParams = invokerAndMethod.method().getParameters();
        Object[] resolvedParams = new Object[methodParams.length];
        for (int idx = 0; idx < methodParams.length; idx++) {
            Parameter methodParameter = methodParams[idx];
            PathParam pathParam = methodParameter.getAnnotation(PathParam.class);
            setUriParam(resolvedParams, idx, pathParams, methodParameter, pathParam);
            RequestBody requestBody = methodParameter.getAnnotation(RequestBody.class);
            if (requestBody != null) {
                resolvedParams[idx] = readContent(content, methodParameter.getType());
            }
            QueryParam queryParam = methodParameter.getAnnotation(QueryParam.class);
            setUriParam(resolvedParams, idx, queryParams, methodParameter, queryParam);
            if (methodParameter.getType().equals(HttpHeaders.class)) {
                resolvedParams[idx] = headers;
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

    private static FullHttpResponse serverError(HttpVersion version) {
        return new DefaultFullHttpResponse(version, HttpResponseStatus.INTERNAL_SERVER_ERROR);
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

    private <T> T readContent(ByteBuf content, Class<T> cls) {
        int bufferLen = content.readableBytes();
        byte[] bytes = new byte[bufferLen];
        content.readBytes(bytes);
        String payload = new String(bytes, StandardCharsets.UTF_8);
        return gson.fromJson(payload, cls);
    }

    private record QueryParameter(String key, String value) {
        static QueryParameter fromString(String str) {
            String[] result = str.split("=");
            return new QueryParameter(result[0], result[1]);
        }
    }

    private static Map<String,String> getQueryParams(String uri) {
        String[] splitResult = uri.split("\\?");
        if (splitResult.length > 1) {
            return Arrays.stream(splitResult[1].split("&"))
                    .map(QueryParameter::fromString)
                    .collect(Collectors.toMap(QueryParameter::key, QueryParameter::value));
        }
        return Collections.emptyMap();
    }

    private static String getPath(String uri) {
        return uri.split("\\?")[0];
    }

    private static HttpHeaders getHttpHeaders(FullHttpRequest request) {
        Map<String, List<String>> headers = request.headers()
                .entries()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(),
                        e -> Stream.of(e.getValue()), Stream::concat))
                .entrySet()
                .stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue().toList()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return new HttpHeaders(headers);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest request) {
            String path = getPath(request.uri());
            List<String> segments = Arrays.stream(path.split("/"))
                    .filter(str -> !str.isBlank())
                    .toList();
            Map<String,String> queryParams = getQueryParams(request.uri());
            HttpRequestMethod requestMethod = methodsMapping.get(request.method());
            Map<String,String> pathParams = new HashMap<>();
            HttpHeaders headers = getHttpHeaders(request);
            ChannelFuture future = root.findMapping(requestMethod, segments, pathParams)
                    .map(invokerAndMethod -> {
                        try {
                            Object result = invoke(invokerAndMethod,
                                    pathParams,
                                    request.content(),
                                    queryParams,
                                    headers);
                            Class<?> returnType = invokerAndMethod.method().getReturnType();
                            if (returnType.equals(void.class) || returnType.equals(Void.class)) {
                                return ctx.write(emptyResponse(request.protocolVersion()));
                            } else {
                                String responseString = gson.toJson(result);
                                return ctx.write(withBody(request.protocolVersion(), responseString));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            return ctx.write(serverError(request.protocolVersion()));
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
