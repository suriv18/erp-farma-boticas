package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CrearAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CrearAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearAlmacenHandler implements CrearAlmacenUseCase {

    private final OrganizacionWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearAlmacenHandler(
            OrganizacionWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<AlmacenResult, ApplicationError> execute(CrearAlmacenCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var almacen = Almacen.create(
                new AlmacenId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.establecimientoId() == null ? null : new EstablecimientoId(command.establecimientoId()),
                command.codigo(), command.nombre(), AlmacenTerminalEnums.tipoAlmacen(command.tipo()),
                command.permiteLotes(), command.permiteVencimiento(), command.permiteVenta(),
                command.permiteDespacho(), command.controlTemperatura(), command.temperaturaMinC(),
                command.temperaturaMaxC(), clock.now());
        return almacen.fold(this::persist, this::validationFailure);
    }

    private Result<AlmacenResult, ApplicationError> persist(Almacen almacen) {
        var outcome = writePort.save(almacen);
        if (outcome == OrganizacionWritePort.SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        if (outcome == OrganizacionWritePort.SaveAlmacenOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ALMACEN_CODIGO_DUPLICADO", "Ya existe un almacén con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(OrganizacionApplicationMapper.toResult(almacen));
    }

    private Result<AlmacenResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
