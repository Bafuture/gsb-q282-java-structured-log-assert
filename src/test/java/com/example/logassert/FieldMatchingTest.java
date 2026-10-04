package com.example.logassert;

import static com.example.logassert.Conditions.exceptionIs;
import static com.example.logassert.Conditions.field;
import static com.example.logassert.Conditions.fieldWhere;
import static com.example.logassert.Conditions.hasField;
import static com.example.logassert.Conditions.level;
import static com.example.logassert.Conditions.levelAtLeast;
import static com.example.logassert.Conditions.messageContains;
import static com.example.logassert.Conditions.missingField;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FieldMatchingTest {

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
    void matchesByLevelAndFieldValue() {
        StructuredLogger log = StructuredLogger.get("field.test");
        log.info("ok", "status", 200);
        log.error("boom", new IllegalStateException("x"), "status", 500);

        LogAssertions.assertThat(captor)
                .exists(level(Level.ERROR).and(field("status", 500)))
                .absent(level(Level.ERROR).and(field("status", 200)));
    }

    @Test
    void matchesFieldPresenceAndAbsence() {
        StructuredLogger log = StructuredLogger.get("presence.test");
        log.warn("with user", "userId", "u-1");
        log.warn("without user");

        LogAssertions.assertThat(captor)
                .exists(hasField("userId"))
                .exists(missingField("userId"))
                .absent(hasField("sessionId"));
    }

    @Test
    void matchesFieldWithPredicate() {
        StructuredLogger log = StructuredLogger.get("predicate.test");
        log.info("slow", "durationMs", 950L);

        LogAssertions.assertThat(captor)
                .exists(fieldWhere("durationMs", v -> ((Number) v).longValue() > 500))
                .absent(fieldWhere("durationMs", v -> ((Number) v).longValue() > 5000));
    }

    @Test
    void matchesByExceptionType() {
        StructuredLogger log = StructuredLogger.get("ex.test");
        log.error("io problem", new java.io.IOException("disk full"));
        log.error("state problem", new IllegalStateException("bad"));

        LogAssertions.assertThat(captor)
                .exists(exceptionIs(java.io.IOException.class))
                .exists(level(Level.ERROR).and(exceptionIs(IllegalStateException.class)))
                .absent(exceptionIs(NullPointerException.class));
    }

    @Test
    void matchesByMessageAndLevelThreshold() {
        StructuredLogger log = StructuredLogger.get("msg.test");
        log.info("user login succeeded");
        log.warn("user login throttled");

        LogAssertions.assertThat(captor)
                .exists(messageContains("login"))
                .exists(levelAtLeast(Level.WARN).and(messageContains("throttled")))
                .absent(levelAtLeast(Level.ERROR));
    }

    @Test
    void negatedConditionInvertsMatch() {
        StructuredLogger.get("neg.test").info("fine");

        LogAssertions.assertThat(captor)
                .exists(level(Level.ERROR).negate())
                .absent(level(Level.ERROR));
    }

    @Test
    void failedFieldMatchThrowsAssertionError() {
        StructuredLogger.get("fail.test").info("hello", "status", 200);

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .exists(field("status", 404)))
                .isInstanceOf(AssertionError.class);
    }
}
