package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CrearEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CrearEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
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

public final class CrearEstablecimientoHandler implements CrearEstablecimientoUseCase {

    private final OrganizacionWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearEstablecimientoHandler(
            OrganizacionWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EstablecimientoResult, ApplicationError> execute(CrearEstablecimientoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var establecimiento = Establecimiento.create(
                new EstablecimientoId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.empresaId() == null ? null : new EmpresaOperadoraId(command.empresaId()),
                command.codigo(), command.nombre(),
                EstablecimientoEnums.tipoEstablecimiento(command.tipoEstablecimiento()),
                command.categoriaRegulatoriaCodigo(), command.codigoAnexoSunat(), command.codigoDigemid(),
                command.direccion(), command.ubigeo(), command.referencia(), command.latitud(),
                command.longitud(), command.telefono(), command.email(), command.esPrincipal(),
                command.permiteVentaOnline(), command.permiteDelivery(),
                EstablecimientoEnums.perfilOperacion(command.perfilOperacion()), command.zonaHoraria(),
                clock.now());
        return establecimiento.fold(this::persist, this::validationFailure);
    }

    private Result<EstablecimientoResult, ApplicationError> persist(Establecimiento establecimiento) {
        var outcome = writePort.save(establecimiento);
        if (outcome == OrganizacionWritePort.SaveEstablecimientoOutcome.EMPRESA_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_NO_ENCONTRADA", "La empresa indicada no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == OrganizacionWritePort.SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_NO_OPERATIVA",
                    "La empresa no está operativa (suspendida o bloqueada); no admite establecimientos nuevos.",
                    ErrorCategory.CONFLICT));
        }
        if (outcome == OrganizacionWritePort.SaveEstablecimientoOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_CODIGO_DUPLICADO",
                    "Ya existe un establecimiento con el código indicado.", ErrorCategory.CONFLICT));
        }
        if (outcome == OrganizacionWritePort.SaveEstablecimientoOutcome.DUPLICATE_DIGEMID) {
            return Result.failure(new StandardApplicationError(
                    "ORG_ESTABLECIMIENTO_DIGEMID_DUPLICADO",
                    "Ya existe un establecimiento con el código DIGEMID indicado.", ErrorCategory.CONFLICT));
        }
        return Result.success(OrganizacionApplicationMapper.toResult(establecimiento));
    }

    private Result<EstablecimientoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
