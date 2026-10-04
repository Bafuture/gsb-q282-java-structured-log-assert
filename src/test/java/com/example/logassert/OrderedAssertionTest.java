package com.example.logassert;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderedAssertionTest {

    private static final Instant BASE = Instant.parse("2026-10-04T11:00:00Z");

    private final LogCapture capture = new LogCapture();

    @Test
    void passesWhenLogsAppearInOrderWithUnrelatedLogsBetween() {
        emit(LogLevel.INFO, "noise before");
        emit(LogLevel.INFO, "request received", 1);
        emit(LogLevel.DEBUG, "unrelated debug", 2);
        emit(LogLevel.WARN, "unrelated warn", 3);
        emit(LogLevel.ERROR, "payment failed", 4);
        emit(LogLevel.INFO, "noise after", 5);

        LogAssertions.assertThat(capture).containsInOrder(
                LogMatcher.info().withMessageContaining("request received"),
                LogMatcher.error().withFieldPresent("requestId"));
    }

    @Test
    void passesWhenLogsAreExactlyConsecutive() {
        emit(LogLevel.INFO, "first", 1);
        emit(LogLevel.INFO, "second", 2);
        emit(LogLevel.INFO, "third", 3);

        LogAssertions.assertThat(capture).containsInOrder(
                LogMatcher.messageContaining("first"),
                LogMatcher.messageContaining("second"),
                LogMatcher.messageContaining("third"));
    }

    @Test
    void failsWhenOrderIsWrongEvenIfAllStepsExist() {
        emit(LogLevel.ERROR, "payment failed", 1);
        emit(LogLevel.INFO, "request received", 2);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).containsInOrder(
                        LogMatcher.messageContaining("request received"),
                        LogMatcher.error()))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("Expected logs to appear in order")
                .hasMessageContaining("step 2")
                .hasMessageContaining("did not occur after step 1");
    }

    @Test
    void failureShowsRemainingEntriesAndMismatchReasons() {
        emit(LogLevel.INFO, "request received", 1);
        emit(LogLevel.WARN, "slow downstream", 2);
        emit(LogLevel.INFO, "request completed", 3);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).containsInOrder(
                        LogMatcher.messageContaining("request received"),
                        LogMatcher.error().withField("errorCode", "E500")))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("step 2")
                .hasMessageContaining("slow downstream")
                .hasMessageContaining("level expected ERROR but was WARN");
    }

    @Test
    void failsWhenFirstStepMissing() {
        emit(LogLevel.ERROR, "only error", 1);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).containsInOrder(
                        LogMatcher.info(),
                        LogMatcher.error()))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("step 1 was never matched");
    }

    private void emit(LogLevel level, String message) {
        capture.record(LogEntry.builder(level, message).timestamp(BASE.plusSeconds(0)).build());
    }

    private void emit(LogLevel level, String message, int sequence) {
        capture.record(LogEntry.builder(level, message)
                .timestamp(BASE.plusSeconds(sequence))
                .field("requestId", "req-7")
                .build());
    }
}
