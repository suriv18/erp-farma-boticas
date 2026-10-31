package com.softprimesolutions.organizacion.api.dto.response;

import java.util.List;
import java.util.UUID;

public record MiPuestoResponse(List<EstablecimientoPuesto> establecimientos) {

    public record EstablecimientoPuesto(
            UUID id, String codigo, String nombre, List<AlmacenPuesto> almacenes, List<TerminalPuesto> terminales) {
    }

    public record AlmacenPuesto(UUID id, String codigo, String nombre, String tipo, boolean permiteVenta) {
    }

    public record TerminalPuesto(UUID id, String codigo, String nombre, String estado, boolean operable) {
    }
}
