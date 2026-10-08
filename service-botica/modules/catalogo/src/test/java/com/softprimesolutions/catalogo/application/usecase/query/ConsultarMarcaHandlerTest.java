package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.EstadoMarca;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarMarcaHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID MARCA_ID = UUID.randomUUID();

    @Test
    void returnsMarcaResultWhenFound() {
        var marca = Marca.restore(
                new MarcaId(MARCA_ID), new TenantId(TENANT_ID), "GENFAR", "Genfar", "Marca generica",
                EstadoMarca.ACTIVO);
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.of(marca));
        var handler = new ConsultarMarcaHandler(port);

        var result = handler.execute(new ConsultarMarcaQuery(TENANT_ID, MARCA_ID));

        assertTrue(result.isSuccess());
        result.fold(
                success -> {
                    assertEquals(MARCA_ID, success.id());
                    assertEquals("GENFAR", success.codigo());
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.empty());
        var handler = new ConsultarMarcaHandler(port);

        var result = handler.execute(new ConsultarMarcaQuery(TENANT_ID, MARCA_ID));

        assertTrue(result.isFailure());
        result.fold(
                success -> null,
                failure -> {
                    assertEquals("CAT_MARCA_NO_ENCONTRADA", failure.code());
                    assertEquals(ErrorCategory.NOT_FOUND, failure.category());
                    return null;
                });
    }

    private record StubCatalogoComercialPort(Optional<Marca> marca) implements CatalogoComercialPort {

        @Override
        public SaveMarcaOutcome save(Marca marca) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Marca> findMarcaById(UUID tenantId, UUID marcaId) {
            return marca;
        }

        @Override
        public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean marcaExists(UUID tenantId, UUID marcaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
