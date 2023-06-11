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
    public HttpRequestHandler requestHandler() {
        return new HttpRequestHandler();
    }

    @Bean
    public NettyHttpServer httpServer(HttpRequestHandler handler) {
        HttpServerInitializer initializer = new HttpServerInitializer(handler, maxContentLength);
        return new NettyHttpServer(workers, port, initializer);
    }
}
