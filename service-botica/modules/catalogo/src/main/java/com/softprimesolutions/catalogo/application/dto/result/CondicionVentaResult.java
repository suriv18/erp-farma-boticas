package com.softprimesolutions.catalogo.application.dto.result;

import java.time.LocalDate;

public record CondicionVentaResult(
        String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
        String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta, String estado) {
}
