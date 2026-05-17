package ru.practicum.exception;

public class ActionTypeNotFound extends RuntimeException {
    public ActionTypeNotFound(String message) {
        super(message);
    }
}
