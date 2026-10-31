package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record AsociarPrincipioActivoCommand(
        UUID productoReguladoId, UUID principioActivoId, String concentracionTexto, BigDecimal cantidad,
        String unidadMedidaCodigo, boolean esPrincipal, short orden) implements Command<ProductoReguladoResult> {
}
