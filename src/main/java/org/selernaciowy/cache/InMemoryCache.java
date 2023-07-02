package org.selernaciowy.cache;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class InMemoryCache implements Cache {
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    private record CacheEntry(Object value, Instant expireTime) {}

    @Override
    public Optional<Object> get(String key) {
        cache.computeIfPresent(key, (currentKey, currentValue) -> {
            if (clock.instant().isAfter(currentValue.expireTime())) {
                log.info("Entry for key {} expired. Invalidating", key);
                return null;
            } else {
                return currentValue;
            }
        });
        return Optional.ofNullable(cache.get(key)).map(CacheEntry::value);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        cache.put(key, new CacheEntry(value, clock.instant().plus(ttl)));
    }
}
