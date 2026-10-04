package com.example.logassert;

import java.util.List;

public final class CountAssert {

    private final LogView view;
    private final LogMatcher matcher;

    CountAssert(LogView view, LogMatcher matcher) {
        this.view = view;
        this.matcher = matcher;
    }

    public CountAssert occursExactly(int expected) {
        view.stats().recordAssertion();
        List<LogEntry> entries = view.entries();
        List<LogEntry> matched = entries.stream().filter(matcher::matches).toList();
        if (matched.size() != expected) {
            throw fail(new StringBuilder()
                    .append("Expected exactly ").append(expected).append(" log entries matching:\n")
                    .append(LogSourceAssert.expectationsBlock(matcher))
                    .append("in ").append(view.label())
                    .append(", but found ").append(matched.size()).append(".\n")
                    .append(matchedBlock(entries, matched))
                    .append("All captured entries:\n")
                    .append(LogSourceAssert.entriesWithMismatches(entries, matcher))
                    .toString());
        }
        return this;
    }

    public CountAssert occursAtLeast(int minimum) {
        view.stats().recordAssertion();
        List<LogEntry> entries = view.entries();
        List<LogEntry> matched = entries.stream().filter(matcher::matches).toList();
        if (matched.size() < minimum) {
            throw fail(new StringBuilder()
                    .append("Expected at least ").append(minimum).append(" log entries matching:\n")
                    .append(LogSourceAssert.expectationsBlock(matcher))
                    .append("in ").append(view.label())
                    .append(", but found only ").append(matched.size()).append(".\n")
                    .append(matchedBlock(entries, matched))
                    .append("All captured entries:\n")
                    .append(LogSourceAssert.entriesWithMismatches(entries, matcher))
                    .toString());
        }
        return this;
    }

    private String matchedBlock(List<LogEntry> entries, List<LogEntry> matched) {
        StringBuilder sb = new StringBuilder();
        if (matched.isEmpty()) {
            sb.append("Matched entries: <none>\n");
        } else {
            sb.append("Matched entries:\n");
            for (LogEntry entry : matched) {
                sb.append("  [").append(entries.indexOf(entry)).append("] ")
                        .append(entry.render()).append('\n');
            }
        }
        return sb.toString();
    }

    private LogAssertionError fail(String message) {
        view.stats().recordFailure();
        return new LogAssertionError(message);
    }
}
