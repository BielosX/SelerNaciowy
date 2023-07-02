package org.selernaciowy.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {
    private final Cache cache;

    // Any public method annotated with @Cached
    @Pointcut("execution(public * *(..)) && @annotation(Cached)")
    public void cachedMethod() {}

    @Around("cachedMethod() && @annotation(cached)")
    public Object invokeOrGetCached(ProceedingJoinPoint pjp, Cached cached) throws Throwable {
        String key = Arrays.stream(pjp.getArgs())
                .map(Object::toString)
                .reduce("/", (current, elem) -> current + elem + "/");
        Optional<Object> cachedResult =  cache.get(key);
        if (cachedResult.isPresent()) {
            log.info("Returning result from cache");
            return cachedResult.get();
        } else {
            log.info("Calculating result");
            Object result = pjp.proceed();
            if (result == null) {
                log.info("Method returned null");
                return null;
            }
            cache.set(key, result, Duration.parse(cached.ttl()));
            return result;
        }
    }
}
