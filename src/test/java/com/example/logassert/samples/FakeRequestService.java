package com.example.logassert.samples;

import com.example.logassert.Level;
import com.example.logassert.StructuredLogger;

/**
 * Sample "production" service used by the tests: it emits structured logs
 * tagged with a request id, the way a real request-handling component would.
 */
public final class FakeRequestService {

    private final StructuredLogger log = StructuredLogger.get(FakeRequestService.class);

    public void handle(String requestId, String userId) {
        log.at(Level.INFO)
                .field("requestId", requestId)
                .field("userId", userId)
                .field("event", "request.received")
                .log("request received");
    }

    public void process(String requestId, long durationMs) {
        log.at(Level.INFO)
                .field("requestId", requestId)
                .field("durationMs", durationMs)
                .field("event", "request.processed")
                .log("request processed");
    }

    public void failPayment(String requestId, String userId, String reason) {
        log.at(Level.ERROR)
                .field("requestId", requestId)
                .field("userId", userId)
                .field("reason", reason)
                .thrown(new IllegalStateException("payment declined: " + reason))
                .log("payment failed");
    }
}
