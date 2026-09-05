package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearUsuarioRequest(
        @jakarta.validation.constraints.NotNull UUID tenantId,
        @NotBlank @Size(max = 100) String identityProvider,
        @NotBlank @Size(max = 300) String identitySubject,
        @Size(max = 500) String identityIssuer,
        @Email @Size(max = 254) String emailClaim,
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
