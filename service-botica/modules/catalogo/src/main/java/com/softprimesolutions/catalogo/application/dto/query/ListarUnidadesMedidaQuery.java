package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarUnidadesMedidaQuery(String estado) implements Query<List<UnidadMedidaResult>> {
}
