package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.mapper.InventarioApplicationMapper;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public final class CambioEstadoLote {

    private final InventarioWritePort writePort;
    private final ClockPort clock;

    public CambioEstadoLote(InventarioWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    public Result<LoteResult, ApplicationError> aplicar(UUID tenantId, UUID loteId, Transicion transicion) {
        var ahora = clock.now();
        var hoy = LocalDate.ofInstant(ahora, ZoneOffset.UTC);
        return writePort.findLote(tenantId, loteId)
                .map(lote -> transicion.sobre(lote, ahora, hoy).fold(
                        nuevo -> persistir(lote, nuevo, hoy),
                        error -> Result.<LoteResult, ApplicationError>failure(InventarioErrors.fromDomain(error))))
                .orElseGet(() -> Result.failure(InventarioErrors.loteNoEncontrado()));
    }

    private Result<LoteResult, ApplicationError> persistir(Lote previo, Lote nuevo, LocalDate hoy) {
        if (!writePort.actualizarEstado(nuevo, previo.estado())) {
            return Result.failure(InventarioErrors.modificacionConcurrente());
        }
        return Result.success(InventarioApplicationMapper.toResult(nuevo, hoy));
    }

    @FunctionalInterface
    public interface Transicion {
        Result<Lote, ErrorDetail> sobre(Lote lote, Instant ahora, LocalDate hoy);
    }
}
