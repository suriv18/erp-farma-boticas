package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.DesasociarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.DesasociarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class DesasociarPrincipioActivoHandler implements DesasociarPrincipioActivoUseCase {

    private final ProductoReguladoPort writePort;

    public DesasociarPrincipioActivoHandler(ProductoReguladoPort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ProductoReguladoResult, ApplicationError> execute(DesasociarPrincipioActivoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = writePort.findById(command.productoReguladoId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRODUCTO_REGULADO_NO_ENCONTRADO", "El producto regulado indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }

        var updated = existing.get().sinPrincipioActivoAsociado(new PrincipioActivoId(command.principioActivoId()));
        writePort.save(updated);
        return Result.success(CatalogoApplicationMapper.toResult(updated));
    }
}
