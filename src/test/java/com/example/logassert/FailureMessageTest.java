package com.example.logassert;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailureMessageTest {

    private static final Instant BASE = Instant.parse("2026-10-04T14:00:00Z");

    private final LogCapture capture = new LogCapture();

    @Test
    void failureListsEveryCapturedEntryWithItsMismatchReason() {
        capture.record(LogEntry.builder(LogLevel.INFO, "order received")
                .timestamp(BASE)
                .field("requestId", "req-1")
                .build());
        capture.record(LogEntry.builder(LogLevel.ERROR, "payment failed")
                .timestamp(BASE.plusSeconds(1))
                .field("requestId", "req-2")
                .field("errorCode", "E400")
                .build());

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.error()
                        .withField("requestId", "req-1")
                        .withFieldPresent("errorCode")))
                .isInstanceOf(LogAssertionError.class)
                .satisfies(error -> {
                    String message = error.getMessage();
                    assertThat(message)
                            .contains("level = ERROR")
                            .contains("field 'requestId' = 'req-1'")
                            .contains("field 'errorCode' is present")
                            .contains("[0]")
                            .contains("order received")
                            .contains("level expected ERROR but was INFO")
                            .contains("field 'errorCode' is absent")
                            .contains("[1]")
                            .contains("payment failed")
                            .contains("field 'requestId' expected 'req-1' but was 'req-2'");
                });
    }

    @Test
    void failureMessageContainsTimestampsLevelsAndFieldsNotBooleans() {
        capture.record(LogEntry.builder(LogLevel.WARN, "cache miss")
                .timestamp(BASE)
                .field("cacheKey", "user:42")
                .build());

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.error()))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("2026-10-04T14:00:00Z")
                .hasMessageContaining("WARN")
                .hasMessageContaining("cacheKey=user:42")
                .hasMessageNotContaining("expected:<true> but was:<false>");
    }

    @Test
    void failureOnEmptyCaptureSaysNoEntriesCaptured() {
        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.any()))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("<no entries captured>");
    }

    @Test
    void exceptionDetailsAppearInFailureOutput() {
        capture.record(LogEntry.builder(LogLevel.ERROR, "db write failed")
                .timestamp(BASE)
                .exception(new IllegalStateException("connection refused"))
                .build());

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture)
                        .hasEntryMatching(LogMatcher.error().withField("db", "orders")))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("IllegalStateException")
                .hasMessageContaining("connection refused")
                .hasMessageContaining("field 'db' is absent");
    }
}
