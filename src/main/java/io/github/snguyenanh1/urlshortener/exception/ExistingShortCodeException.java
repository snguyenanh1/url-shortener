package io.github.snguyenanh1.urlshortener.exception;

public class ExistingShortCodeException extends RuntimeException {
    public ExistingShortCodeException(String message) {
        super(message);
    }
}
