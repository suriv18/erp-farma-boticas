package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.mapper.OrganizacionApplicationMapper;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearEmpresaOperadoraHandler implements CrearEmpresaOperadoraUseCase {

    private final OrganizacionWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearEmpresaOperadoraHandler(
            OrganizacionWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraResult, ApplicationError> execute(CrearEmpresaOperadoraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var empresa = EmpresaOperadora.create(
                new EmpresaOperadoraId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.ruc(), command.razonSocial(), command.nombreComercial(),
                command.direccionFiscal(), command.ubigeoFiscal(), command.telefono(),
                command.email(), command.sitioWeb(), command.monedaFuncional(),
                command.zonaHoraria(), command.permiteVentaOnline(), clock.now());
        return empresa.fold(this::persist, this::validationFailure);
    }

    private Result<EmpresaOperadoraResult, ApplicationError> persist(EmpresaOperadora empresa) {
        var outcome = writePort.save(empresa);
        if (outcome == OrganizacionWritePort.SaveEmpresaOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "ORG_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == OrganizacionWritePort.SaveEmpresaOutcome.DUPLICATE_RUC) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_RUC_DUPLICADO", "Ya existe una empresa con el RUC indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(OrganizacionApplicationMapper.toResult(empresa));
    }

    private Result<EmpresaOperadoraResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
