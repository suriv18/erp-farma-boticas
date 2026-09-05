package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VincularIdentidadExternaRequest(
        @NotBlank @Size(max = 100) String provider,
        @NotBlank @Size(max = 300) String subject,
        @Size(max = 500) String issuer,
        @Email @Size(max = 254) String emailClaim) {
}
