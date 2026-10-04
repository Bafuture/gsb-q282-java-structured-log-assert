package com.example.logassert;

import static com.example.logassert.Conditions.field;
import static com.example.logassert.Conditions.hasField;
import static com.example.logassert.Conditions.level;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CountAssertionTest {

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
    void assertsExactCount() {
        StructuredLogger log = StructuredLogger.get("count.test");
        log.info("retry", "attempt", 1);
        log.info("retry", "attempt", 2);
        log.info("done");

        LogAssertions.assertThat(captor)
                .exactly(2, hasField("attempt"))
                .exactly(1, field("attempt", 1))
                .exactly(3, level(Level.INFO));
    }

    @Test
    void assertsAtLeastCount() {
        StructuredLogger log = StructuredLogger.get("atleast.test");
        log.warn("a");
        log.warn("b");
        log.error("c");

        LogAssertions.assertThat(captor)
                .atLeast(1, level(Level.ERROR))
                .atLeast(2, level(Level.WARN))
                .atLeast(0, level(Level.TRACE));
    }

    @Test
    void exactCountFailureReportsActualMatches() {
        StructuredLogger log = StructuredLogger.get("countfail.test");
        log.warn("retry", "attempt", 1);
        log.warn("retry", "attempt", 2);
        log.info("other thing");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .exactly(3, hasField("attempt")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected exactly 3")
                .hasMessageContaining("found 2")
                .hasMessageContaining("[0]")
                .hasMessageContaining("[1]")
                .hasMessageContaining("retry");
    }

    @Test
    void atLeastFailureReportsActualCount() {
        StructuredLogger.get("atleastfail.test").error("only one");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .atLeast(2, level(Level.ERROR)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected at least 2")
                .hasMessageContaining("found 1");
    }

    @Test
    void zeroMatchesShowNoEntriesSection() {
        StructuredLogger.get("zero.test").info("hello");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .exactly(1, level(Level.ERROR)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("(no log entries)");
        assertThat(captor.stats().failed()).isEqualTo(1);
    }
}
