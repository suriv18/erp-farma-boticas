package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarUnidadMedidaCommand(
        String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente)
        implements Command<UnidadMedidaResult> {
}
