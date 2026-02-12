package io.github.snguyenanh1.urlshortener.exception;

public class DuplicateShortCodeException extends RuntimeException {
    public DuplicateShortCodeException(String message) {
        super(message);
    }
}
