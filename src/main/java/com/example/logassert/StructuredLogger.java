package com.example.logassert;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StructuredLogger {

    private final LogCapture capture;
    private final Clock clock;

    public StructuredLogger(LogCapture capture) {
        this(capture, Clock.systemUTC());
    }

    public StructuredLogger(LogCapture capture, Clock clock) {
        this.capture = capture;
        this.clock = clock;
    }

    public void debug(String message, Object... keyValues) {
        emit(LogLevel.DEBUG, message, null, keyValues);
    }

    public void info(String message, Object... keyValues) {
        emit(LogLevel.INFO, message, null, keyValues);
    }

    public void warn(String message, Object... keyValues) {
        emit(LogLevel.WARN, message, null, keyValues);
    }

    public void error(String message, Object... keyValues) {
        emit(LogLevel.ERROR, message, null, keyValues);
    }

    public void error(String message, Throwable throwable, Object... keyValues) {
        emit(LogLevel.ERROR, message, throwable, keyValues);
    }

    public void log(LogLevel level, String message, Throwable throwable, Object... keyValues) {
        emit(level, message, throwable, keyValues);
    }

    private void emit(LogLevel level, String message, Throwable throwable, Object... keyValues) {
        capture.record(LogEntry.builder(level, message)
                .timestamp(Instant.now(clock))
                .fields(pairs(keyValues))
                .exception(throwable)
                .build());
    }

    private static Map<String, Object> pairs(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (keyValues == null) {
            return map;
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must contain an even number of elements");
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            if (!(keyValues[i] instanceof String key)) {
                throw new IllegalArgumentException("field key at index " + i + " must be a String");
            }
            map.put(key, keyValues[i + 1]);
        }
        return map;
    }
}
