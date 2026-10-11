package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarUnidadMedidaQuery(String codigo) implements Query<UnidadMedidaResult> {
}
