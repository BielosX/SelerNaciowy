package org.selernaciowy.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"local && !redis"})
public class LocalCacheConfiguration {

    @Bean
    public Cache inMemoryCache() {
        return new InMemoryCache();
    }
}
