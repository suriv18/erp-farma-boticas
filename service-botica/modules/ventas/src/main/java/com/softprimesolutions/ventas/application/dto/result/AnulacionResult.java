package com.softprimesolutions.ventas.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record AnulacionResult(Instant anuladaAt, UUID anuladaPorId, String motivo) {
}
