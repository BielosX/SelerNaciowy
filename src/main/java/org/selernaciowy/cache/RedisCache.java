package org.selernaciowy.cache;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;

import java.time.Duration;
import java.util.Optional;

@RequiredArgsConstructor
public class RedisCache implements Cache {
    private final JedisPooled pooled;
    private final Gson gson = new Gson();

    private record RedisEntry(String value, String className) {}

    @Override
    @SneakyThrows
    public Optional<Object> get(String key) {
        String value = pooled.get(key);
        if (value == null) {
            return Optional.empty();
        }
        RedisEntry entry = gson.fromJson(value, RedisEntry.class);
        Class<?> resultClass = Class.forName(entry.className());
        return Optional.of(gson.fromJson(entry.value(), resultClass));
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        String className = value.getClass().getName();
        RedisEntry entry = new RedisEntry(gson.toJson(value), className);
        pooled.set(key, gson.toJson(entry), SetParams.setParams());
        pooled.expire(key, ttl.getSeconds());
    }
}
