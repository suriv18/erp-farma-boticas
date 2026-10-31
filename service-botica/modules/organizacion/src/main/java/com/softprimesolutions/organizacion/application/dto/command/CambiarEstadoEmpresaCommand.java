package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CambiarEstadoEmpresaCommand(UUID empresaId, UUID tenantId, String estado) {
}
