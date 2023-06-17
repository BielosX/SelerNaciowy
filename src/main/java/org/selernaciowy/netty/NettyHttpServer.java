package org.selernaciowy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
@RequiredArgsConstructor
public class NettyHttpServer implements InitializingBean {
    private final int workers;
    private final int port;
    private final HttpServerInitializer initializer;

    private EventLoopGroup producerGroup;
    private EventLoopGroup workersGroup;

    public void start() throws Exception {
        this.producerGroup = new NioEventLoopGroup(1);
        this.workersGroup = new NioEventLoopGroup(workers);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(producerGroup, workersGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(initializer);
        Channel channel = bootstrap.bind(port).sync().channel();
        log.info("Netty Server started. Listening on port: {}", port);
        channel.closeFuture().sync();
    }

    public void shutdownGracefully() {
        producerGroup.shutdownGracefully();
        workersGroup.shutdownGracefully();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }
}
