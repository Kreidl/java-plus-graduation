package ru.practicum.exception;

public class FallbackException extends RuntimeException {
    public FallbackException(String message) {
        super(message);
    }
}
