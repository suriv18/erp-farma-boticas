package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearViaAdministracionCommand(String codigo, String denominacion, String fuente)
        implements Command<ViaAdministracionResult> {
}
