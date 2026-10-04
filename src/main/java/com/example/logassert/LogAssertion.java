package com.example.logassert;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fluent assertions over a snapshot of captured {@link LogEntry}s.
 * Every terminal method increments the session's assertion counter, and
 * failures throw {@link AssertionError} with a readable report containing the
 * relevant entries and the mismatch reason (never just a boolean).
 */
public final class LogAssertion {

    private final List<LogEntry> entries;
    private final AssertionStats stats;
    private final String scope;

    LogAssertion(List<LogEntry> entries, AssertionStats stats, String scope) {
        this.entries = List.copyOf(entries);
        this.stats = stats;
        this.scope = scope;
    }

    /** Entries visible to this assertion, in emission order. */
    public List<LogEntry> entries() {
        return entries;
    }

    /** Narrows to entries satisfying an additional condition. */
    public LogAssertion filter(LogCondition condition) {
        List<LogEntry> narrowed = select(condition);
        return new LogAssertion(narrowed, stats,
                scope + " filtered by [" + condition.describe() + "]");
    }

    /**
     * Narrows to entries belonging to the same request/trace: entries that
     * carry the given correlation field with the given value.
     */
    public LogAssertion withContext(String correlationKey, Object correlationValue) {
        LogCondition condition = Conditions.field(correlationKey, correlationValue);
        return new LogAssertion(select(condition), stats,
                scope + " with context " + correlationKey + "="
                        + LogRenderer.renderValue(correlationValue));
    }

    /** Asserts at least one matching entry exists. */
    public LogAssertion exists(LogCondition condition) {
        stats.recordAssertion();
        List<LogEntry> matched = select(condition);
        if (matched.isEmpty()) {
            fail("Expected at least one entry where " + condition.describe()
                    + ", but none matched.", condition, matched);
        }
        return this;
    }

    /** Asserts no matching entry exists. */
    public LogAssertion absent(LogCondition condition) {
        stats.recordAssertion();
        List<LogEntry> matched = select(condition);
        if (!matched.isEmpty()) {
            stats.recordFailure();
            throw new AssertionError(buildReport(
                    "Expected no entry where " + condition.describe()
                            + ", but " + matched.size() + " matched.",
                    matched,
                    "entries matching " + condition.describe()));
        }
        return this;
    }

    /** Asserts the condition matches exactly {@code expected} entries. */
    public LogAssertion exactly(int expected, LogCondition condition) {
        stats.recordAssertion();
        List<LogEntry> matched = select(condition);
        if (matched.size() != expected) {
            fail("Expected exactly " + expected + " entr" + (expected == 1 ? "y" : "ies")
                    + " where " + condition.describe() + ", but found "
                    + matched.size() + ".", condition, matched);
        }
        return this;
    }

    /** Asserts the condition matches at least {@code minimum} entries. */
    public LogAssertion atLeast(int minimum, LogCondition condition) {
        stats.recordAssertion();
        List<LogEntry> matched = select(condition);
        if (matched.size() < minimum) {
            fail("Expected at least " + minimum + " entr" + (minimum == 1 ? "y" : "ies")
                    + " where " + condition.describe() + ", but found "
                    + matched.size() + ".", condition, matched);
        }
        return this;
    }

    /**
     * Asserts that entries matching the given conditions appear in the
     * specified order. Unrelated entries may occur between matches; a
     * greedy (earliest-match) subsequence search is used.
     */
    public LogAssertion inOrder(LogCondition... conditions) {
        stats.recordAssertion();
        int searchFrom = 0;
        for (int i = 0; i < conditions.length; i++) {
            int found = -1;
            for (int j = searchFrom; j < entries.size(); j++) {
                if (conditions[i].matches(entries.get(j))) {
                    found = j;
                    break;
                }
            }
            if (found == -1) {
                stats.recordFailure();
                List<LogEntry> remainder =
                        entries.subList(searchFrom, entries.size());
                throw new AssertionError(buildReport(
                        "Expected entries in order, but condition #" + (i + 1)
                                + " [" + conditions[i].describe() + "] was not found"
                                + (searchFrom == 0
                                        ? " in the captured entries."
                                        : " after position " + (searchFrom - 1) + "."),
                        List.copyOf(remainder),
                        "remaining entries searched from position " + searchFrom));
            }
            searchFrom = found + 1;
        }
        return this;
    }

    private List<LogEntry> select(LogCondition condition) {
        Objects.requireNonNull(condition, "condition");
        List<LogEntry> matched = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (condition.matches(entry)) {
                matched.add(entry);
            }
        }
        return matched;
    }

    private void fail(String reason, LogCondition condition, List<LogEntry> matched) {
        stats.recordFailure();
        throw new AssertionError(buildReport(
                reason,
                matched,
                "entries matching " + condition.describe()));
    }

    private String buildReport(String reason, List<LogEntry> matched,
                               String matchedLabel) {
        return "Log assertion failed (" + scope + ")\n"
                + "Reason: " + reason + "\n"
                + "Matched (" + matched.size() + ") - " + matchedLabel + ":\n"
                + LogRenderer.renderList(matched) + "\n"
                + "All entries in scope (" + entries.size() + "):\n"
                + LogRenderer.renderList(entries);
    }
}
