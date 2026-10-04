package com.example.logassert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Captures every entry emitted by {@link StructuredLogger} while started.
 * Use {@link #start()} in a try-with-resources block (or {@code @BeforeEach} /
 * {@code @AfterEach}) and build assertions via {@link LogAssertions}.
 */
public final class LogCaptor implements AutoCloseable {

    private final List<LogEntry> entries = new CopyOnWriteArrayList<>();
    private final AssertionStats stats = new AssertionStats();
    private final StructuredLogger.Sink sink = entry -> {
        entries.add(entry);
        stats.recordCaptured();
    };

    private LogCaptor() {
    }

    /** Creates and starts a captor. */
    public static LogCaptor start() {
        LogCaptor captor = new LogCaptor();
        StructuredLogger.addSink(captor.sink);
        return captor;
    }

    /** All entries captured so far, in emission order. */
    public List<LogEntry> entries() {
        return List.copyOf(entries);
    }

    public AssertionStats stats() {
        return stats;
    }

    public void clear() {
        entries.clear();
    }

    /** Stops capturing; the captor no longer receives new entries. */
    @Override
    public void close() {
        StructuredLogger.removeSink(sink);
    }
}
