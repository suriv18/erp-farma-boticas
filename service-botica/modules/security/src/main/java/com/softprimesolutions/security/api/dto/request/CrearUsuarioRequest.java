package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearUsuarioRequest(
        @NotNull UUID tenantId,
        @Size(max = 20) String documentType,
        @Size(max = 30) String documentNumber,
        @Size(max = 150) String firstNames,
        @Size(max = 180) String lastNames,
        @Size(max = 150) String username,
        @Email @Size(max = 254) String email,
        @Size(max = 40) String phone,
        @Size(max = 250) String displayName,
        Boolean credentialChangeRequired,
        Boolean mfaRequired) {
}
