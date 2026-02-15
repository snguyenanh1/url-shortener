package io.github.snguyenanh1.urlshortener.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            ShortCodeNotFoundException.class,
            UrlNotFoundException.class,
    })
    ResponseEntity<Object> handleNotFoundException(RuntimeException ex, WebRequest request) {
        var bodyOfResponse = Map.of( "error", 404,
                "message", ex.getMessage());
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({
            InvalidExpirationDateException.class,
            InvalidShortCodeException.class,
            InvalidUrlException.class
    })
    ResponseEntity<Object> handleInvalidException(RuntimeException ex, WebRequest request) {
        var bodyOfResponse = Map.of("error", 400,
                "message", ex.getMessage());
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({
            ExpiredUrlException.class
    })
    ResponseEntity<Object> handleExpiredException(RuntimeException ex, WebRequest request) {
        var bodyOfResponse = Map.of( "error", 410,
                "message", ex.getMessage());
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.GONE, request);
    }
}
