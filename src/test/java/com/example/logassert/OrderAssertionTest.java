package com.example.logassert;

import static com.example.logassert.Conditions.field;
import static com.example.logassert.Conditions.hasField;
import static com.example.logassert.Conditions.level;
import static com.example.logassert.Conditions.messageContains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderAssertionTest {

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
    void passesWhenConditionsAppearInOrder() {
        StructuredLogger log = StructuredLogger.get("order.test");
        log.info("request received");
        log.info("request validated");
        log.info("request completed");

        LogAssertions.assertThat(captor).inOrder(
                messageContains("received"),
                messageContains("validated"),
                messageContains("completed"));
    }

    @Test
    void allowsUnrelatedEntriesBetweenMatches() {
        StructuredLogger log = StructuredLogger.get("interleave.test");
        log.info("request received");
        log.debug("heartbeat");
        log.warn("retry scheduled", "attempt", 1);
        log.info("request completed");

        LogAssertions.assertThat(captor).inOrder(
                messageContains("received"),
                messageContains("completed"));
    }

    @Test
    void failsWhenOrderIsReversed() {
        StructuredLogger log = StructuredLogger.get("reversed.test");
        log.info("completed");
        log.info("received");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor).inOrder(
                messageContains("received"),
                messageContains("completed")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("condition #2")
                .hasMessageContaining("not found");
    }

    @Test
    void failsWhenOneConditionMissing() {
        StructuredLogger log = StructuredLogger.get("missing.test");
        log.info("received");
        log.info("something else");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor).inOrder(
                messageContains("received"),
                messageContains("completed")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("condition #2");
    }

    @Test
    void orderRespectsFieldValuesNotJustMessages() {
        StructuredLogger log = StructuredLogger.get("orderfield.test");
        log.info("event", "phase", "start", "requestId", "r-1");
        log.info("event", "phase", "end", "requestId", "r-1");

        LogAssertions.assertThat(captor).inOrder(
                level(Level.INFO).and(field("phase", "start")),
                hasField("requestId").and(field("phase", "end")));

        // wrong order (end before start) must fail
        log.info("event", "phase", "end", "requestId", "r-2");
        log.info("event", "phase", "start", "requestId", "r-2");
        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .withContext("requestId", "r-2")
                .inOrder(
                        field("phase", "start"),
                        field("phase", "end")))
                .isInstanceOf(AssertionError.class);
        assertThat(captor.stats().failed()).isEqualTo(1);
    }
}
