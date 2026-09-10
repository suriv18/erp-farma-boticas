package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProductoReguladoRequest(
        @NotBlank @Size(min = 2, max = 40) String tipoProducto,
        @Size(max = 50) String rubroCodigo,
        @Size(max = 40) String tipoRegistro,
        @Size(max = 100) String numeroRegistro,
        @NotBlank @Size(min = 2, max = 500) String denominacion,
        @Size(max = 300) String concentracionTexto,
        @Size(max = 500) String presentacionRegulatoria,
        String formaFarmaceuticaCodigo,
        String viaAdministracionCodigo,
        String unidadMedidaCodigo,
        String condicionVentaCodigo,
        @Size(max = 30) String clasificacionAtc,
        String clasificacionControladaCodigo,
        @Size(max = 40) String tipoLiberacion,
        @Size(max = 40) String origenFabricacion,
        @Size(max = 100) String paisOrigen,
        @Size(max = 30) String subpartidaNacional,
        @Size(max = 300) String titularRegistro,
        @Size(max = 300) String fabricante,
        @Size(max = 300) String importador,
        @Size(max = 200) String establecimientoExpendio,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        @Size(max = 300) String fuente,
        @Size(max = 100) String versionFuente) {
}
