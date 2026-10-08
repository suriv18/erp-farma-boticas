package com.softprimesolutions.compras.application.usecase.command;

import com.softprimesolutions.compras.application.dto.command.CambiarEstadoProveedorCommand;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.in.CambiarEstadoProveedorUseCase;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CambiarEstadoProveedorHandler implements CambiarEstadoProveedorUseCase {

    private final ProveedorWritePort writePort;
    private final ClockPort clock;

    public CambiarEstadoProveedorHandler(ProveedorWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ProveedorResult, ApplicationError> execute(CambiarEstadoProveedorCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return EstadoProveedor.desde(command.estado())
                .map(estado -> writePort.findById(command.tenantId(), command.proveedorId())
                        .map(proveedor -> proveedor.cambiarEstado(estado, new Actor(command.actorId()), clock.now()))
                        .map(this::guardar)
                        .orElseGet(() -> Result.failure(ComprasErrors.proveedorNoEncontrado())))
                .orElseGet(() -> Result.failure(ComprasErrors.estadoInvalido()));
    }

    private Result<ProveedorResult, ApplicationError> guardar(Proveedor proveedor) {
        writePort.actualizar(proveedor);
        return Result.success(ComprasApplicationMapper.toResult(proveedor));
    }
}
