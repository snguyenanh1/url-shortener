package io.github.snguyenanh1.urlshortener.exception;

public class InvalidUsernameException extends RuntimeException {
  public InvalidUsernameException(String message) {
    super(message);
  }
}
