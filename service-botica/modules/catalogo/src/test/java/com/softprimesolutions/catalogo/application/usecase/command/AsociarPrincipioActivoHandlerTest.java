package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.AsociarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AsociarPrincipioActivoHandlerTest {

    private static final UUID PRODUCTO_REGULADO_ID = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");

    @Test
    void associatesAnActiveIngredientSuccessfully() {
        var existing = ProductoRegulado.create(
                        new com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId(PRODUCTO_REGULADO_ID),
                        "MEDICAMENTO", null, null, null, "Paracetamol 500mg", null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                        Instant.parse("2026-09-07T10:00:00Z"))
                .getOrElse(error -> null);
        var writePort = new FakeProductoReguladoPort(existing);
        var handler = new AsociarPrincipioActivoHandler(writePort);

        var result = handler.execute(new AsociarPrincipioActivoCommand(
                PRODUCTO_REGULADO_ID, UUID.randomUUID(), "500mg", new BigDecimal("500"), "MG", true, (short) 1));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrElse(error -> null).principiosActivos().size());
    }

    @Test
    void failsWithNotFoundWhenProductoReguladoDoesNotExist() {
        var writePort = new FakeProductoReguladoPort(null);
        var handler = new AsociarPrincipioActivoHandler(writePort);

        var result = handler.execute(new AsociarPrincipioActivoCommand(
                PRODUCTO_REGULADO_ID, UUID.randomUUID(), null, null, null, true, (short) 1));

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_REGULADO_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeProductoReguladoPort implements ProductoReguladoPort {
        private final ProductoRegulado existing;

        private FakeProductoReguladoPort(ProductoRegulado existing) {
            this.existing = existing;
        }

        @Override
        public SaveOutcome save(ProductoRegulado productoRegulado) { return SaveOutcome.UPDATED; }

        @Override
        public Optional<ProductoRegulado> findById(UUID productoReguladoId) { return Optional.ofNullable(existing); }

        @Override
        public boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }
    }
}
