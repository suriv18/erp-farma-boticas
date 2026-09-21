package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCategoriaProductoQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.EstadoCategoriaProducto;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarCategoriaProductoHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CATEGORIA_ID = UUID.randomUUID();

    @Test
    void returnsCategoriaResultWhenFound() {
        var categoria = CategoriaProducto.restore(
                new CategoriaProductoId(CATEGORIA_ID), new TenantId(TENANT_ID), null, "MEDICAMENTOS",
                "Medicamentos", "Categoria raiz", 1, 1, EstadoCategoriaProducto.ACTIVO);
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.of(categoria));
        var handler = new ConsultarCategoriaProductoHandler(port);

        var result = handler.execute(new ConsultarCategoriaProductoQuery(TENANT_ID, CATEGORIA_ID));

        assertTrue(result.isSuccess());
        result.fold(
                success -> {
                    assertEquals(CATEGORIA_ID, success.id());
                    assertEquals("MEDICAMENTOS", success.codigo());
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoComercialPort port = new StubCatalogoComercialPort(Optional.empty());
        var handler = new ConsultarCategoriaProductoHandler(port);

        var result = handler.execute(new ConsultarCategoriaProductoQuery(TENANT_ID, CATEGORIA_ID));

        assertTrue(result.isFailure());
        result.fold(
                success -> null,
                failure -> {
                    assertEquals("CAT_CATEGORIA_PRODUCTO_NO_ENCONTRADA", failure.code());
                    assertEquals(ErrorCategory.NOT_FOUND, failure.category());
                    return null;
                });
    }

    private record StubCatalogoComercialPort(Optional<CategoriaProducto> categoria) implements CatalogoComercialPort {

        @Override
        public SaveMarcaOutcome save(com.softprimesolutions.catalogo.domain.model.Marca marca) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveSkuOutcome save(com.softprimesolutions.catalogo.domain.model.SKUComercial sku) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<com.softprimesolutions.catalogo.domain.model.SKUComercial> findSkuById(
                UUID tenantId, UUID skuId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CategoriaProducto> findCategoriaById(UUID tenantId, UUID categoriaId) {
            return categoria;
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
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, java.time.Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCategoriaStatus(
                UUID tenantId, UUID categoriaId, String status, java.time.Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, java.time.Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
