package org.selernaciowy.examples;

import org.selernaciowy.cache.Cached;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExampleService {
    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    @Cached(ttl = "PT20S")
    public Optional<String> getNameById(UUID id) {
        return Optional.ofNullable(names.get(id));
    }

    public void setName(UUID id, String name) {
        names.put(id, name);
    }
}
