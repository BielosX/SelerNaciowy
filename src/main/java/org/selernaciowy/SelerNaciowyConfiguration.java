package org.selernaciowy;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ComponentScan("org.selernaciowy")
public class SelerNaciowyConfiguration {
    @Bean
    public static YamlPropertiesProvider yamlPropertiesProvider() {
        return new YamlPropertiesProvider();
    }

    @Bean
    @DependsOn({"yamlPropertiesProvider"})
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

}
