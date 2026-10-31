package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.AsociarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.AsociarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivoAsociado;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class AsociarPrincipioActivoHandler implements AsociarPrincipioActivoUseCase {

    private final ProductoReguladoPort writePort;

    public AsociarPrincipioActivoHandler(ProductoReguladoPort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ProductoReguladoResult, ApplicationError> execute(AsociarPrincipioActivoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = writePort.findById(command.productoReguladoId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRODUCTO_REGULADO_NO_ENCONTRADO", "El producto regulado indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }

        var asociado = new PrincipioActivoAsociado(
                new PrincipioActivoId(command.principioActivoId()), command.concentracionTexto(),
                command.cantidad(), command.unidadMedidaCodigo(), command.esPrincipal(), command.orden());
        var updated = existing.get().conPrincipioActivoAsociado(asociado);

        var outcome = writePort.save(updated);
        if (outcome == ProductoReguladoPort.SaveOutcome.PRINCIPIO_ACTIVO_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", "El principio activo indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(updated));
    }
}
