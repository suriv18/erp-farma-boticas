package com.softprimesolutions.shared.kernel.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Base mínima para registrar eventos sin acoplar el dominio al publicador. */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected final void registerEvent(DomainEvent event) {
        domainEvents.add(Objects.requireNonNull(event, "event es obligatorio"));
    }

    public final List<DomainEvent> pullDomainEvents() {
        var published = List.copyOf(domainEvents);
        domainEvents.clear();
        return published;
    }
}
