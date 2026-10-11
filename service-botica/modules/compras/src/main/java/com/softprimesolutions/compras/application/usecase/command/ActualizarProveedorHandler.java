package com.softprimesolutions.compras.application.usecase.command;

import com.softprimesolutions.compras.application.dto.command.ActualizarProveedorCommand;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.in.ActualizarProveedorUseCase;
import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarProveedorHandler implements ActualizarProveedorUseCase {

    private final ProveedorWritePort writePort;
    private final ClockPort clock;

    public ActualizarProveedorHandler(ProveedorWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ProveedorResult, ApplicationError> execute(ActualizarProveedorCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return writePort.findById(command.tenantId(), command.proveedorId())
                .map(proveedor -> ComprasApplicationMapper.toDatos(command.proveedor()).fold(
                        datos -> guardar(proveedor.actualizar(datos, new Actor(command.actorId()), clock.now())),
                        error -> Result.<ProveedorResult, ApplicationError>failure(ComprasErrors.fromDomain(error))))
                .orElseGet(() -> Result.failure(ComprasErrors.proveedorNoEncontrado()));
    }

    private Result<ProveedorResult, ApplicationError> guardar(Proveedor proveedor) {
        if (writePort.actualizar(proveedor) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(ComprasErrors.proveedorDuplicado());
        }
        return Result.success(ComprasApplicationMapper.toResult(proveedor));
    }
}
