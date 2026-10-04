package com.example.logassert;

import static com.example.logassert.Conditions.field;
import static com.example.logassert.Conditions.level;
import static com.example.logassert.Conditions.messageContains;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.logassert.samples.FakeRequestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContextFilterTest {

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
    void filtersEntriesBelongingToOneRequest() {
        service.handle("req-1", "user-a");
        service.handle("req-2", "user-b");
        service.failPayment("req-1", "user-a", "insufficient funds");

        LogAssertions.assertThat(captor)
                .withContext("requestId", "req-1")
                .exists(level(Level.ERROR).and(field("userId", "user-a")));

        LogAssertions.assertThat(captor)
                .withContext("requestId", "req-2")
                .absent(level(Level.ERROR));
    }

    @Test
    void contextViewOnlySeesItsOwnEntries() {
        service.handle("req-1", "user-a");
        service.process("req-1", 120);
        service.handle("req-2", "user-b");

        LogAssertion req1 = LogAssertions.assertThat(captor)
                .withContext("requestId", "req-1");

        assertThat(req1.entries()).hasSize(2);
        req1.exactly(2, level(Level.INFO))
                .inOrder(
                        messageContains("received"),
                        messageContains("processed"));
    }

    @Test
    void filterComposesWithContext() {
        service.handle("req-1", "user-a");
        service.failPayment("req-1", "user-a", "card expired");
        service.handle("req-2", "user-b");

        LogAssertions.assertThat(captor)
                .withContext("requestId", "req-1")
                .filter(level(Level.ERROR))
                .exists(field("reason", "card expired"));
    }

    @Test
    void unknownRequestIdMatchesNothing() {
        service.handle("req-1", "user-a");

        LogAssertion unknown = LogAssertions.assertThat(captor)
                .withContext("requestId", "req-999");

        assertThat(unknown.entries()).isEmpty();
        unknown.absent(level(Level.ERROR));
    }
}
