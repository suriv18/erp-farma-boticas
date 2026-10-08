package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.ActualizarAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarAlmacenHandler implements ActualizarAlmacenUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public ActualizarAlmacenHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<AlmacenResult, ApplicationError> execute(ActualizarAlmacenCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = readPort.findAlmacenById(command.tenantId(), command.almacenId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ALMACEN_NO_ENCONTRADO", "El almacén indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        var current = existing.get();
        var almacen = Almacen.restore(
                new AlmacenId(current.id()), new TenantId(current.tenantId()),
                new EstablecimientoId(current.establecimientoId()), current.codigo(), current.nombre(),
                AlmacenTerminalEnums.tipoAlmacen(current.tipo()), current.permiteLotes(),
                current.permiteVencimiento(), current.permiteVenta(), current.permiteDespacho(),
                current.controlTemperatura(), current.temperaturaMinC(), current.temperaturaMaxC(),
                current.activo(), current.createdAt(), current.updatedAt());

        var updated = almacen.updateDetails(
                command.nombre(), AlmacenTerminalEnums.tipoAlmacen(command.tipo()), command.permiteLotes(),
                command.permiteVencimiento(), command.permiteVenta(), command.permiteDespacho(),
                command.controlTemperatura(), command.temperaturaMinC(), command.temperaturaMaxC(),
                clock.now());

        return updated.flatMap(candidate -> applyActivo(candidate, command.activo()))
                .fold(this::persist, this::validationFailure);
    }

    private Result<Almacen, ErrorDetail> applyActivo(Almacen almacen, boolean activo) {
        if (almacen.activo() == activo) {
            return Result.success(almacen);
        }
        return activo ? almacen.activar(clock.now()) : almacen.desactivar(clock.now());
    }

    private Result<AlmacenResult, ApplicationError> persist(Almacen almacen) {
        writePort.save(almacen);
        return Result.success(OrganizacionApplicationMapper.toResult(almacen));
    }

    private Result<AlmacenResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
