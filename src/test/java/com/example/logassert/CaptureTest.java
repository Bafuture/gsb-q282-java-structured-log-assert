package com.example.logassert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaptureTest {

    private LogCaptor captor;

    @BeforeEach
    void setUp() {
        captor = LogCaptor.start();
    }

    @AfterEach
    void tearDown() {
        StructuredLogger.resetClock();
        captor.close();
    }

    @Test
    void capturesLevelMessageFieldsAndException() {
        StructuredLogger log = StructuredLogger.get("capture.test");
        IllegalArgumentException boom = new IllegalArgumentException("bad input");

        log.at(Level.ERROR)
                .field("requestId", "req-1")
                .field("attempt", 3)
                .thrown(boom)
                .log("processing failed");

        assertThat(captor.entries()).hasSize(1);
        LogEntry entry = captor.entries().get(0);
        assertThat(entry.level()).isEqualTo(Level.ERROR);
        assertThat(entry.message()).isEqualTo("processing failed");
        assertThat(entry.loggerName()).isEqualTo("capture.test");
        assertThat(entry.fields())
                .containsEntry("requestId", "req-1")
                .containsEntry("attempt", 3);
        assertThat(entry.thrown()).isSameAs(boom);
        assertThat(entry.thrownType()).isEqualTo("IllegalArgumentException");
    }

    @Test
    void capturesTimestampFromClock() {
        Instant fixed = Instant.parse("2026-10-05T08:00:00Z");
        StructuredLogger.setClock(Clock.fixed(fixed, ZoneOffset.UTC));

        StructuredLogger.get("clock.test").info("hello");

        assertThat(captor.entries().get(0).timestamp()).isEqualTo(fixed);
    }

    @Test
    void entriesWithoutExceptionHaveNullThrown() {
        StructuredLogger.get("plain.test").info("plain", "k", "v");

        LogEntry entry = captor.entries().get(0);
        assertThat(entry.thrown()).isNull();
        assertThat(entry.thrownType()).isNull();
        assertThat(entry.hasField("k")).isTrue();
        assertThat(entry.hasField("missing")).isFalse();
    }

    @Test
    void entriesAreImmutableSnapshots() {
        StructuredLogger.get("immut.test").info("a");
        StructuredLogger.get("immut.test").info("b");

        assertThat(captor.entries()).hasSize(2);
        assertThat(captor.entries()).extracting(LogEntry::message)
                .containsExactly("a", "b");
    }
}
