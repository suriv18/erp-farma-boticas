package com.softprimesolutions.organizacion.application.port;

import com.softprimesolutions.organizacion.domain.empresa.EmpresaOperadora;

/** Puerto cuya implementación debe garantizar unicidad de forma atómica. */
@FunctionalInterface
public interface EmpresaOperadoraRepository {

    SaveOutcome save(EmpresaOperadora company);

    enum SaveOutcome {
        CREATED,
        DUPLICATE_IDENTIFIER
    }
}
