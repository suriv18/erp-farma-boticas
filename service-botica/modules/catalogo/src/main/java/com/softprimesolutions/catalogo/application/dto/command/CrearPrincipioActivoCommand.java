package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearPrincipioActivoCommand(
        String codigoFuente, String denominacion, String nombreNormalizado, String fuente)
        implements Command<PrincipioActivoResult> {
}
