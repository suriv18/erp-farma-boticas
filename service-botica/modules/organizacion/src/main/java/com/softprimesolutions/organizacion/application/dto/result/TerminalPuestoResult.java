package com.softprimesolutions.organizacion.application.dto.result;

import java.util.UUID;

public record TerminalPuestoResult(UUID id, String codigo, String nombre, String estado, boolean operable) {
}
