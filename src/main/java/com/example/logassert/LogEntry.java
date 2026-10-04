package com.example.logassert;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * One captured structured log entry: level, timestamp, message, key/value
 * fields and (optionally) an attached exception.
 */
public record LogEntry(
        Level level,
        Instant timestamp,
        String loggerName,
        String message,
        Map<String, Object> fields,
        Throwable thrown) {

    public LogEntry {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(loggerName, "loggerName");
        Objects.requireNonNull(message, "message");
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    public boolean hasField(String key) {
        return fields.containsKey(key);
    }

    public Object field(String key) {
        return fields.get(key);
    }

    /** Simple class name of the attached exception, or null when absent. */
    public String thrownType() {
        return thrown == null ? null : thrown.getClass().getSimpleName();
    }
}
