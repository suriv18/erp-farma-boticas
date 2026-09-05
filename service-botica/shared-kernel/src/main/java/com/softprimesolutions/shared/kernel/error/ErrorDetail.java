package com.softprimesolutions.shared.kernel.error;

import java.util.Map;
import java.util.Objects;

/** Error portable que no depende de HTTP, persistencia ni Spring. */
public record ErrorDetail(String code, String message, Map<String, Object> metadata) {

    public ErrorDetail {
        Objects.requireNonNull(code, "code es obligatorio");
        Objects.requireNonNull(message, "message es obligatorio");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata es obligatorio"));
    }

    public ErrorDetail(String code, String message) {
        this(code, message, Map.of());
    }
}
