package com.softprimesolutions.organizacion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.organizacion.api.RegistrarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.port.EmpresaOperadoraRepository;
import com.softprimesolutions.organizacion.domain.empresa.EmpresaOperadora;
import com.softprimesolutions.shared.application.error.ApplicationError;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarEmpresaOperadoraHandlerTest {

    private static final UUID COMPANY_ID = UUID.fromString("8a9773f5-8b95-4d2a-956f-252f433634f4");
    private static final Instant REGISTERED_AT = Instant.parse("2026-08-31T20:00:00Z");

    @Test
    void registersAndNormalizesAValidCompany() {
        var repository = new CapturingRepository(EmpresaOperadoraRepository.SaveOutcome.CREATED);
        var handler = handler(repository);

        var result = handler.handle(new RegistrarEmpresaOperadoraCommand(
                " ruc ", " 20123456789 ", " Boticas del Pacífico S.A.C. ", " Boticas Pacífico "));

        assertTrue(result.isSuccess());
        var registered = result.getOrElse(error -> null);
        assertEquals(COMPANY_ID, registered.id());
        assertEquals("RUC", registered.identifierType());
        assertEquals("20123456789", registered.identifierValue());
        assertEquals("Boticas del Pacífico S.A.C.", registered.legalName());
        assertEquals("ACTIVE", registered.status());
        assertEquals(REGISTERED_AT, registered.registeredAt());
        assertEquals(1, repository.savedCompanies.size());
    }

    @Test
    void rejectsInvalidInputBeforePersistence() {
        var repository = new CapturingRepository(EmpresaOperadoraRepository.SaveOutcome.CREATED);
        var handler = handler(repository);

        var result = handler.handle(new RegistrarEmpresaOperadoraCommand(
                "R", "", " ", null));

        assertTrue(result.isFailure());
        var error = result.fold(value -> null, applicationError -> applicationError);
        assertEquals("ORG_EMPRESA_INVALIDA", error.code());
        assertTrue(repository.savedCompanies.isEmpty());
    }

    @Test
    void reportsAnAtomicIdentifierConflict() {
        var repository = new CapturingRepository(
                EmpresaOperadoraRepository.SaveOutcome.DUPLICATE_IDENTIFIER);
        var handler = handler(repository);

        var result = handler.handle(new RegistrarEmpresaOperadoraCommand(
                "RUC", "20123456789", "Boticas del Pacífico S.A.C.", null));

        assertTrue(result.isFailure());
        ApplicationError error = result.fold(value -> null, applicationError -> applicationError);
        assertEquals("ORG_EMPRESA_DUPLICADA", error.code());
        assertFalse(repository.savedCompanies.isEmpty());
    }

    private static RegistrarEmpresaOperadoraHandler handler(EmpresaOperadoraRepository repository) {
        return new RegistrarEmpresaOperadoraHandler(
                repository,
                () -> COMPANY_ID,
                () -> REGISTERED_AT);
    }

    private static final class CapturingRepository implements EmpresaOperadoraRepository {

        private final SaveOutcome outcome;
        private final List<EmpresaOperadora> savedCompanies = new ArrayList<>();

        private CapturingRepository(SaveOutcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public SaveOutcome save(EmpresaOperadora company) {
            savedCompanies.add(company);
            return outcome;
        }
    }
}
