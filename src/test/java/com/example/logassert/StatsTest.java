package com.example.logassert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatsTest {

    private final LogCapture capture = new LogCapture();

    @Test
    void countsCapturedEntries() {
        capture.record(LogLevel.INFO, "one");
        capture.record(LogLevel.WARN, "two");
        capture.record(LogLevel.ERROR, "three");

        assertThat(capture.stats().capturedCount()).isEqualTo(3);
        assertThat(capture.stats().assertionCount()).isZero();
        assertThat(capture.stats().failureCount()).isZero();
    }

    @Test
    void countsAssertionsAndFailures() {
        capture.record(LogLevel.ERROR, "boom");

        LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.error());
        LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursExactly(1);
        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.info()))
                .isInstanceOf(LogAssertionError.class);

        LogStats stats = capture.stats();
        assertThat(stats.assertionCount()).isEqualTo(3);
        assertThat(stats.failureCount()).isEqualTo(1);
    }

    @Test
    void viewsShareStatsWithTheirCapture() {
        capture.record(LogEntry.builder(LogLevel.ERROR, "boom")
                .field("requestId", "req-1")
                .build());

        LogAssertions.assertThat(capture.forRequest("req-1"))
                .hasEntryMatching(LogMatcher.error());
        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture.forRequest("req-1"))
                        .hasEntryMatching(LogMatcher.warn()))
                .isInstanceOf(LogAssertionError.class);

        assertThat(capture.stats().assertionCount()).isEqualTo(2);
        assertThat(capture.stats().failureCount()).isEqualTo(1);
        assertThat(capture.stats().capturedCount()).isEqualTo(1);
    }

    @Test
    void failedCountAssertionsAreCounted() {
        capture.record(LogLevel.INFO, "ok");

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursExactly(2))
                .isInstanceOf(LogAssertionError.class);
        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).matching(LogMatcher.error()).occursAtLeast(1))
                .isInstanceOf(LogAssertionError.class);
        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture).containsInOrder(LogMatcher.error()))
                .isInstanceOf(LogAssertionError.class);

        LogStats stats = capture.stats();
        assertThat(stats.assertionCount()).isEqualTo(3);
        assertThat(stats.failureCount()).isEqualTo(3);
    }

    @Test
    void statsToStringIsReadable() {
        capture.record(LogLevel.INFO, "x");
        LogAssertions.assertThat(capture).hasEntryMatching(LogMatcher.info());

        assertThat(capture.stats().toString())
                .contains("captured=1")
                .contains("assertions=1")
                .contains("failures=0");
    }
}
