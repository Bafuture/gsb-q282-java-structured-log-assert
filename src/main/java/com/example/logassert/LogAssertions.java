package com.example.logassert;

/**
 * Entry point for assertions over a {@link LogCaptor}.
 *
 * <pre>{@code
 * try (LogCaptor captor = LogCaptor.start()) {
 *     service.handle(request);
 *     LogAssertions.assertThat(captor)
 *         .withContext("requestId", "req-1")
 *         .exists(Conditions.level(Level.ERROR).and(Conditions.hasField("userId")));
 * }
 * }</pre>
 */
public final class LogAssertions {

    private LogAssertions() {
    }

    public static LogAssertion assertThat(LogCaptor captor) {
        return new LogAssertion(captor.entries(), captor.stats(), "captured logs");
    }
}
