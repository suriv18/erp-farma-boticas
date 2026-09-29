package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.model.PerfilOperacion;
import com.softprimesolutions.organizacion.domain.model.TipoEstablecimiento;
import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarEstablecimientoHandler implements ActualizarEstablecimientoUseCase {

    private final OrganizacionReadPort readPort;
    private final OrganizacionWritePort writePort;
    private final ClockPort clock;

    public ActualizarEstablecimientoHandler(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort clock) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EstablecimientoResult, ApplicationError> execute(ActualizarEstablecimientoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var existing = readPort.findEstablecimientoById(command.tenantId(), command.establecimientoId());
        if (existing.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        var current = existing.get();
        var establecimiento = Establecimiento.restore(
                new EstablecimientoId(current.id()), new TenantId(current.tenantId()),
                new EmpresaOperadoraId(current.empresaId()), current.codigo(), current.nombre(),
                TipoEstablecimiento.valueOf(current.tipoEstablecimiento()), current.categoriaRegulatoriaCodigo(),
                current.codigoAnexoSunat(), current.codigoDigemid(), current.direccion(), current.ubigeo(),
                current.referencia(), current.latitud(), current.longitud(), current.telefono(),
                current.email(), current.esPrincipal(), current.permiteVentaOnline(), current.permiteDelivery(),
                PerfilOperacion.valueOf(current.perfilOperacion()), current.zonaHoraria(),
                EstadoEstablecimiento.valueOf(current.estadoOperativo()), current.createdAt(),
                current.updatedAt());

        var updated = establecimiento.updateDetails(
                command.nombre(), EstablecimientoEnums.tipoEstablecimiento(command.tipoEstablecimiento()),
                command.categoriaRegulatoriaCodigo(), command.codigoAnexoSunat(), command.codigoDigemid(),
                command.direccion(), command.ubigeo(), command.referencia(), command.latitud(),
                command.longitud(), command.telefono(), command.email(), command.esPrincipal(),
                command.permiteVentaOnline(), command.permiteDelivery(),
                EstablecimientoEnums.perfilOperacion(command.perfilOperacion()), command.zonaHoraria(),
                clock.now());

        return updated.fold(this::persist, this::validationFailure);
    }

    private Result<EstablecimientoResult, ApplicationError> persist(Establecimiento establecimiento) {
        writePort.save(establecimiento);
        return Result.success(OrganizacionApplicationMapper.toResult(establecimiento));
    }

    private Result<EstablecimientoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
