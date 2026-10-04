package com.example.logassert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class LogMatcher {

    private record Condition(
            String expectation,
            Predicate<LogEntry> test,
            Function<LogEntry, String> mismatch) {
    }

    private final List<Condition> conditions;

    private LogMatcher(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public static LogMatcher any() {
        return new LogMatcher(new ArrayList<>());
    }

    public static LogMatcher level(LogLevel level) {
        return any().withLevel(level);
    }

    public static LogMatcher field(String key, Object expectedValue) {
        return any().withField(key, expectedValue);
    }

    public static LogMatcher fieldPresent(String key) {
        return any().withFieldPresent(key);
    }

    public static LogMatcher messageContaining(String substring) {
        return any().withMessageContaining(substring);
    }

    public static LogMatcher info() {
        return level(LogLevel.INFO);
    }

    public static LogMatcher warn() {
        return level(LogLevel.WARN);
    }

    public static LogMatcher error() {
        return level(LogLevel.ERROR);
    }

    public LogMatcher withLevel(LogLevel expected) {
        Objects.requireNonNull(expected, "expected level");
        return with(
                "level = " + expected,
                entry -> entry.level() == expected,
                entry -> "level expected " + expected + " but was " + entry.level());
    }

    public LogMatcher withMessageContaining(String substring) {
        Objects.requireNonNull(substring, "substring");
        return with(
                "message contains '" + substring + "'",
                entry -> entry.message().contains(substring),
                entry -> "message expected to contain '" + substring + "' but was '" + entry.message() + "'");
    }

    public LogMatcher withField(String key, Object expectedValue) {
        Objects.requireNonNull(key, "key");
        return with(
                "field '" + key + "' = " + quote(expectedValue),
                entry -> entry.hasField(key) && Objects.equals(entry.field(key), expectedValue),
                entry -> {
                    if (!entry.hasField(key)) {
                        return "field '" + key + "' is absent (expected " + quote(expectedValue) + ")";
                    }
                    return "field '" + key + "' expected " + quote(expectedValue)
                            + " but was " + quote(entry.field(key));
                });
    }

    public LogMatcher withFieldPresent(String key) {
        Objects.requireNonNull(key, "key");
        return with(
                "field '" + key + "' is present",
                entry -> entry.hasField(key),
                entry -> "field '" + key + "' is absent");
    }

    public LogMatcher withException(Class<? extends Throwable> expectedType) {
        Objects.requireNonNull(expectedType, "expectedType");
        return with(
                "exception of type " + expectedType.getSimpleName(),
                entry -> entry.throwable().map(expectedType::isInstance).orElse(false),
                entry -> entry.throwable()
                        .map(t -> "exception expected " + expectedType.getSimpleName()
                                + " but was " + t.getClass().getSimpleName())
                        .orElse("expected exception " + expectedType.getSimpleName()
                                + " but entry has no exception"));
    }

    public LogMatcher withExceptionTypeNamed(String typeName) {
        Objects.requireNonNull(typeName, "typeName");
        return with(
                "exception type named '" + typeName + "'",
                entry -> entry.exceptionTypeName()
                        .map(name -> name.equals(typeName) || name.endsWith("." + typeName))
                        .orElse(false),
                entry -> entry.exceptionTypeName()
                        .map(name -> "exception type expected '" + typeName + "' but was '" + name + "'")
                        .orElse("expected exception type '" + typeName + "' but entry has no exception"));
    }

    public LogMatcher with(String expectation, Predicate<LogEntry> test, Function<LogEntry, String> mismatch) {
        List<Condition> copy = new ArrayList<>(conditions);
        copy.add(new Condition(expectation, test, mismatch));
        return new LogMatcher(copy);
    }

    public boolean matches(LogEntry entry) {
        return conditions.stream().allMatch(c -> c.test().test(entry));
    }

    public List<String> mismatches(LogEntry entry) {
        List<String> reasons = new ArrayList<>();
        for (Condition condition : conditions) {
            if (!condition.test().test(entry)) {
                reasons.add(condition.mismatch().apply(entry));
            }
        }
        return reasons;
    }

    public List<String> expectations() {
        return conditions.stream().map(Condition::expectation).toList();
    }

    public String describe() {
        if (conditions.isEmpty()) {
            return "<any entry>";
        }
        return String.join(" AND ", expectations());
    }

    static List<String> immutableExpectationsOf(LogMatcher matcher) {
        return Collections.unmodifiableList(matcher.expectations());
    }

    private static String quote(Object value) {
        return value instanceof String ? "'" + value + "'" : String.valueOf(value);
    }
}
