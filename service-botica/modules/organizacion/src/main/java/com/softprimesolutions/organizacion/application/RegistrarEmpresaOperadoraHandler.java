package com.softprimesolutions.organizacion.application;

import com.softprimesolutions.organizacion.api.EmpresaOperadoraRegistrada;
import com.softprimesolutions.organizacion.api.RegistrarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.port.EmpresaOperadoraRepository;
import com.softprimesolutions.organizacion.domain.empresa.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.empresa.EmpresaOperadoraId;
import com.softprimesolutions.shared.application.cqrs.CommandHandler;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class RegistrarEmpresaOperadoraHandler
        implements CommandHandler<RegistrarEmpresaOperadoraCommand, EmpresaOperadoraRegistrada> {

    private final EmpresaOperadoraRepository repository;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public RegistrarEmpresaOperadoraHandler(
            EmpresaOperadoraRepository repository,
            IdentifierGenerator identifierGenerator,
            ClockPort clock) {
        this.repository = Objects.requireNonNull(repository, "repository es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(
                identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<EmpresaOperadoraRegistrada, ApplicationError> handle(
            RegistrarEmpresaOperadoraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");

        var company = EmpresaOperadora.register(
                new EmpresaOperadoraId(identifierGenerator.next()),
                command.identifierType(),
                command.identifierValue(),
                command.legalName(),
                command.tradeName(),
                clock.now());

        return company.fold(this::persist, this::validationFailure);
    }

    private Result<EmpresaOperadoraRegistrada, ApplicationError> persist(
            EmpresaOperadora company) {
        var outcome = repository.save(company);
        if (outcome == EmpresaOperadoraRepository.SaveOutcome.DUPLICATE_IDENTIFIER) {
            return Result.failure(new StandardApplicationError(
                    "ORG_EMPRESA_DUPLICADA",
                    "Ya existe una empresa operadora con el identificador indicado.",
                    ErrorCategory.CONFLICT,
                    Map.of("identifierType", company.identifierType())));
        }

        return Result.success(new EmpresaOperadoraRegistrada(
                company.id().value(),
                company.identifierType(),
                company.identifierValue(),
                company.legalName(),
                company.tradeName(),
                company.status().name(),
                company.registeredAt()));
    }

    private Result<EmpresaOperadoraRegistrada, ApplicationError> validationFailure(
            ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
