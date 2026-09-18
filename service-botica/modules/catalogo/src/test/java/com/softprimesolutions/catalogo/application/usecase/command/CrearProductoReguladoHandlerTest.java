package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearProductoReguladoCommand;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearProductoReguladoHandlerTest {

    @Test
    void createsAProductoReguladoSuccessfully() {
        var writePort = new FakeProductoReguladoPort();
        var handler = new CrearProductoReguladoHandler(
                writePort, () -> UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"),
                () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearProductoReguladoCommand(
                "MEDICAMENTO", null, null, null, "Paracetamol 500mg", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertTrue(result.isSuccess());
        assertEquals("Paracetamol 500mg", result.getOrElse(error -> null).denominacion());
    }

    @Test
    void failsWithNotFoundWhenFormaFarmaceuticaDoesNotExist() {
        var writePort = new FakeProductoReguladoPort();
        writePort.outcome = ProductoReguladoPort.SaveOutcome.FORMA_FARMACEUTICA_NOT_FOUND;
        var handler = new CrearProductoReguladoHandler(
                writePort, () -> UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"),
                () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearProductoReguladoCommand(
                "MEDICAMENTO", null, null, null, "Paracetamol 500mg", null, null, "TABLETA", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertTrue(result.isFailure());
        assertEquals("CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeProductoReguladoPort implements ProductoReguladoPort {
        private SaveOutcome outcome = SaveOutcome.CREATED;

        @Override
        public SaveOutcome save(ProductoRegulado productoRegulado) { return outcome; }

        @Override
        public Optional<ProductoRegulado> findById(UUID productoReguladoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }
    }
}
