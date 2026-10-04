package com.example.logassert;

import static com.example.logassert.Conditions.field;
import static com.example.logassert.Conditions.level;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatisticsTest {

    private LogCaptor captor;

    @BeforeEach
    void setUp() {
        captor = LogCaptor.start();
    }

    @AfterEach
    void tearDown() {
        captor.close();
    }

    @Test
    void countsCapturedEntries() {
        StructuredLogger log = StructuredLogger.get("stats.test");
        log.info("a");
        log.warn("b");
        log.error("c", new RuntimeException("x"));

        assertThat(captor.stats().captured()).isEqualTo(3);
    }

    @Test
    void countsAssertionsAndFailures() {
        StructuredLogger log = StructuredLogger.get("stats2.test");
        log.info("hello", "status", 200);

        LogAssertions.assertThat(captor)
                .exists(level(Level.INFO))
                .exactly(1, field("status", 200));

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .exists(level(Level.ERROR)))
                .isInstanceOf(AssertionError.class);

        AssertionStats stats = captor.stats();
        assertThat(stats.captured()).isEqualTo(1);
        assertThat(stats.asserted()).isEqualTo(3);
        assertThat(stats.failed()).isEqualTo(1);
    }

    @Test
    void failedAssertionsStillIncrementAssertionCount() {
        StructuredLogger.get("stats3.test").info("x");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .atLeast(5, level(Level.INFO)))
                .isInstanceOf(AssertionError.class);

        assertThat(captor.stats().asserted()).isEqualTo(1);
        assertThat(captor.stats().failed()).isEqualTo(1);
    }

    @Test
    void clearResetsEntriesButKeepsStats() {
        StructuredLogger log = StructuredLogger.get("stats4.test");
        log.info("before clear");
        LogAssertions.assertThat(captor).exists(level(Level.INFO));

        captor.clear();
        log.info("after clear");

        assertThat(captor.entries()).hasSize(1);
        assertThat(captor.stats().captured()).isEqualTo(2);
        assertThat(captor.stats().asserted()).isEqualTo(1);
    }

    @Test
    void statsToStringIsReadable() {
        StructuredLogger.get("stats5.test").info("x");
        LogAssertions.assertThat(captor).exists(level(Level.INFO));

        assertThat(captor.stats().toString())
                .contains("captured=1")
                .contains("asserted=1")
                .contains("failed=0");
    }
}
