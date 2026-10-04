package com.example.logassert;

/**
 * Mutable counters for a capture session: how many entries were captured,
 * how many assertions ran and how many of them failed.
 */
public final class AssertionStats {

    private long captured;
    private long asserted;
    private long failed;

    void recordCaptured() {
        captured++;
    }

    void recordAssertion() {
        asserted++;
    }

    void recordFailure() {
        failed++;
    }

    public long captured() {
        return captured;
    }

    public long asserted() {
        return asserted;
    }

    public long failed() {
        return failed;
    }

    @Override
    public String toString() {
        return "AssertionStats{captured=" + captured
                + ", asserted=" + asserted
                + ", failed=" + failed + "}";
    }
}
