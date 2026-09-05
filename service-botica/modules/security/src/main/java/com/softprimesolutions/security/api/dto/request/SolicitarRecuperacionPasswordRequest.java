package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SolicitarRecuperacionPasswordRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(max = 254) String login) {
}
