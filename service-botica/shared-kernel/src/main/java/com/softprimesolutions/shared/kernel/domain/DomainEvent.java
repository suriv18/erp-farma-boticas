package com.softprimesolutions.shared.kernel.domain;

import java.time.Instant;

/** Hecho inmutable ocurrido dentro de un agregado. */
public interface DomainEvent {

    Instant occurredOn();
}
