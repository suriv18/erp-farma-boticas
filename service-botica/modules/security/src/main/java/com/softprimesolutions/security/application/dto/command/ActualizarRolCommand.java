package com.softprimesolutions.security.application.dto.command;

import java.util.UUID;

public record ActualizarRolCommand(UUID roleId, String code, String name, String description, String roleType) {
}
