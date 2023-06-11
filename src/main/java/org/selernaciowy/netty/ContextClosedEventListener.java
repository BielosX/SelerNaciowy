package org.selernaciowy.netty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ContextClosedEventListener {

    @EventListener
    public void handleContextCloseEvent(ContextClosedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        NettyHttpServer server = context.getBean(NettyHttpServer.class);
        server.shutdownGracefully();
    }
}
