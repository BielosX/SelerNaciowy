package org.selernaciowy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpPathSegment {
    private final Map<HttpRequestMethod, InvokerAndMethod> methods;
    private final Map<String, HttpPathSegment> children;

    public HttpPathSegment() {
        this.methods = new HashMap<>();
        this.children = new HashMap<>();
    }

    private static <T> T head(List<T> list) {
        return list.get(0);
    }

    private static <T> List<T> tail(List<T> list) {
        return list.subList(1, list.size());
    }

    public void registerMethod(HttpRequestMethod requestMethod,
                               List<String> pathSegments,
                               Method method,
                               Object invoker) {
        if (pathSegments.isEmpty()) {
            methods.put(requestMethod, new InvokerAndMethod(invoker, method));
        } else {
            String head = head(pathSegments);
            List<String> tail = tail(pathSegments);
            Optional<HttpPathSegment> segment = Optional.ofNullable(children.get(head));
            segment.ifPresentOrElse(child -> child.registerMethod(requestMethod, tail, method, invoker), () -> {
                HttpPathSegment newSegment = new HttpPathSegment();
                newSegment.registerMethod(requestMethod, tail, method, invoker);
                this.children.put(head, newSegment);
            });
        }
    }

    public Optional<InvokerAndMethod> findMapping(HttpRequestMethod method, List<String> pathSegments) {
        if (pathSegments.isEmpty()) {
            return Optional.ofNullable(methods.get(method));
        }
        String head = head(pathSegments);
        List<String> tail = tail(pathSegments);
        return Optional.ofNullable(children.get(head))
                .flatMap(child -> child.findMapping(method, tail));
    }

    public record InvokerAndMethod(Object invoker, Method method) {}
}
