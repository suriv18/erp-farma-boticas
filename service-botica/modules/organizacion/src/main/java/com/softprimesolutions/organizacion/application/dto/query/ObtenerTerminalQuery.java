package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerTerminalQuery(UUID tenantId, UUID terminalId) {
}
