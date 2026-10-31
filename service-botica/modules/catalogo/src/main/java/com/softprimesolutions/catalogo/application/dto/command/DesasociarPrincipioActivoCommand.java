package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record DesasociarPrincipioActivoCommand(UUID productoReguladoId, UUID principioActivoId)
        implements Command<ProductoReguladoResult> {
}
