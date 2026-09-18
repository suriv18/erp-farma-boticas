package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearCategoriaProductoCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearCategoriaProductoHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void createsARootCategoriaSuccessfully() {
        var writePort = new FakeCatalogoComercialPort();
        var handler = new CrearCategoriaProductoHandler(writePort, () -> UUID.fromString(
                "98a1587e-27ef-4077-befd-6f5af4901589"));

        var result = handler.execute(new CrearCategoriaProductoCommand(
                TENANT_ID, null, "ANALGESICOS", "Analgésicos", null, 1, 0));

        assertTrue(result.isSuccess());
        assertEquals("Analgésicos", result.getOrElse(error -> null).nombre());
    }

    @Test
    void failsWithNotFoundWhenCategoriaPadreDoesNotExist() {
        var writePort = new FakeCatalogoComercialPort();
        writePort.categoriaOutcome = CatalogoComercialPort.SaveCategoriaOutcome.CATEGORIA_PADRE_NOT_FOUND;
        var handler = new CrearCategoriaProductoHandler(writePort, () -> UUID.fromString(
                "98a1587e-27ef-4077-befd-6f5af4901589"));

        var result = handler.execute(new CrearCategoriaProductoCommand(
                TENANT_ID, UUID.randomUUID(), "ANTIINFLAMATORIOS", "Antiinflamatorios", null, 2, 0));

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_PADRE_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoComercialPort implements CatalogoComercialPort {
        private SaveCategoriaOutcome categoriaOutcome = SaveCategoriaOutcome.CREATED;

        @Override
        public SaveMarcaOutcome save(Marca marca) { throw new UnsupportedOperationException(); }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) { return categoriaOutcome; }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) { throw new UnsupportedOperationException(); }

        @Override
        public java.util.Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) { throw new UnsupportedOperationException(); }

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
