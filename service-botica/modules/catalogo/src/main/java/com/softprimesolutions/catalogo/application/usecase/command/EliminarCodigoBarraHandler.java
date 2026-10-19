package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.EliminarCodigoBarraCommand;
import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.EliminarCodigoBarraUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class EliminarCodigoBarraHandler implements EliminarCodigoBarraUseCase {

    private final CatalogoComercialPort writePort;

    public EliminarCodigoBarraHandler(CatalogoComercialPort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<SkuResult, ApplicationError> execute(EliminarCodigoBarraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = writePort.findSkuById(command.tenantId(), command.skuId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "CAT_SKU_NO_ENCONTRADO", "El SKU indicado no existe.", ErrorCategory.NOT_FOUND));
        }

        var updated = existing.get().sinCodigoBarra(command.codigoBarra());
        writePort.save(updated);
        return Result.success(CatalogoApplicationMapper.toResult(updated));
    }
}
