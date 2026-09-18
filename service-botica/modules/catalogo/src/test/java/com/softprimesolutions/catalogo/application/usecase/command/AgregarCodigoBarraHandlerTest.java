package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.AgregarCodigoBarraCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.model.TipoSku;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgregarCodigoBarraHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");
    private static final UUID SKU_ID = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");

    @Test
    void addsABarcodeSuccessfully() {
        var existing = SKUComercial.create(
                        new SkuId(SKU_ID), new TenantId(TENANT_ID), null, null, null, TipoSku.NO_REGULADO,
                        "SKU-001", "Alcohol en gel", null, null, null, null, null, null, null, null, null,
                        false, null, null, true, true, true, BigDecimal.ZERO, null, null, "test",
                        Instant.parse("2026-09-07T10:00:00Z"))
                .getOrElse(error -> null);
        var writePort = new FakeCatalogoComercialPort(existing);
        var handler = new AgregarCodigoBarraHandler(writePort);

        var result = handler.execute(new AgregarCodigoBarraCommand(
                TENANT_ID, SKU_ID, "7501234567890", "EAN13", null, null));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrElse(error -> null).codigosBarra().size());
    }

    @Test
    void failsWithNotFoundWhenSkuDoesNotExist() {
        var writePort = new FakeCatalogoComercialPort(null);
        var handler = new AgregarCodigoBarraHandler(writePort);

        var result = handler.execute(new AgregarCodigoBarraCommand(
                TENANT_ID, SKU_ID, "7501234567890", "EAN13", null, null));

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoComercialPort implements CatalogoComercialPort {
        private final SKUComercial existing;

        private FakeCatalogoComercialPort(SKUComercial existing) {
            this.existing = existing;
        }

        @Override
        public SaveMarcaOutcome save(Marca marca) { throw new UnsupportedOperationException(); }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) { throw new UnsupportedOperationException(); }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) { return SaveSkuOutcome.UPDATED; }

        @Override
        public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) { return Optional.ofNullable(existing); }

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
