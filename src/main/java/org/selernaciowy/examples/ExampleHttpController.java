package org.selernaciowy.examples;

import org.selernaciowy.annotations.HttpController;
import org.selernaciowy.annotations.HttpGet;
import org.selernaciowy.annotations.HttpPathPrefix;
import org.selernaciowy.annotations.HttpPost;
import org.springframework.stereotype.Component;

@Component
@HttpController
@HttpPathPrefix("/api/v1")
public class ExampleHttpController {

    @HttpGet("/hello")
    public String hello() {
        return "Hello";
    }

    @HttpPost("/test/post")
    public void postHandler() {

    }
}
