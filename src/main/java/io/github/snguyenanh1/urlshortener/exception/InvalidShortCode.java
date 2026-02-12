package io.github.snguyenanh1.urlshortener.exception;

public class InvalidShortCode extends RuntimeException {
    public InvalidShortCode(String message) {
        super(message);
    }
}
