package com.example.logassert;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal structured logger. Entries are delivered to every registered sink
 * (see {@link LogCaptor}); without sinks, logging is a no-op.
 *
 * <p>Obtain instances via {@link #get(Class)} / {@link #get(String)}.
 */
public final class StructuredLogger {

    @FunctionalInterface
    interface Sink {
        void accept(LogEntry entry);
    }

    private static final Map<String, StructuredLogger> CACHE = new ConcurrentHashMap<>();
    private static final List<Sink> SINKS = new CopyOnWriteArrayList<>();
    private static volatile Clock clock = Clock.systemUTC();

    private final String name;

    private StructuredLogger(String name) {
        this.name = name;
    }

    public static StructuredLogger get(Class<?> type) {
        return get(type.getName());
    }

    public static StructuredLogger get(String name) {
        return CACHE.computeIfAbsent(name, StructuredLogger::new);
    }

    static void addSink(Sink sink) {
        SINKS.add(sink);
    }

    static void removeSink(Sink sink) {
        SINKS.remove(sink);
    }

    /** Test hook: inject a fixed clock. Visible for tests. */
    static void setClock(Clock newClock) {
        clock = newClock;
    }

    /** Test hook: restore the system clock. Visible for tests. */
    static void resetClock() {
        clock = Clock.systemUTC();
    }

    public String name() {
        return name;
    }

    public EventBuilder at(Level level) {
        return new EventBuilder(level);
    }

    public void trace(String message, Object... keyValues) {
        emit(Level.TRACE, message, null, keyValues);
    }

    public void debug(String message, Object... keyValues) {
        emit(Level.DEBUG, message, null, keyValues);
    }

    public void info(String message, Object... keyValues) {
        emit(Level.INFO, message, null, keyValues);
    }

    public void error(String message, Object... keyValues) {
        emit(Level.ERROR, message, null, keyValues);
    }

    public void warn(String message, Object... keyValues) {
        emit(Level.WARN, message, null, keyValues);
    }

    public void error(String message, Throwable thrown) {
        emit(Level.ERROR, message, thrown);
    }

    public void error(String message, Throwable thrown, Object... keyValues) {
        emit(Level.ERROR, message, thrown, keyValues);
    }

    private void emit(Level level, String message, Throwable thrown, Object... keyValues) {
        LogEntry entry = new LogEntry(
                level,
                Instant.now(clock),
                name,
                message,
                parseKeyValues(keyValues),
                thrown);
        for (Sink sink : SINKS) {
            sink.accept(entry);
        }
    }

    static Map<String, Object> parseKeyValues(Object... keyValues) {
        Map<String, Object> fields = new java.util.LinkedHashMap<>();
        if (keyValues == null) {
            return fields;
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "key/value pairs must be even in number, got " + keyValues.length);
        }
        for (int i = 0; i < keyValues.length; i += 2) {
            Object key = keyValues[i];
            if (!(key instanceof String)) {
                throw new IllegalArgumentException(
                        "field keys must be Strings, got: " + key);
            }
            fields.put((String) key, keyValues[i + 1]);
        }
        return fields;
    }

    /** Fluent builder for a single log event. */
    public final class EventBuilder {
        private final Level level;
        private final List<Object> keyValues = new ArrayList<>();
        private Throwable thrown;

        private EventBuilder(Level level) {
            this.level = level;
        }

        public EventBuilder field(String key, Object value) {
            keyValues.add(key);
            keyValues.add(value);
            return this;
        }

        public EventBuilder thrown(Throwable t) {
            this.thrown = t;
            return this;
        }

        public void log(String message) {
            emit(level, message, thrown, keyValues.toArray());
        }
    }
}
