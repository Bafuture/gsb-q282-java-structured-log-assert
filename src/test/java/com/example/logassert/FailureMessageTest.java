package com.example.logassert;

import static com.example.logassert.Conditions.field;
import static com.example.logassert.Conditions.hasField;
import static com.example.logassert.Conditions.level;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FailureMessageTest {

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
    void existsFailureListsAllCapturedEntriesAndReason() {
        StructuredLogger log = StructuredLogger.get("failmsg.test");
        log.info("user login", "userId", "u-1");
        log.warn("cache miss", "key", "profile:u-1");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .exists(level(Level.ERROR).and(hasField("traceId"))))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Log assertion failed")
                .hasMessageContaining("Reason:")
                .hasMessageContaining("level=ERROR")
                .hasMessageContaining("has field[traceId]")
                .hasMessageContaining("user login")
                .hasMessageContaining("cache miss")
                .hasMessageContaining("userId='u-1'")
                .hasMessageContaining("All entries in scope (2)");
    }

    @Test
    void absentFailureListsOffendingEntries() {
        StructuredLogger log = StructuredLogger.get("absentmsg.test");
        log.error("boom", new IllegalStateException("kaput"), "requestId", "r-9");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .absent(level(Level.ERROR)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected no entry")
                .hasMessageContaining("but 1 matched")
                .hasMessageContaining("boom")
                .hasMessageContaining("IllegalStateException")
                .hasMessageContaining("requestId='r-9'");
    }

    @Test
    void failureMessageIsNotJustBoolean() {
        StructuredLogger.get("bool.test").info("hello");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .exists(field("missing", "x")))
                .isInstanceOf(AssertionError.class)
                .satisfies(error -> assertThat(error.getMessage())
                        .doesNotContain("expected:<true> but was:<false>")
                        .contains("hello"));
    }

    @Test
    void contextFailuresShowScopeInMessage() {
        StructuredLogger.get("scope.test").info("received", "requestId", "req-7");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .withContext("requestId", "req-7")
                .exists(level(Level.ERROR)))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("requestId='req-7'")
                .hasMessageContaining("level=ERROR");
    }

    @Test
    void inOrderFailureShowsRemainingEntries() {
        StructuredLogger log = StructuredLogger.get("ordermsg.test");
        log.info("first");
        log.info("unrelated noise");

        assertThatThrownBy(() -> LogAssertions.assertThat(captor)
                .inOrder(
                        com.example.logassert.Conditions.messageContains("first"),
                        com.example.logassert.Conditions.messageContains("second")))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("condition #2")
                .hasMessageContaining("remaining entries")
                .hasMessageContaining("unrelated noise");
    }
}
