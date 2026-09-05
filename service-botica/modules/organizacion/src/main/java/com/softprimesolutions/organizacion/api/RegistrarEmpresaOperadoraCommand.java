package com.softprimesolutions.organizacion.api;

import com.softprimesolutions.shared.application.cqrs.Command;

public record RegistrarEmpresaOperadoraCommand(
        String identifierType,
        String identifierValue,
        String legalName,
        String tradeName) implements Command<EmpresaOperadoraRegistrada> {
}
