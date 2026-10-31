package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record ActualizarCategoriaProductoCommand(
        UUID tenantId, UUID categoriaId, UUID categoriaPadreId, String codigo, String nombre,
        String descripcion, int nivel, int orden) implements Command<CategoriaProductoResult> {
}
