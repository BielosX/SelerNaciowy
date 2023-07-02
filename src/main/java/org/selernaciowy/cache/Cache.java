package org.selernaciowy.cache;

import java.time.Duration;
import java.util.Optional;

public interface Cache {
    Optional<Object> get(String key);
    void set(String key, Object value, Duration ttl);
}
