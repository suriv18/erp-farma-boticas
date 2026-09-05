package com.softprimesolutions.security.infrastructure.client.identity;

import com.softprimesolutions.security.api.PasswordResetRequested;
import com.softprimesolutions.security.application.port.out.PasswordResetNotificationPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringPasswordResetEventPublisher implements PasswordResetNotificationPort {

    private final ApplicationEventPublisher events;

    public SpringPasswordResetEventPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    @Override
    public void send(PasswordResetMessage message) {
        events.publishEvent(new PasswordResetRequested(
                message.tenantId(), message.userId(), message.destination(),
                message.resetToken(), message.expiresAt()));
    }
}
