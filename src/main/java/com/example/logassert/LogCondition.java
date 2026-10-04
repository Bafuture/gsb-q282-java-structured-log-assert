package com.example.logassert;

/**
 * A predicate over a {@link LogEntry} with a human-readable description used
 * in failure messages. Compose with {@link #and(LogCondition)} and
 * {@link #negate()}, or build common cases via {@link Conditions}.
 */
@FunctionalInterface
public interface LogCondition {

    boolean matches(LogEntry entry);

    /** Human-readable description rendered in assertion failure messages. */
    default String describe() {
        return "custom condition";
    }

    default LogCondition and(LogCondition other) {
        LogCondition self = this;
        return new LogCondition() {
            @Override
            public boolean matches(LogEntry entry) {
                return self.matches(entry) && other.matches(entry);
            }

            @Override
            public String describe() {
                return "(" + self.describe() + " AND " + other.describe() + ")";
            }
        };
    }

    default LogCondition negate() {
        LogCondition self = this;
        return new LogCondition() {
            @Override
            public boolean matches(LogEntry entry) {
                return !self.matches(entry);
            }

            @Override
            public String describe() {
                return "NOT (" + self.describe() + ")";
            }
        };
    }
}
