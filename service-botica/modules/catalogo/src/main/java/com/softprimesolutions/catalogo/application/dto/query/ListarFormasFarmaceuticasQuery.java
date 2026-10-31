package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarFormasFarmaceuticasQuery(String estado) implements Query<List<FormaFarmaceuticaResult>> {
}
