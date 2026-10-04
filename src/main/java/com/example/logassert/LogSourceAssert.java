package com.example.logassert;

import java.util.List;

public final class LogSourceAssert {

    private final LogView view;

    LogSourceAssert(LogView view) {
        this.view = view;
    }

    public LogSourceAssert hasEntryMatching(LogMatcher matcher) {
        view.stats().recordAssertion();
        List<LogEntry> entries = view.entries();
        boolean found = entries.stream().anyMatch(matcher::matches);
        if (!found) {
            throw fail(new StringBuilder()
                    .append("Expected at least one log entry matching:\n")
                    .append(expectationsBlock(matcher))
                    .append("in ").append(view.label())
                    .append(", but none of the ").append(entries.size()).append(" entries matched:\n")
                    .append(entriesWithMismatches(entries, matcher))
                    .toString());
        }
        return this;
    }

    public LogSourceAssert hasNoEntryMatching(LogMatcher matcher) {
        view.stats().recordAssertion();
        List<LogEntry> entries = view.entries();
        List<LogEntry> matched = entries.stream().filter(matcher::matches).toList();
        if (!matched.isEmpty()) {
            StringBuilder sb = new StringBuilder()
                    .append("Expected no log entry matching:\n")
                    .append(expectationsBlock(matcher))
                    .append("in ").append(view.label())
                    .append(", but found ").append(matched.size()).append(":\n");
            for (LogEntry entry : matched) {
                sb.append("  [").append(entries.indexOf(entry)).append("] ")
                        .append(entry.render()).append('\n');
            }
            throw fail(sb.toString());
        }
        return this;
    }

    public LogSourceAssert containsInOrder(LogMatcher... steps) {
        view.stats().recordAssertion();
        List<LogEntry> entries = view.entries();
        int searchFrom = 0;
        int lastMatchedIndex = -1;
        for (int step = 0; step < steps.length; step++) {
            LogMatcher matcher = steps[step];
            int foundAt = -1;
            for (int i = searchFrom; i < entries.size(); i++) {
                if (matcher.matches(entries.get(i))) {
                    foundAt = i;
                    break;
                }
            }
            if (foundAt < 0) {
                throw fail(orderFailure(entries, steps, step, lastMatchedIndex));
            }
            lastMatchedIndex = foundAt;
            searchFrom = foundAt + 1;
        }
        return this;
    }

    public CountAssert matching(LogMatcher matcher) {
        return new CountAssert(view, matcher);
    }

    private String orderFailure(List<LogEntry> entries, LogMatcher[] steps,
                                int failedStep, int lastMatchedIndex) {
        StringBuilder sb = new StringBuilder()
                .append("Expected logs to appear in order:\n");
        for (int i = 0; i < steps.length; i++) {
            sb.append("  step ").append(i + 1).append(": ").append(steps[i].describe()).append('\n');
        }
        sb.append("in ").append(view.label()).append(", but step ").append(failedStep + 1);
        if (lastMatchedIndex >= 0) {
            sb.append(" did not occur after step ").append(failedStep)
                    .append(" (matched at index ").append(lastMatchedIndex).append(")");
        } else {
            sb.append(" was never matched");
        }
        sb.append(".\n");
        int from = lastMatchedIndex + 1;
        if (from < entries.size()) {
            sb.append("Entries searched for step ").append(failedStep + 1).append(":\n")
                    .append(entriesWithMismatches(entries.subList(from, entries.size()), steps[failedStep], from));
        } else {
            sb.append("No entries remain after index ").append(lastMatchedIndex).append(".\n");
        }
        return sb.toString();
    }

    static String expectationsBlock(LogMatcher matcher) {
        StringBuilder sb = new StringBuilder();
        for (String expectation : LogMatcher.immutableExpectationsOf(matcher)) {
            sb.append("  - ").append(expectation).append('\n');
        }
        if (matcher.expectations().isEmpty()) {
            sb.append("  - <any entry>\n");
        }
        return sb.toString();
    }

    static String entriesWithMismatches(List<LogEntry> entries, LogMatcher matcher) {
        return entriesWithMismatches(entries, matcher, 0);
    }

    static String entriesWithMismatches(List<LogEntry> entries, LogMatcher matcher, int indexOffset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            LogEntry entry = entries.get(i);
            sb.append("  [").append(i + indexOffset).append("] ").append(entry.render()).append('\n');
            List<String> reasons = matcher.mismatches(entry);
            if (!reasons.isEmpty()) {
                sb.append("      mismatch: ").append(String.join("; ", reasons)).append('\n');
            }
        }
        if (entries.isEmpty()) {
            sb.append("  <no entries captured>\n");
        }
        return sb.toString();
    }

    private LogAssertionError fail(String message) {
        view.stats().recordFailure();
        return new LogAssertionError(message);
    }
}
