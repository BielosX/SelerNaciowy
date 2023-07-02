package org.selernaciowy.cache;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import redis.clients.jedis.JedisPooled;

@Configuration
@Profile({"redis && !local"})
public class RedisCacheConfiguration {
    private final static DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private final static int REDIS_PORT = 6379;

    @RequiredArgsConstructor
    public static class RedisContainer {
        @Delegate
        private final GenericContainer<?> redisContainer;
    }

    @Bean
    public RedisContainer redisContainer() {
        GenericContainer<?> container = new GenericContainer<>(REDIS_IMAGE);
        RedisContainer redisContainer = new RedisContainer(container);
        redisContainer.addExposedPort(REDIS_PORT);
        redisContainer.start();
        return redisContainer;
    }

    @Bean
    public Cache redisCache(RedisContainer redisContainer) {
        JedisPooled jedisPooled = new JedisPooled(redisContainer.getHost(),
                redisContainer.getMappedPort(REDIS_PORT));
        return new RedisCache(jedisPooled);
    }
}
