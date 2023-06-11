package org.selernaciowy.netty;

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
import io.netty.util.ReferenceCountUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.selernaciowy.HttpPathSegment;
import org.selernaciowy.HttpRequestMethod;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@ChannelHandler.Sharable
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {
    private final HttpPathSegment root = new HttpPathSegment();
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
    private static void invoke(HttpPathSegment.InvokerAndMethod invokerAndMethod) {
        invokerAndMethod.method().invoke(invokerAndMethod.invoker());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest request) {
            String responseString = "Hello";
            List<String> segments = Arrays.stream(request.uri().split("/"))
                    .filter(str -> !str.isBlank())
                    .toList();
            HttpRequestMethod requestMethod = methodsMapping.get(request.method());
            root.findMapping(requestMethod, segments)
                    .ifPresent(HttpRequestHandler::invoke);
            FullHttpResponse response = new DefaultFullHttpResponse(request.protocolVersion(),
                    HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer(responseString.getBytes(StandardCharsets.UTF_8)));
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            ChannelFuture future = ctx.write(response);
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
