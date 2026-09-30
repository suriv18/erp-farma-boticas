package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.ActualizarTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarTerminalPosHandler implements ActualizarTerminalPosUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public ActualizarTerminalPosHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<TerminalPosResult, ApplicationError> execute(ActualizarTerminalPosCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = readPort.findTerminalById(command.tenantId(), command.terminalId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_TERMINAL_NO_ENCONTRADO", "El terminal indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        var current = existing.get();
        var terminal = TerminalPos.restore(
                new TerminalPosId(current.id()), new TenantId(current.tenantId()),
                new EstablecimientoId(current.establecimientoId()), current.codigo(), current.nombre(),
                current.serieBoletaDefecto(), current.serieFacturaDefecto(), current.numeroSerieEquipo(),
                current.hostname(), current.ipEquipo(), current.impresoraCodigo(),
                current.storeEdgeHabilitado(), AlmacenTerminalEnums.estadoTerminalPos(current.estado()),
                current.createdAt(), current.updatedAt());

        var updated = terminal.updateDetails(
                command.nombre(), command.serieBoletaDefecto(), command.serieFacturaDefecto(),
                command.numeroSerieEquipo(), command.hostname(), command.ipEquipo(),
                command.impresoraCodigo(), command.storeEdgeHabilitado(), clock.now());

        return updated.flatMap(candidate -> applyEstado(candidate, command.estado()))
                .fold(this::persist, this::validationFailure);
    }

    private Result<TerminalPos, ErrorDetail> applyEstado(TerminalPos terminal, String estado) {
        var nuevoEstado = AlmacenTerminalEnums.estadoTerminalPos(estado);
        if (terminal.estado().equals(nuevoEstado)) {
            return Result.success(terminal);
        }
        return terminal.cambiarEstado(nuevoEstado, clock.now());
    }

    private Result<TerminalPosResult, ApplicationError> persist(TerminalPos terminal) {
        var outcome = writePort.save(terminal);
        var seriesConflict = TerminalSaveErrors.seriesConflict(outcome, terminal);
        if (seriesConflict.isPresent()) return Result.failure(seriesConflict.get());
        return Result.success(OrganizacionApplicationMapper.toResult(terminal));
    }

    private Result<TerminalPosResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
