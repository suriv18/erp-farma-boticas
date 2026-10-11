package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarFormaFarmaceuticaQuery(String codigo) implements Query<FormaFarmaceuticaResult> {
}
