package com.softprimesolutions.security.application.dto.command;

import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearUsuarioCommand(
        UUID tenantId,
        String identityProvider,
        String identitySubject,
        String identityIssuer,
        String emailClaim,
        String documentType,
        String documentNumber,
        String firstNames,
        String lastNames,
        String username,
        String email,
        String phone,
        String displayName,
        boolean credentialChangeRequired,
        boolean mfaRequired) implements Command<UsuarioResult> {
}
