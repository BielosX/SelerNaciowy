package org.selernaciowy;

import org.selernaciowy.netty.NettyFacade;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ContextRefreshedEventListener {
    @EventListener
    public void handleContextRefreshEvent(ContextRefreshedEvent event) throws Exception {
        ApplicationContext context = event.getApplicationContext();
        NettyFacade facade = context.getBean(NettyFacade.class);
        facade.start();
    }
}
