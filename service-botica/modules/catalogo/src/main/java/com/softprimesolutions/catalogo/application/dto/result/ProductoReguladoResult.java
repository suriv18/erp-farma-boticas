package com.softprimesolutions.catalogo.application.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProductoReguladoResult(
        UUID id,
        String tipoProducto,
        String rubroCodigo,
        String tipoRegistro,
        String numeroRegistro,
        String denominacion,
        String concentracionTexto,
        String presentacionRegulatoria,
        String formaFarmaceuticaCodigo,
        String viaAdministracionCodigo,
        String unidadMedidaCodigo,
        String condicionVentaCodigo,
        String clasificacionAtc,
        String clasificacionControladaCodigo,
        String tipoLiberacion,
        String origenFabricacion,
        String paisOrigen,
        String subpartidaNacional,
        String titularRegistro,
        String fabricante,
        String importador,
        String establecimientoExpendio,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        String fuente,
        String versionFuente,
        List<PrincipioActivoAsociadoResult> principiosActivos,
        String estadoRegulatorio,
        Instant createdAt,
        Instant updatedAt) {

    public ProductoReguladoResult {
        principiosActivos = List.copyOf(principiosActivos);
    }
}
