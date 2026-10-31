package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CambiarEstadoEstablecimientoCommand(UUID establecimientoId, UUID tenantId, String estado) {
}
