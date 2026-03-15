package com.rag.app.integration.events.events;

import com.rag.app.shared.domain.events.DomainEvent;

public final class ChatQuerySubmittedEvent extends DomainEvent {
    private final String userId;
    private final String question;
    private final String correlationId;

    public ChatQuerySubmittedEvent(String userId, String question, String correlationId) {
        super("ChatQuerySubmitted");
        this.userId = userId;
        this.question = question;
        this.correlationId = correlationId;
    }

    public String userId() {
        return userId;
    }

    public String question() {
        return question;
    }

    public String correlationId() {
        return correlationId;
    }
}
