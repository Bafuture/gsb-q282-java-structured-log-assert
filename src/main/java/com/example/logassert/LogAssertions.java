package com.example.logassert;

public final class LogAssertions {

    private LogAssertions() {
    }

    public static LogSourceAssert assertThat(LogView view) {
        return new LogSourceAssert(view);
    }

    public static LogSourceAssert assertThat(LogCapture capture) {
        return new LogSourceAssert(capture);
    }
}
