package com.example.logassert;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LogEntry {

    private final LogLevel level;
    private final Instant timestamp;
    private final String message;
    private final Map<String, Object> fields;
    private final Throwable throwable;

    private LogEntry(Builder builder) {
        this.level = Objects.requireNonNull(builder.level, "level");
        this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp");
        this.message = Objects.requireNonNull(builder.message, "message");
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(builder.fields));
        this.throwable = builder.throwable;
    }

    public static Builder builder(LogLevel level, String message) {
        return new Builder(level, message);
    }

    public static LogEntry of(LogLevel level, String message) {
        return builder(level, message).build();
    }

    public LogLevel level() {
        return level;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public String message() {
        return message;
    }

    public Map<String, Object> fields() {
        return fields;
    }

    public boolean hasField(String key) {
        return fields.containsKey(key);
    }

    public Object field(String key) {
        return fields.get(key);
    }

    public Optional<Throwable> throwable() {
        return Optional.ofNullable(throwable);
    }

    public Optional<String> exceptionTypeName() {
        return throwable().map(t -> t.getClass().getName());
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(timestamp).append(' ');
        sb.append(String.format("%-5s", level)).append(' ');
        sb.append(message);
        if (!fields.isEmpty()) {
            sb.append(' ').append(fields);
        }
        if (throwable != null) {
            sb.append(" | exception=").append(throwable.getClass().getSimpleName());
            if (throwable.getMessage() != null) {
                sb.append(": ").append(throwable.getMessage());
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return render();
    }

    public static final class Builder {
        private final LogLevel level;
        private final String message;
        private Instant timestamp = Instant.now();
        private final Map<String, Object> fields = new LinkedHashMap<>();
        private Throwable throwable;

        private Builder(LogLevel level, String message) {
            this.level = level;
            this.message = message;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder field(String key, Object value) {
            this.fields.put(key, value);
            return this;
        }

        public Builder fields(Map<String, ?> values) {
            this.fields.putAll(values);
            return this;
        }

        public Builder exception(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
