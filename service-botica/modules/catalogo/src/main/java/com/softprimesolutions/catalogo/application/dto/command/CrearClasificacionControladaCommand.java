package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.cqrs.Command;

public record CrearClasificacionControladaCommand(
        String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
        boolean retieneReceta, Integer vigenciaRecetaDias) implements Command<ClasificacionControladaResult> {
}
