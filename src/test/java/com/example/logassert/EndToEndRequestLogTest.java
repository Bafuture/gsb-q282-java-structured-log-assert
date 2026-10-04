package com.example.logassert;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndToEndRequestLogTest {

    private static final Instant BASE = Instant.parse("2026-10-04T15:00:00Z");

    @Test
    void requestProducesErrorLogWithExpectedFields() {
        LogCapture capture = new LogCapture();
        StructuredLogger log = new StructuredLogger(capture, Clock.fixed(BASE, ZoneOffset.UTC));

        simulateCheckoutRequest(log, "req-100", "trace-xyz", false);

        LogAssertions.assertThat(capture.forRequest("req-100")).containsInOrder(
                LogMatcher.info().withMessageContaining("checkout started"),
                LogMatcher.error()
                        .withField("requestId", "req-100")
                        .withField("errorCode", "PAYMENT_DECLINED")
                        .withFieldPresent("amount")
                        .withException(PaymentException.class));
        LogAssertions.assertThat(capture.forTrace("trace-xyz"))
                .matching(LogMatcher.error())
                .occursExactly(1);
    }

    @Test
    void assertionFailsReadablyWhenErrorLogIsMissing() {
        LogCapture capture = new LogCapture();
        StructuredLogger log = new StructuredLogger(capture, Clock.fixed(BASE, ZoneOffset.UTC));

        simulateCheckoutRequest(log, "req-200", "trace-ok", true);

        assertThatThrownBy(() ->
                LogAssertions.assertThat(capture.forRequest("req-200"))
                        .hasEntryMatching(LogMatcher.error().withFieldPresent("errorCode")))
                .isInstanceOf(LogAssertionError.class)
                .hasMessageContaining("logs where requestId = 'req-200'")
                .hasMessageContaining("checkout started")
                .hasMessageContaining("checkout completed");
    }

    private static void simulateCheckoutRequest(StructuredLogger log, String requestId,
                                                String traceId, boolean paymentSucceeds) {
        log.info("checkout started", "requestId", requestId, "traceId", traceId);
        log.debug("cart validated", "requestId", requestId, "traceId", traceId, "items", 3);
        if (paymentSucceeds) {
            log.info("checkout completed", "requestId", requestId, "traceId", traceId);
        } else {
            log.error("payment failed",
                    new PaymentException("card declined"),
                    "requestId", requestId,
                    "traceId", traceId,
                    "errorCode", "PAYMENT_DECLINED",
                    "amount", 9900);
        }
    }

    static final class PaymentException extends RuntimeException {
        PaymentException(String message) {
            super(message);
        }
    }
}
