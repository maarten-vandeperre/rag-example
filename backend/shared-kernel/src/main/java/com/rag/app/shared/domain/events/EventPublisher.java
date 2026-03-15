package com.rag.app.shared.domain.events;

import java.util.List;

public interface EventPublisher {
    void publish(DomainEvent event);

    void publishAll(List<DomainEvent> events);
}
