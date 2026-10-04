package com.example.logassert;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Factory methods for common {@link LogCondition}s: level, message,
 * key/value field matching, field presence and exception type.
 */
public final class Conditions {

    private Conditions() {
    }

    public static LogCondition level(Level level) {
        return named("level=" + level, e -> e.level() == level);
    }

    public static LogCondition levelAtLeast(Level level) {
        return named("level>=" + level, e -> e.level().isAtLeast(level));
    }

    public static LogCondition messageContains(String substring) {
        return named("message contains '" + substring + "'",
                e -> e.message().contains(substring));
    }

    public static LogCondition field(String key, Object expectedValue) {
        return named("field[" + key + "]=" + LogRenderer.renderValue(expectedValue),
                e -> e.hasField(key) && Objects.equals(e.field(key), expectedValue));
    }

    public static LogCondition fieldWhere(String key, Predicate<Object> predicate) {
        return named("field[" + key + "] satisfies predicate",
                e -> e.hasField(key) && predicate.test(e.field(key)));
    }

    public static LogCondition hasField(String key) {
        return named("has field[" + key + "]", e -> e.hasField(key));
    }

    public static LogCondition missingField(String key) {
        return named("missing field[" + key + "]", e -> !e.hasField(key));
    }

    public static LogCondition exceptionIs(Class<? extends Throwable> type) {
        return named("exception is " + type.getSimpleName(),
                e -> e.thrown() != null && type.isInstance(e.thrown()));
    }

    private static LogCondition named(String description, Predicate<LogEntry> predicate) {
        return new LogCondition() {
            @Override
            public boolean matches(LogEntry entry) {
                return predicate.test(entry);
            }

            @Override
            public String describe() {
                return description;
            }
        };
    }
}
