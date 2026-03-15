package com.rag.app.shared.domain.valueobjects;

import com.rag.app.shared.domain.exceptions.ValidationException;

import java.util.Objects;

public abstract class EntityId {
    private final String value;

    protected EntityId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Entity ID cannot be null or empty");
        }
        this.value = value.trim();
    }

    public String value() {
        return value;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        EntityId entityId = (EntityId) other;
        return Objects.equals(value, entityId.value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
