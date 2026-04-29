package ru.practicum.exception;

public class IllegalEventUpdateException extends RuntimeException {
    public IllegalEventUpdateException(String message) {
        super(message);
    }
}
