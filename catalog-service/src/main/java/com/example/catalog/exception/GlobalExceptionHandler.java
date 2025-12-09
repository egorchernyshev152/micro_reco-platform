package com.example.catalog.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error("Not Found")
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDbConstraint(DataIntegrityViolationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error("Conflict")
                        .message("Constraint violation")
                        .path(req.getRequestURI())
                        .build()
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error("Conflict")
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Validation Failed")
                        .message(msg)
                        .path(req.getRequestURI())
                        .build()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Constraint Violation")
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .error("Internal Server Error")
                        .message(ex.getMessage())
                        .path(req.getRequestURI())
                        .build()
        );
    }
}
