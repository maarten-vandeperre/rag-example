package com.rag.app.shared.domain.valueobjects;

import java.time.Instant;
import java.util.Objects;

public final class Timestamp {
    private final Instant instant;

    private Timestamp(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant cannot be null");
    }

    public static Timestamp now() {
        return new Timestamp(Instant.now());
    }

    public static Timestamp of(Instant instant) {
        return new Timestamp(instant);
    }

    public Instant instant() {
        return instant;
    }

    public boolean isBefore(Timestamp other) {
        return instant.isBefore(other.instant);
    }

    public boolean isAfter(Timestamp other) {
        return instant.isAfter(other.instant);
    }
}
