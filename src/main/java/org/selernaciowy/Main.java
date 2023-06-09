package org.selernaciowy;

import lombok.extern.slf4j.Slf4j;
import org.selernaciowy.netty.NettyFacade;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(SelerNaciowyConfiguration.class);
        Runtime.getRuntime().addShutdownHook(new Thread(context::close));
        NettyFacade facade = context.getBean(NettyFacade.class);
        facade.start();
    }
}
