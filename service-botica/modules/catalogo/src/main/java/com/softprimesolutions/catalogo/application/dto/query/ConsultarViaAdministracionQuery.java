package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarViaAdministracionQuery(String codigo) implements Query<ViaAdministracionResult> {
}
