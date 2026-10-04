package com.example.logassert;

import java.util.concurrent.atomic.AtomicLong;

public final class LogStats {

    private final AtomicLong captured = new AtomicLong();
    private final AtomicLong assertions = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    public void recordCaptured() {
        captured.incrementAndGet();
    }

    public void recordAssertion() {
        assertions.incrementAndGet();
    }

    public void recordFailure() {
        failures.incrementAndGet();
    }

    public long capturedCount() {
        return captured.get();
    }

    public long assertionCount() {
        return assertions.get();
    }

    public long failureCount() {
        return failures.get();
    }

    @Override
    public String toString() {
        return "LogStats{captured=" + capturedCount()
                + ", assertions=" + assertionCount()
                + ", failures=" + failureCount() + '}';
    }
}
