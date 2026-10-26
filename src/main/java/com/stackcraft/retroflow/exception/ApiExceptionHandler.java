package com.stackcraft.retroflow.exception;

import com.stackcraft.retroflow.dto.ErrorMessage;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleInvalidRequest(MethodArgumentNotValidException exception) {
        String description = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", description);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> handleConstraintViolation(ConstraintViolationException exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class})
    public ResponseEntity<ErrorMessage> handleBadRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request: " + exception.getMessage());
    }

    @ExceptionHandler(RetroflowException.class)
    public ResponseEntity<ErrorMessage> handleBusinessRule(RetroflowException exception) {
        return error(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", exception.getMessage());
    }

    @ExceptionHandler({NotFoundException.class, EntityNotFoundException.class, NoSuchElementException.class})
    public ResponseEntity<ErrorMessage> handleNotFound(RuntimeException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorMessage> handleDataConflict(DataIntegrityViolationException exception) {
        return error(HttpStatus.BAD_REQUEST, "DATA_CONFLICT", "The request conflicts with existing data");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorMessage> handleNotImplemented(UnsupportedOperationException exception) {
        return error(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", exception.getMessage());
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorMessage> error(HttpStatus status, String code, String description) {
        return ResponseEntity.status(status).body(new ErrorMessage(code, description));
    }
}
