package com.example.logassert;

import java.util.List;
import java.util.Objects;

final class FilteredLogView implements LogView {

    private final LogCapture parent;
    private final String fieldKey;
    private final Object value;

    FilteredLogView(LogCapture parent, String fieldKey, Object value) {
        this.parent = parent;
        this.fieldKey = Objects.requireNonNull(fieldKey, "fieldKey");
        this.value = value;
    }

    @Override
    public List<LogEntry> entries() {
        return parent.entries().stream()
                .filter(entry -> entry.hasField(fieldKey) && Objects.equals(entry.field(fieldKey), value))
                .toList();
    }

    @Override
    public LogStats stats() {
        return parent.stats();
    }

    @Override
    public String label() {
        return "logs where " + fieldKey + " = '" + value + "'";
    }
}
