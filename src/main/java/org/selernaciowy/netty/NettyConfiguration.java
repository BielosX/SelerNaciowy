package org.selernaciowy.netty;

import lombok.Setter;
import org.selernaciowy.annotations.PropertyValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Setter
public class NettyConfiguration {
    @PropertyValue("${netty.maxContentLength}")
    private int maxContentLength;
    @PropertyValue("${netty.workers}")
    private int workers;
    @PropertyValue("${server.port}")
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
