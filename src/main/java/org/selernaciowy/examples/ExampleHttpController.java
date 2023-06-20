package org.selernaciowy.examples;

import lombok.extern.slf4j.Slf4j;
import org.selernaciowy.annotations.HttpController;
import org.selernaciowy.annotations.HttpGet;
import org.selernaciowy.annotations.HttpPathPrefix;
import org.selernaciowy.annotations.HttpPost;
import org.selernaciowy.annotations.PathParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@HttpController
@HttpPathPrefix("/api/v1")
public class ExampleHttpController {

    @HttpGet("/hello")
    public String hello() {
        log.info("Hello from GET /hello");
        return "Hello";
    }

    @HttpPost("/test/post")
    public void postHandler() {
        log.info("Hello from POST /test/post");
    }

    @HttpPost("/users/:userId/books/:bookId")
    public void pathParamPost(@PathParam("userId") String userId,
                              @PathParam int bookId) {
        log.info("Hello from POST /users/{}/books/{}", userId, bookId);
    }
}
