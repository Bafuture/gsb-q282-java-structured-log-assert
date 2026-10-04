package com.example.logassert;

public final class LogAssertionError extends AssertionError {

    public LogAssertionError(String message) {
        super(message);
    }
}
