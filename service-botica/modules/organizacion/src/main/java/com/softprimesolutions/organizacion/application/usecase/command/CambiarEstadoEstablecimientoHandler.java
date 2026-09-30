package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CambiarEstadoEstablecimientoHandler implements CambiarEstadoEstablecimientoUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public CambiarEstadoEstablecimientoHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EstablecimientoResult, ApplicationError> execute(CambiarEstadoEstablecimientoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var estado = EnumParser.parse(EstadoEstablecimiento.class, command.estado());
        if (estado.isEmpty()) {
            return EstadoErrors.invalidEstado("ORG_ESTABLECIMIENTO_ESTADO_INVALIDO", EstadoEstablecimiento.class);
        }
        var existing = readPort.findEstablecimientoById(command.tenantId(), command.establecimientoId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }

        return OrganizacionApplicationMapper.toDomain(existing.get())
                .cambiarEstadoOperativo(estado.get(), clock.now())
                .fold(this::persist, EstadoErrors::validationFailure);
    }

    private Result<EstablecimientoResult, ApplicationError> persist(Establecimiento establecimiento) {
        writePort.save(establecimiento);
        return Result.success(OrganizacionApplicationMapper.toResult(establecimiento));
    }
}
