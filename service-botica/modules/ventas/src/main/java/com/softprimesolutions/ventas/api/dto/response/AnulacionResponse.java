package com.softprimesolutions.ventas.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AnulacionResponse(Instant anuladaAt, UUID anuladaPorId, String motivo) {
}
