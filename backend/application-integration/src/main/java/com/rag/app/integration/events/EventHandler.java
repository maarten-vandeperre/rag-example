package com.rag.app.integration.events;

import com.rag.app.shared.domain.events.DomainEvent;

@FunctionalInterface
public interface EventHandler<T extends DomainEvent> {
    void handle(T event);
}
