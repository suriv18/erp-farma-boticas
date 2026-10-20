package com.softprimesolutions.organizacion.application.dto.result;

import java.util.UUID;

public record AlmacenPuestoResult(UUID id, String codigo, String nombre, String tipo, boolean permiteVenta) {
}
