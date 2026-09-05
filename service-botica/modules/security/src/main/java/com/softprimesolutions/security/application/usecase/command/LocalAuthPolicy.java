package com.softprimesolutions.security.application.usecase.command;

import java.time.Duration;

public record LocalAuthPolicy(
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration passwordResetTtl,
        int maxFailedAttempts,
        Duration lockDuration) {

    public LocalAuthPolicy {
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("accessTokenTtl debe ser positivo");
        }
        if (accessTokenTtl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("accessTokenTtl no debe exceder una hora");
        }
        if (refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
            throw new IllegalArgumentException("refreshTokenTtl debe ser positivo");
        }
        if (refreshTokenTtl.compareTo(Duration.ofDays(90)) > 0) {
            throw new IllegalArgumentException("refreshTokenTtl no debe exceder 90 dias");
        }
        if (passwordResetTtl == null || passwordResetTtl.isNegative() || passwordResetTtl.isZero()) {
            throw new IllegalArgumentException("passwordResetTtl debe ser positivo");
        }
        if (passwordResetTtl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalArgumentException("passwordResetTtl no debe exceder una hora");
        }
        if (maxFailedAttempts < 1) throw new IllegalArgumentException("maxFailedAttempts debe ser positivo");
        if (lockDuration == null || lockDuration.isNegative() || lockDuration.isZero()) {
            throw new IllegalArgumentException("lockDuration debe ser positivo");
        }
    }
}
