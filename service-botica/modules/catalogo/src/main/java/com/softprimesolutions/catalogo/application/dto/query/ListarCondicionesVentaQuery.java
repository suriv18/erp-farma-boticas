package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarCondicionesVentaQuery(String estado) implements Query<List<CondicionVentaResult>> {
}
