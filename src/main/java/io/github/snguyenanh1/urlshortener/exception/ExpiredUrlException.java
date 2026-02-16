package io.github.snguyenanh1.urlshortener.exception;

public class ExpiredUrlException extends RuntimeException {
    public ExpiredUrlException(String message) {
        super(message);
    }
}
