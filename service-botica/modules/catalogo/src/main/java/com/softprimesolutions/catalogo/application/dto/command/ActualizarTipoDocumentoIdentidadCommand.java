package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record ActualizarTipoDocumentoIdentidadCommand(
        String codigo, String sigla, String denominacion, Integer max, Integer min)
        implements Command<TipoDocumentoIdentidadResult> {
}
