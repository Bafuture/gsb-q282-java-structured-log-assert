package com.example.logassert;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FieldMatchingTest {

    private static final Instant BASE = Instant.parse("2026-10-04T10:00:00Z");

    private final LogCapture capture = new LogCapture();

    @Test
    void matchesByLevel() {
        capture.record(LogEntry.of(LogLevel.INFO, "service started"));
        capture.record(LogEntry.of(LogLevel.ERROR, "service crashed"));

        LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.level(LogLevel.ERROR));
        LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.error());
    }

    @Test
    void matchesByLevelFailsWhenLevelDiffers() {
        capture.record(LogEntry.of(LogLevel.WARN, "almost crashed"));

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.error()))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("level expected ERROR but was WARN");
    }

    @Test
    void matchesByKeyValueField() {
        capture.record(LogEntry.builder(LogLevel.INFO, "order received")
                .timestamp(BASE)
                .field("orderId", "o-1")
                .field("amount", 250)
                .build());

        LogAssertions.assertThat(capture)
                .hasEntryMatching(LogMatcher.any()
                        .withField("orderId", "o-1")
                        .withField("amount", 250));
    }

    @Test
    void fieldValueMatchIsStrict() {
        capture.record(LogEntry.builder(LogLevel.INFO, "order received")
                .timestamp(BASE)
                .field("orderId", "o-1")
                .build());

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture)
                        .hasEntryMatching(LogMatcher.field("orderId", "o-2")))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("field 'orderId' expected 'o-2' but was 'o-1'");
    }

    @Test
    void matchesByFieldPresence() {
        capture.record(LogEntry.builder(LogLevel.INFO, "order received")
                .timestamp(BASE)
                .field("traceId", "t-9")
                .build());

        LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.any().withFieldPresent("traceId"));

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.any().withFieldPresent("requestId")))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("field 'requestId' is absent");
    }

    @Test
    void matchesByExceptionType() {
        capture.record(LogEntry.builder(LogLevel.ERROR, "io failed")
                .timestamp(BASE)
                .exception(new IOException("disk full"))
                .build());

        LogAssertions.assertThat(capture)
                .hasEntryMatching(LogMatcher.error().withException(IOException.class));
    }

    @Test
    void exceptionTypeMismatchReportsActualType() {
        capture.record(LogEntry.builder(LogLevel.ERROR, "boom")
                .timestamp(BASE)
                .exception(new IllegalStateException("bad state"))
                .build());

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture)
                        .hasEntryMatching(LogMatcher.error().withException(IOException.class)))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("exception expected IOException but was IllegalStateException");
    }

    @Test
    void combinesLevelFieldsPresenceAndExceptionType() {
        capture.record(LogEntry.builder(LogLevel.DEBUG, "checking")
                .timestamp(BASE)
                .field("requestId", "req-1")
                .build());
        capture.record(LogEntry.builder(LogLevel.ERROR, "payment failed")
                .timestamp(BASE.plusSeconds(1))
                .field("requestId", "req-1")
                .field("errorCode", "E500")
                .field("amount", 99)
                .exception(new IllegalArgumentException("negative amount"))
                .build());

        LogAssertions.assertThat(capture)
                .hasEntryMatching(LogMatcher.error()
                        .withField("requestId", "req-1")
                        .withField("errorCode", "E500")
                        .withFieldPresent("amount")
                        .withException(IllegalArgumentException.class));
    }

    @Test
    void supportsMessageContaining() {
        capture.record(LogEntry.of(LogLevel.WARN, "retrying call after timeout, attempt=2"));

        LogAssertions.assertThat(capture)
                .hasEntryMatching(LogMatcher.warn().withMessageContaining("retrying"));
    }

    @Test
    void hasNoEntryMatching() {
        capture.record(LogEntry.of(LogLevel.INFO, "all good"));

        LogAssertions.assertThat(capture).hasNoEntryMatching(LogMatcher.error());
        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).hasNoEntryMatching(LogMatcher.info()))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("Expected no log entry matching");
    }
}
