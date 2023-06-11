package org.selernaciowy;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.selernaciowy.annotations.HttpController;
import org.selernaciowy.annotations.HttpMethod;
import org.selernaciowy.annotations.HttpPathPrefix;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class HttpAnnotationsProcessor implements BeanPostProcessor {
    private static final String VALUE_METHOD_NAME = "value";

    private static boolean isHttpMethod(Annotation annotation) {
        return annotation.annotationType().isAnnotationPresent(HttpMethod.class);
    }

    private static HttpRequestMethod getMethod(Annotation annotation) {
        return annotation.annotationType().getAnnotation(HttpMethod.class).method();
    }

    @SneakyThrows
    private static String getValue(Annotation annotation) {
        return (String)annotation.annotationType().getMethod(VALUE_METHOD_NAME).invoke(annotation);
    }

    private record HttpMethodMapping(HttpRequestMethod httpMethod, Annotation annotation, Method method) {}

    private static Optional<HttpMethodMapping> getHttpMethodAnnotation(Method method) {
        return Arrays.stream(method.getAnnotations())
                .filter(HttpAnnotationsProcessor::isHttpMethod)
                .findFirst()
                .map(annotation -> new HttpMethodMapping(getMethod(annotation), annotation, method));
    }

    private static void processMapping(HttpMethodMapping mapping, String prefix) {
        String path = prefix + getValue(mapping.annotation());
        log.info("Found {} mapping. Path: {}, method: {}",
                mapping.httpMethod(),
                path,
                mapping.method().getName());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> cls = bean.getClass();
        if (cls.isAnnotationPresent(HttpController.class)) {
            log.info("{} bean registered as HttpController", beanName);
            String pathPrefix = Optional.ofNullable(cls.getAnnotation(HttpPathPrefix.class))
                    .map(HttpPathPrefix::value)
                    .orElse("");
            List<HttpMethodMapping> mappings = Arrays.stream(cls.getMethods())
                    .flatMap(method -> getHttpMethodAnnotation(method)
                            .map(Collections::singletonList)
                            .orElse(Collections.emptyList()).stream())
                    .toList();
            mappings.forEach(mapping -> processMapping(mapping, pathPrefix));
        }
        return bean;
    }
}
