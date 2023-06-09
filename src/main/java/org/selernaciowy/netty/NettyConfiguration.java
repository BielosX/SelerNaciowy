package org.selernaciowy.netty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class NettyConfiguration {
    @Value("${netty.maxContentLength}")
    private int maxContentLength;
    @Value("${netty.workers}")
    private int workers;
    @Value("${server.port}")
    private int port;

    @Bean
    public NettyFacade nettyFacade() {
        HttpRequestHandler handler = new HttpRequestHandler();
        HttpServerInitializer initializer = new HttpServerInitializer(handler, maxContentLength);
        NettyHttpServer server = new NettyHttpServer(workers, port, initializer);
        return new NettyFacade(server);
    }
}
