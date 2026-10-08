package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitarRecuperacionPasswordRequest(
        @NotBlank @Size(max = 254) String login) {
}
