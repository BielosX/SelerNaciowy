package org.selernaciowy.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAspectJAutoProxy
public class CacheConfiguration {

    @Bean
    @Profile({"local && !redis"})
    public Cache inMemoryCache() {
        return new InMemoryCache();
    }
}
