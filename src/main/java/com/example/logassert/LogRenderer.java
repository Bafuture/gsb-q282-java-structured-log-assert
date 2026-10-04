package com.example.logassert;

import java.util.List;
import java.util.Map;

/**
 * Renders captured entries as readable, numbered, indented text for
 * assertion failure messages.
 */
final class LogRenderer {

    private static final int STACK_FRAMES = 5;

    private LogRenderer() {
    }

    static String renderList(List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return "  (no log entries)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append("  [").append(i).append("] ").append(renderEntry(entries.get(i)));
        }
        return sb.toString();
    }

    static String renderEntry(LogEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(entry.timestamp())
                .append(" ").append(entry.level())
                .append("  ").append(entry.message());
        for (Map.Entry<String, Object> field : entry.fields().entrySet()) {
            sb.append("\n        ").append(field.getKey())
                    .append("=").append(renderValue(field.getValue()));
        }
        Throwable thrown = entry.thrown();
        if (thrown != null) {
            sb.append("\n        thrown: ")
                    .append(thrown.getClass().getName())
                    .append(": ").append(thrown.getMessage());
            StackTraceElement[] trace = thrown.getStackTrace();
            for (int i = 0; i < Math.min(STACK_FRAMES, trace.length); i++) {
                sb.append("\n          at ").append(trace[i]);
            }
        }
        return sb.toString();
    }

    static String renderValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }
}
