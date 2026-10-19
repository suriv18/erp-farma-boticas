package com.softprimesolutions.compras.application.dto.command;

import java.util.UUID;

public record TransicionarOrdenCompraCommand(
        UUID tenantId, UUID ordenId, TransicionOrden transicion, String motivo, UUID actorId) {
}
