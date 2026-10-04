package com.example.logassert;

/**
 * Log severity levels, ordered from most to least verbose.
 */
public enum Level {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    /**
     * @return true if this level is at least as severe as {@code other}.
     */
    public boolean isAtLeast(Level other) {
        return ordinal() >= other.ordinal();
    }
}
