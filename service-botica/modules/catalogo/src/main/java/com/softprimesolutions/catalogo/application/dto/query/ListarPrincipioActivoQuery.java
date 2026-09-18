package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarPrincipioActivoQuery(String texto, String estado) implements Query<List<PrincipioActivoResult>> {
}
