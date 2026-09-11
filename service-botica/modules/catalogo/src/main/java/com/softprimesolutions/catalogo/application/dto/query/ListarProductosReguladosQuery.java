package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ListarProductosReguladosQuery(
        String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size)
        implements Query<PaginaResult<ProductoReguladoResumen>> {
}
