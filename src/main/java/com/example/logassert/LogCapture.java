package com.example.logassert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LogCapture implements LogView {

    private final List<LogEntry> entries = new CopyOnWriteArrayList<>();
    private final LogStats stats = new LogStats();

    public LogEntry record(LogEntry entry) {
        entries.add(entry);
        stats.recordCaptured();
        return entry;
    }

    public LogEntry record(LogLevel level, String message) {
        return record(LogEntry.of(level, message));
    }

    @Override
    public List<LogEntry> entries() {
        return new ArrayList<>(entries);
    }

    @Override
    public LogStats stats() {
        return stats;
    }

    @Override
    public String label() {
        return "all captured logs";
    }

    public LogView forContext(String fieldKey, Object value) {
        return new FilteredLogView(this, fieldKey, value);
    }

    public LogView forRequest(String requestId) {
        return forContext("requestId", requestId);
    }

    public LogView forTrace(String traceId) {
        return forContext("traceId", traceId);
    }

    public void clear() {
        entries.clear();
    }
}
