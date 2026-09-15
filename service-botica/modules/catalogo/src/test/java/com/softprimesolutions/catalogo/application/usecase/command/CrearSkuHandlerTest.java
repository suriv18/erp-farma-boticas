package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearSkuCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearSkuHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void createsANoRegulatedSkuSuccessfully() {
        var writePort = new FakeCatalogoComercialPort();
        var handler = new CrearSkuHandler(
                writePort, () -> UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"),
                () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearSkuCommand(
                TENANT_ID, null, null, null, "NO_REGULADO", "SKU-001", "Alcohol en gel", null, null,
                null, null, null, null, null, null, null, false, null, null, true, true, true,
                BigDecimal.ZERO, null, null, "test"));

        assertTrue(result.isSuccess());
        assertEquals("Alcohol en gel", result.getOrElse(error -> null).descripcionComercial());
    }

    @Test
    void failsWithConflictWhenCodigoInternoAlreadyExists() {
        var writePort = new FakeCatalogoComercialPort();
        writePort.skuOutcome = CatalogoComercialPort.SaveSkuOutcome.DUPLICATE_CODIGO_INTERNO;
        var handler = new CrearSkuHandler(
                writePort, () -> UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"),
                () -> Instant.parse("2026-09-07T10:00:00Z"));

        var result = handler.execute(new CrearSkuCommand(
                TENANT_ID, null, null, null, "NO_REGULADO", "SKU-001", "Alcohol en gel", null, null,
                null, null, null, null, null, null, null, false, null, null, true, true, true,
                BigDecimal.ZERO, null, null, "test"));

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_CODIGO_INTERNO_DUPLICADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoComercialPort implements CatalogoComercialPort {
        private SaveSkuOutcome skuOutcome = SaveSkuOutcome.CREATED;

        @Override
        public SaveMarcaOutcome save(Marca marca) { throw new UnsupportedOperationException(); }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) { throw new UnsupportedOperationException(); }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) { return skuOutcome; }

        @Override
        public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean marcaExists(UUID tenantId, UUID marcaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }
    }
}
