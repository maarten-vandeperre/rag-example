package com.rag.app.integration.events;

import com.rag.app.shared.domain.events.DomainEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class EventBus implements AutoCloseable {
    private final Map<Class<? extends DomainEvent>, List<EventHandler<? extends DomainEvent>>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public EventBus() {
        this.executor = Executors.newCachedThreadPool();
    }

    public EventBus(ExecutorService executor) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    public <T extends DomainEvent> void register(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, ignored -> new CopyOnWriteArrayList<>()).add(handler);
    }

    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> void publish(T event) {
        List<EventHandler<? extends DomainEvent>> registeredHandlers = handlers.getOrDefault(event.getClass(), List.of());
        for (EventHandler<? extends DomainEvent> registeredHandler : registeredHandlers) {
            EventHandler<T> handler = (EventHandler<T>) registeredHandler;
            executor.submit(() -> handler.handle(event));
        }
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
