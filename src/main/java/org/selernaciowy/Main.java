package org.selernaciowy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class Main {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(SelerNaciowyConfiguration.class);
        Runtime.getRuntime().addShutdownHook(new Thread(context::close));
    }
}
