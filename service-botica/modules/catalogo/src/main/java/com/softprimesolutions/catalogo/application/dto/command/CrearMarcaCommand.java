package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearMarcaCommand(UUID tenantId, String codigo, String nombre, String descripcion)
        implements Command<MarcaResult> {
}
