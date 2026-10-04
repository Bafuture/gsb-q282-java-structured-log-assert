package com.example.logassert;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextFilterTest {

    private static final Instant BASE = Instant.parse("2026-10-04T13:00:00Z");

    private final LogCapture capture = new LogCapture();

    @Test
    void filtersLogsBelongingToOneRequest() {
        emit("req-1", "trace-A", LogLevel.INFO, "request received", 1);
        emit("req-2", "trace-B", LogLevel.INFO, "request received", 2);
        emit("req-1", "trace-A", LogLevel.ERROR, "payment failed", 3);
        emit("req-2", "trace-B", LogLevel.INFO, "request completed", 4);

        LogAssertions.assertThat(capture.forRequest("req-1"))
                .hasEntryMatching(LogMatcher.error().withFieldPresent("traceId"));
        LogAssertions.assertThat(capture.forRequest("req-2"))
                .hasNoEntryMatching(LogMatcher.error());
    }

    @Test
    void filtersByTraceIdAcrossRequests() {
        emit("req-1", "trace-A", LogLevel.INFO, "step one", 1);
        emit("req-2", "trace-A", LogLevel.WARN, "step two", 2);
        emit("req-3", "trace-B", LogLevel.ERROR, "other trace error", 3);

        LogAssertions.assertThat(capture.forTrace("trace-A")).containsInOrder(
                LogMatcher.info().withMessageContaining("step one"),
                LogMatcher.warn().withMessageContaining("step two"));
        LogAssertions.assertThat(capture.forTrace("trace-A"))
                .hasNoEntryMatching(LogMatcher.error());
    }

    @Test
    void contextFilterWorksWithCountAssertions() {
        emit("req-1", "trace-A", LogLevel.WARN, "retry", 1);
        emit("req-1", "trace-A", LogLevel.WARN, "retry", 2);
        emit("req-2", "trace-A", LogLevel.WARN, "retry", 3);

        LogAssertions.assertThat(capture.forRequest("req-1"))
                .matching(LogMatcher.warn()).occursExactly(2);
        LogAssertions.assertThat(capture.forTrace("trace-A"))
                .matching(LogMatcher.warn()).occursExactly(3);
    }

    @Test
    void contextFilterFailureMessageNamesTheContext() {
        emit("req-1", "trace-A", LogLevel.INFO, "all fine", 1);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture.forRequest("req-1"))
                        .hasEntryMatching(LogMatcher.error()))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("logs where requestId = 'req-1'")
                .hasMessageContaining("all fine");
    }

    @Test
    void unknownRequestYieldsEmptyView() {
        emit("req-1", "trace-A", LogLevel.INFO, "hello", 1);

        LogView view = capture.forRequest("req-unknown");
        assertThat(view.entries()).isEmpty();
        LogAssertions.assertThat(view).hasNoEntryMatching(LogMatcher.any());
    }

    private void emit(String requestId, String traceId, LogLevel level, String message, int sequence) {
        capture.record(LogEntry.builder(level, message)
                .timestamp(BASE.plusSeconds(sequence))
                .field("requestId", requestId)
                .field("traceId", traceId)
                .build());
    }
}
