package com.softprimesolutions.security.application.dto.query;

import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarPermisosQuery(String search) implements Query<List<PermisoResult>> {
}
