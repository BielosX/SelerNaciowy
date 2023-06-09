package org.selernaciowy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class Main {
    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(SelerNaciowyConfiguration.class);
        AppFacade facade = context.getBean(AppFacade.class);
        log.info("Log log");
        facade.run();
    }
}
