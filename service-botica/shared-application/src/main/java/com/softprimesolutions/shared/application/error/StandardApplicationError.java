package com.softprimesolutions.shared.application.error;

import java.util.Map;
import java.util.Objects;

public record StandardApplicationError(
        String code,
        String message,
        ErrorCategory category,
        Map<String, Object> metadata) implements ApplicationError {

    public StandardApplicationError {
        Objects.requireNonNull(code, "code es obligatorio");
        Objects.requireNonNull(message, "message es obligatorio");
        Objects.requireNonNull(category, "category es obligatorio");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata es obligatorio"));
    }

    public StandardApplicationError(String code, String message, ErrorCategory category) {
        this(code, message, category, Map.of());
    }
}
