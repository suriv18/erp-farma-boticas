package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.AgregarCodigoBarraCommand;
import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.AgregarCodigoBarraUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CodigoBarraSku;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class AgregarCodigoBarraHandler implements AgregarCodigoBarraUseCase {

    private final CatalogoComercialPort writePort;

    public AgregarCodigoBarraHandler(CatalogoComercialPort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<SkuResult, ApplicationError> execute(AgregarCodigoBarraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = writePort.findSkuById(command.tenantId(), command.skuId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "CAT_SKU_NO_ENCONTRADO", "El SKU indicado no existe.", ErrorCategory.NOT_FOUND));
        }

        var codigo = new CodigoBarraSku(
                command.codigoBarra(), command.tipoCodigo() == null ? "EAN13" : command.tipoCodigo(), false,
                command.vigenteDesde(), command.vigenteHasta(), EstadoCatalogoSoporte.ACTIVO);
        var updated = existing.get().conCodigoBarra(codigo);

        var outcome = writePort.save(updated);
        if (outcome == CatalogoComercialPort.SaveSkuOutcome.DUPLICATE_CODIGO_BARRA) {
            return Result.failure(new StandardApplicationError(
                    "CAT_SKU_CODIGO_BARRA_DUPLICADO", "Ya existe un SKU con el código de barras indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(updated));
    }
}
