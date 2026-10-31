package com.softprimesolutions.clientes.application.dto.command;

import java.util.UUID;

public record CambiarEstadoClienteCommand(UUID tenantId, UUID clienteId, String estado, String motivo) {
}
