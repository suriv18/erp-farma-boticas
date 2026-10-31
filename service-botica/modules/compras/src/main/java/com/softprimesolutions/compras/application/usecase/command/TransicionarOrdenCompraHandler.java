package com.softprimesolutions.compras.application.usecase.command;

import com.softprimesolutions.compras.application.dto.command.TransicionarOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.in.TransicionarOrdenCompraUseCase;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class TransicionarOrdenCompraHandler implements TransicionarOrdenCompraUseCase {

    private final OrdenCompraWritePort ordenes;
    private final ClockPort clock;

    public TransicionarOrdenCompraHandler(OrdenCompraWritePort ordenes, ClockPort clock) {
        this.ordenes = Objects.requireNonNull(ordenes, "ordenes es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<OrdenCompraResult, ApplicationError> execute(TransicionarOrdenCompraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return ordenes.findById(command.tenantId(), command.ordenId())
                .map(orden -> transicionar(command, orden).fold(
                        nueva -> guardar(orden, nueva),
                        error -> Result.<OrdenCompraResult, ApplicationError>failure(ComprasErrors.fromDomain(error))))
                .orElseGet(() -> Result.failure(ComprasErrors.ordenNoEncontrada()));
    }

    private Result<OrdenCompraResult, ApplicationError> guardar(OrdenCompra previa, OrdenCompra nueva) {
        if (!ordenes.actualizarEstado(nueva, previa.estado())) {
            return Result.failure(ComprasErrors.modificacionConcurrente());
        }
        return Result.success(ComprasApplicationMapper.toResult(nueva));
    }

    private Result<OrdenCompra, ErrorDetail> transicionar(TransicionarOrdenCompraCommand command, OrdenCompra orden) {
        var actor = new Actor(command.actorId());
        var ahora = clock.now();
        return switch (command.transicion()) {
            case APROBAR -> orden.aprobar(actor, ahora);
            case EMITIR -> orden.emitir(actor, ahora);
            case ANULAR -> orden.anular(command.motivo(), actor, ahora);
        };
    }
}
