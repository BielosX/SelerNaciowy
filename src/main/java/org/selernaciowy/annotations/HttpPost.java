package org.selernaciowy.annotations;

import org.selernaciowy.HttpRequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@HttpMethod(method = HttpRequestMethod.POST)
public @interface HttpPost {
    String value();
}
