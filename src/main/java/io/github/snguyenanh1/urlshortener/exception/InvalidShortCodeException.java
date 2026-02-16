package io.github.snguyenanh1.urlshortener.exception;

public class InvalidShortCodeException extends RuntimeException {
    public InvalidShortCodeException(String message) {
        super(message);
    }
}
