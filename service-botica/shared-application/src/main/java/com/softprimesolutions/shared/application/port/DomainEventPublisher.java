package com.softprimesolutions.shared.application.port;

import com.softprimesolutions.shared.kernel.domain.DomainEvent;

@FunctionalInterface
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
