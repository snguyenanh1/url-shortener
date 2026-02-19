package io.github.snguyenanh1.urlshortener.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            ShortCodeNotFoundException.class,
            UrlNotFoundException.class,
            UserNotFoundException.class
    })
    public ResponseEntity<Object> handleNotFoundException(RuntimeException ex, WebRequest request) {
        log.warn(ex.getMessage());
        var bodyOfResponse = Map.of( "error", 404,
                "message", ex.getMessage());
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({
            InvalidExpirationDateException.class,
            InvalidShortCodeException.class,
            InvalidUrlException.class,
            InvalidUsernameException.class,
            InvalidEmailException.class,
    })
    public ResponseEntity<Object> handleInvalidException(RuntimeException ex, WebRequest request) {
        log.warn(ex.getMessage());
        var bodyOfResponse = Map.of("error", 400,
                "message", ex.getMessage());
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({
            ExpiredUrlException.class
    })
    public ResponseEntity<Object> handleExpiredException(RuntimeException ex, WebRequest request) {
        log.warn(ex.getMessage());
        var bodyOfResponse = Map.of( "error", 410,
                "message", ex.getMessage());
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.GONE, request);
    }

    @ExceptionHandler({
            InvalidPasswordException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<Object> handleWrongPasswordException(RuntimeException ex, WebRequest request) {
        log.warn(ex.getMessage());
        var bodyOfResponse = Map.of("error", "403",
                "message", ex.getMessage());
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({
            Exception.class
    })
    public ResponseEntity<Object> handleOtherException(Exception ex, WebRequest request) {
        log.error(ex.getMessage(), ex);
        var bodyOfResponse = Map.of("error", 500,
                "message", "Internal Server Error");
        return super.handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }


    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn(ex.getMessage(), ex);
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
        return new ResponseEntity<>(getErrorsMap(errors), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    private Map<String, List<String>> getErrorsMap(List<String> errors) {
        Map<String, List<String>> errorResponse = new HashMap<>();
        errorResponse.put("errors", errors);
        return errorResponse;
    }

}
