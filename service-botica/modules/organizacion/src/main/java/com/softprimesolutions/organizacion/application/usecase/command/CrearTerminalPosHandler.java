package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CrearTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CrearTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearTerminalPosHandler implements CrearTerminalPosUseCase {

    private final OrganizacionWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearTerminalPosHandler(
            OrganizacionWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<TerminalPosResult, ApplicationError> execute(CrearTerminalPosCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var terminal = TerminalPos.create(
                new TerminalPosId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.establecimientoId() == null ? null : new EstablecimientoId(command.establecimientoId()),
                command.codigo(), command.nombre(), command.serieBoletaDefecto(),
                command.serieFacturaDefecto(), command.numeroSerieEquipo(), command.hostname(),
                command.ipEquipo(), command.impresoraCodigo(), command.storeEdgeHabilitado(), clock.now());
        return terminal.fold(this::persist, this::validationFailure);
    }

    private Result<TerminalPosResult, ApplicationError> persist(TerminalPos terminal) {
        var outcome = writePort.save(terminal);
        if (outcome == OrganizacionWritePort.SaveTerminalOutcome.ESTABLECIMIENTO_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        if (outcome == OrganizacionWritePort.SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_OPERATIVO",
                    "El establecimiento no está operativo (suspendido o clausurado); no admite terminales POS nuevos.",
                    ErrorCategory.CONFLICT));
        }
        if (outcome == OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_TERMINAL_CODIGO_DUPLICADO", "Ya existe un terminal con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        var conflict = TerminalSaveErrors.conflict(outcome, terminal);
        if (conflict.isPresent()) return Result.failure(conflict.get());
        return Result.success(OrganizacionApplicationMapper.toResult(terminal));
    }

    private Result<TerminalPosResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
