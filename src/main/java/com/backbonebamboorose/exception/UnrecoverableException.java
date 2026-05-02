package com.backbonebamboorose.exception;

public class UnrecoverableException extends RuntimeException {

    public UnrecoverableException(String message) {
        super(message);
    }

    public UnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
