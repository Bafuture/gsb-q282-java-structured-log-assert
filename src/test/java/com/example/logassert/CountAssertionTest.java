package com.example.logassert;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CountAssertionTest {

    private static final Instant BASE = Instant.parse("2026-10-04T12:00:00Z");

    private final LogCapture capture = new LogCapture();

    @Test
    void assertsExactlyN() {
        emit(LogLevel.ERROR, "boom 1", 1);
        emit(LogLevel.ERROR, "boom 2", 2);
        emit(LogLevel.INFO, "ok", 3);

        LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursExactly(2);
    }

    @Test
    void assertsAtLeastN() {
        emit(LogLevel.WARN, "retry 1", 1);
        emit(LogLevel.WARN, "retry 2", 2);
        emit(LogLevel.WARN, "retry 3", 3);
        emit(LogLevel.INFO, "ok", 4);

        LogAssertions.assertThat(capture).matching(LogMatcher.warn()).occursAtLeast(2);
        LogAssertions.assertThat(capture).matching(LogMatcher.warn()).occursAtLeast(3);
    }

    @Test
    void exactCountFailurePrintsExpectedFoundAndMatchedEntries() {
        emit(LogLevel.ERROR, "boom 1", 1);
        emit(LogLevel.ERROR, "boom 2", 2);
        emit(LogLevel.INFO, "noise", 3);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursExactly(3))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("Expected exactly 3")
                .hasMessageContaining("but found 2")
                .hasMessageContaining("Matched entries")
                .hasMessageContaining("boom 1")
                .hasMessageContaining("boom 2")
                .hasMessageContaining("All captured entries")
                .hasMessageContaining("noise");
    }

    @Test
    void atLeastCountFailurePrintsActualMatchesWhenThereAreSome() {
        emit(LogLevel.ERROR, "only boom", 1);
        emit(LogLevel.INFO, "noise", 2);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursAtLeast(2))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("Expected at least 2")
                .hasMessageContaining("but found only 1")
                .hasMessageContaining("only boom");
    }

    @Test
    void atLeastCountFailurePrintsNoneWhenNothingMatched() {
        emit(LogLevel.INFO, "fine", 1);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursAtLeast(1))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("but found only 0")
                .hasMessageContaining("Matched entries: <none>");
    }

    @Test
    void countWorksWithCompositeMatchers() {
        capture.record(LogEntry.builder(LogLevel.ERROR, "x")
                .timestamp(BASE).field("requestId", "r1").build());
        capture.record(LogEntry.builder(LogLevel.ERROR, "x")
                .timestamp(BASE.plusSeconds(1)).field("requestId", "r2").build());
        capture.record(LogEntry.builder(LogLevel.ERROR, "x")
                .timestamp(BASE.plusSeconds(2)).field("requestId", "r1").build());

        LogAssertions.assertThat(capture)
                .matching(LogMatcher.error().withField("requestId", "r1"))
                .occursExactly(2);
        LogAssertions.assertThat(capture)
                .matching(LogMatcher.error().withField("requestId", "r1"))
                .occursAtLeast(1);
    }

    private void emit(LogLevel level, String message, int sequence) {
        capture.record(LogEntry.builder(level, message)
                .timestamp(BASE.plusSeconds(sequence))
                .build());
    }
}
