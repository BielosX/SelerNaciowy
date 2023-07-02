package org.selernaciowy.examples;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.selernaciowy.HttpHeaders;
import org.selernaciowy.annotations.*;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@HttpController
@HttpPathPrefix("/api/v1")
@RequiredArgsConstructor
public class ExampleHttpController {
    private final ExampleService service;

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

    private record ExampleBody(int count, String name) {}
    private record ExampleResponse(String number, String text) {}

    @HttpPost("/users")
    public ExampleResponse requestBodyPost(@RequestBody ExampleBody body) {
        log.info("Hello from /users, count: {}, name: {}", body.count(), body.name());
        return new ExampleResponse(String.valueOf(body.count()), body.name());
    }

    @HttpPost("/users/query")
    public void requestQueryParam(@QueryParam int number,
                                  @QueryParam("client_id") String clientId) {
        log.info("Hello from /users/query, number: {}, clientId: {}", number, clientId);
    }

    @HttpGet("/error")
    public void serverError() {
        log.info("Hello from /error");
        throw new IllegalStateException();
    }

    @HttpGet("/headers")
    public String getHeader(HttpHeaders headers) {
        log.info("Hello from /headers");
        return headers.get("content-type")
                .stream().findAny()
                .orElse("");
    }

    @HttpGet("/cached/:id")
    public String getCachedName(@PathParam UUID id) {
        String value = service.getNameById(id);
        return Objects.requireNonNullElse(value, "");
    }

    private record SetNameRequest(String name) {}

    @HttpPost("/cached")
    public UUID setName(@RequestBody SetNameRequest body) {
        UUID id = UUID.randomUUID();
        service.setName(id, body.name());
        return id;
    }
}
