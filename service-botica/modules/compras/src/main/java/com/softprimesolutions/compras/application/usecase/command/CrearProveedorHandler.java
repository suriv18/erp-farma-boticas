package com.softprimesolutions.compras.application.usecase.command;

import com.softprimesolutions.compras.application.dto.command.CrearProveedorCommand;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.in.CrearProveedorUseCase;
import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearProveedorHandler implements CrearProveedorUseCase {

    private final ProveedorWritePort writePort;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public CrearProveedorHandler(ProveedorWritePort writePort, IdentifierGenerator identifiers, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ProveedorResult, ApplicationError> execute(CrearProveedorCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return ComprasApplicationMapper.toDatos(command.proveedor()).fold(
                datos -> guardar(Proveedor.crear(
                        identifiers.next(), command.tenantId(), datos, new Actor(command.actorId()), clock.now())),
                error -> Result.<ProveedorResult, ApplicationError>failure(ComprasErrors.fromDomain(error)));
    }

    private Result<ProveedorResult, ApplicationError> guardar(Proveedor proveedor) {
        if (writePort.insertar(proveedor) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(ComprasErrors.proveedorDuplicado());
        }
        return Result.success(ComprasApplicationMapper.toResult(proveedor));
    }
}
