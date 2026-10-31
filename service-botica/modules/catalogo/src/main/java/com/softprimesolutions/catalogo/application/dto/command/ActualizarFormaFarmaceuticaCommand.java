package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarFormaFarmaceuticaCommand(String codigo, String denominacion, String fuente)
        implements Command<FormaFarmaceuticaResult> {
}
