package com.example.logassert;

import static com.example.logassert.Conditions.exceptionIs;
import static com.example.logassert.Conditions.field;
import static com.example.logassert.Conditions.hasField;
import static com.example.logassert.Conditions.level;
import static com.example.logassert.Conditions.messageContains;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.logassert.samples.FakeRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end scenario: "a request produced an error log containing specific
 * fields" — the headline use case of the framework.
 */
class RequestFlowTest {

    private LogCaptor captor;
    private FakeRequestService service;

    @BeforeEach
    void setUp() {
        captor = LogCaptor.start();
        service = new FakeRequestService();
    }

    @AfterEach
    void tearDown() {
        captor.close();
    }

    @Test
    void requestProducesStructuredErrorLog() {
        service.handle("req-42", "user-7");
        service.failPayment("req-42", "user-7", "insufficient funds");

        LogAssertions.assertThat(captor)
                .withContext("requestId", "req-42")
                .exists(level(Level.ERROR)
                        .and(field("userId", "user-7"))
                        .and(hasField("reason"))
                        .and(exceptionIs(IllegalStateException.class)));
    }

    @Test
    void fullLifecycleOrderAndCounts() {
        service.handle("req-1", "user-a");
        service.process("req-1", 80);
        service.handle("req-2", "user-b");
        service.failPayment("req-2", "user-b", "card expired");

        LogAssertion req1 = LogAssertions.assertThat(captor)
                .withContext("requestId", "req-1");
        req1.inOrder(
                        messageContains("received"),
                        messageContains("processed"))
                .exactly(2, level(Level.INFO))
                .absent(level(Level.ERROR));

        LogAssertions.assertThat(captor)
                .withContext("requestId", "req-2")
                .atLeast(1, level(Level.ERROR).and(field("reason", "card expired")));

        assertThat(captor.stats().captured()).isEqualTo(4);
        assertThat(captor.stats().failed()).isZero();
    }
}
