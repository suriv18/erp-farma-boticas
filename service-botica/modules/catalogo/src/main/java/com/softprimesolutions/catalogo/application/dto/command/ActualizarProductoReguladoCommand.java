package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;
import java.util.UUID;

public record ActualizarProductoReguladoCommand(
        UUID productoReguladoId,
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
        String versionFuente) implements Command<ProductoReguladoResult> {
}
