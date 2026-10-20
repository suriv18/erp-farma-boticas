package com.softprimesolutions.organizacion.application.dto.result;

import java.util.List;
import java.util.UUID;

public record EstablecimientoPuestoResult(
        UUID id,
        UUID empresaId,
        String codigo,
        String nombre,
        List<AlmacenPuestoResult> almacenes,
        List<TerminalPuestoResult> terminales) {

    public EstablecimientoPuestoResult {
        almacenes = List.copyOf(almacenes);
        terminales = List.copyOf(terminales);
    }
}
