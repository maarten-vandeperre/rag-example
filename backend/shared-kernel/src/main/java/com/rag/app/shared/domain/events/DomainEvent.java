package com.rag.app.shared.domain.events;

import com.rag.app.shared.domain.valueobjects.Timestamp;

import java.util.UUID;

public abstract class DomainEvent {
    private final String eventId;
    private final Timestamp occurredAt;
    private final String eventType;

    protected DomainEvent(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Timestamp.now();
        this.eventType = eventType;
    }

    public String eventId() {
        return eventId;
    }

    public Timestamp occurredAt() {
        return occurredAt;
    }

    public String eventType() {
        return eventType;
    }
}
