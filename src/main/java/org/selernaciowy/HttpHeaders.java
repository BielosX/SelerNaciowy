package org.selernaciowy;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record HttpHeaders (Map<String, List<String>> headers) {
    public List<String> get(String header) {
        return Optional.ofNullable(headers.get(header.toLowerCase()))
                .orElse(Collections.emptyList());
    }
}
