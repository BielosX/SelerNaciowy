package org.selernaciowy.netty;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NettyFacade {
    private final NettyHttpServer server;

    public void start() throws Exception {
        server.start();
    }

    public void shutdownGracefully() {
        server.shutdownGracefully();
    }
}
