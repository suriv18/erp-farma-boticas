package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarViasAdministracionQuery(String estado) implements Query<List<ViaAdministracionResult>> {
}
