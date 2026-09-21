package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarCondicionVentaQuery(String codigo) implements Query<CondicionVentaResult> {
}
