package com.softprimesolutions.security.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LocalAuthPolicyTest {

    @Test
    void acceptsSecureOperationalLimits() {
        assertDoesNotThrow(() -> new LocalAuthPolicy(
                Duration.ofMinutes(10), Duration.ofDays(7), Duration.ofMinutes(30),
                5, Duration.ofMinutes(15)));
    }

    @Test
    void rejectsNonPositiveDurationsAndAttemptLimit() {
        assertThrows(IllegalArgumentException.class, () -> policy(null, Duration.ofDays(7),
                Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofSeconds(-1), Duration.ofDays(7),
                Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ZERO, Duration.ofDays(7),
                Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), null,
                Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofSeconds(-1),
                Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ZERO,
                Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofDays(7),
                null, 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofDays(7),
                Duration.ofSeconds(-1), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofDays(7),
                Duration.ZERO, 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofDays(7),
                Duration.ofMinutes(30), 0, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofDays(7),
                Duration.ofMinutes(30), 5, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofDays(7),
                Duration.ofMinutes(30), 5, null));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10), Duration.ofDays(7),
                Duration.ofMinutes(30), 5, Duration.ofSeconds(-1)));
    }

    @Test
    void rejectsExcessiveTokenLifetimes() {
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofHours(1).plusSeconds(1),
                Duration.ofDays(7), Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10),
                Duration.ofDays(90).plusSeconds(1), Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
        assertThrows(IllegalArgumentException.class, () -> policy(Duration.ofMinutes(10),
                Duration.ofDays(7), Duration.ofHours(1).plusSeconds(1), 5, Duration.ofMinutes(15)));
    }

    private static LocalAuthPolicy policy(
            Duration access, Duration refresh, Duration reset, int attempts, Duration lock) {
        return new LocalAuthPolicy(access, refresh, reset, attempts, lock);
    }
}
