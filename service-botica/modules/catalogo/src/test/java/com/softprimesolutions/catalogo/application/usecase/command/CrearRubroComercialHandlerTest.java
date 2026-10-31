package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearRubroComercialCommand;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearRubroComercialHandlerTest {

    @Test
    void createsRubroComercialWhenValid() {
        var tenantId = UUID.randomUUID();
        var port = new StubRubroComercialPort(RubroComercialPort.SaveOutcome.CREATED);
        var handler = new CrearRubroComercialHandler(port, UUID::randomUUID);

        var result = handler.execute(
                new CrearRubroComercialCommand(tenantId, "FARMA", "Farmacéutico", "Línea farmacéutica", true, 1));

        assertTrue(result.isSuccess());
        assertEquals("FARMA", result.getOrElse(error -> null).codigo());
        assertTrue(result.getOrElse(error -> null).esFarmaceutico());
        assertEquals("ACTIVO", result.getOrElse(error -> null).estado());
    }

    @Test
    void rejectsDuplicateCodigo() {
        var port = new StubRubroComercialPort(RubroComercialPort.SaveOutcome.DUPLICATE_CODIGO);
        var handler = new CrearRubroComercialHandler(port, UUID::randomUUID);

        var result = handler.execute(
                new CrearRubroComercialCommand(UUID.randomUUID(), "FARMA", "Farmacéutico", null, true, 1));

        assertTrue(result.isFailure());
        assertEquals("CAT_RUBRO_COMERCIAL_DUPLICADO", result.fold(success -> null, error -> error.code()));
        assertEquals(ErrorCategory.CONFLICT, result.fold(success -> null, error -> error.category()));
    }

    @Test
    void rejectsUnknownTenant() {
        var port = new StubRubroComercialPort(RubroComercialPort.SaveOutcome.TENANT_NOT_FOUND);
        var handler = new CrearRubroComercialHandler(port, UUID::randomUUID);

        var result = handler.execute(
                new CrearRubroComercialCommand(UUID.randomUUID(), "FARMA", "Farmacéutico", null, true, 1));

        assertTrue(result.isFailure());
        assertEquals("CAT_TENANT_NO_ENCONTRADO", result.fold(success -> null, error -> error.code()));
        assertEquals(ErrorCategory.NOT_FOUND, result.fold(success -> null, error -> error.category()));
    }

    private record StubRubroComercialPort(RubroComercialPort.SaveOutcome outcome) implements RubroComercialPort {

        @Override
        public SaveOutcome save(RubroComercial rubroComercial) {
            return outcome;
        }

        @Override
        public Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
