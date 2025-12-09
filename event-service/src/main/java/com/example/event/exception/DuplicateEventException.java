package com.example.event.exception;

public class DuplicateEventException extends RuntimeException {
    public DuplicateEventException(String message) {
        super(message);
    }
}
