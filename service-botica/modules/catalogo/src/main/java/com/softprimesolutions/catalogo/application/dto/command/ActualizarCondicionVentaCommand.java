package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.LocalDate;

public record ActualizarCondicionVentaCommand(
        String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
        String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta)
        implements Command<CondicionVentaResult> {
}
