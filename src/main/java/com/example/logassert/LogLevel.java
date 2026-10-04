package com.example.logassert;

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    public boolean isAtLeast(LogLevel other) {
        return ordinal() >= other.ordinal();
    }
}
