package com.softprimesolutions.catalogo.api.dto.response;

import java.time.LocalDate;

public record CondicionVentaResponse(
        String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
        String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta, String estado) {
}
