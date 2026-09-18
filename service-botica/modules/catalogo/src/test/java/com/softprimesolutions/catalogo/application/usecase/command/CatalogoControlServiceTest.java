package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import com.softprimesolutions.catalogo.domain.model.SKUComercial;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogoControlServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void rejectsAnInvalidStatus() {
        var soportePort = new FakeCatalogoSoportePort(true);
        var service = new CatalogoControlService(
                soportePort, new FakeCatalogoComercialPort(true), new FakeProductoReguladoPort(true),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCondicionVentaStatus("SIN-RECETA", "ELIMINADO");

        assertTrue(result.isFailure());
        assertEquals("CAT_ESTADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesCondicionVentaStatusWithAValidValue() {
        var soportePort = new FakeCatalogoSoportePort(true);
        var service = new CatalogoControlService(
                soportePort, new FakeCatalogoComercialPort(true), new FakeProductoReguladoPort(true),
                () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeCondicionVentaStatus("SIN-RECETA", "INACTIVO");

        assertTrue(result.isSuccess());
    }

    @Test
    void reportsNotFoundWhenMarcaDoesNotExist() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(false),
                new FakeProductoReguladoPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeMarcaStatus(TENANT_ID, UUID.randomUUID(), "ACTIVO");

        assertTrue(result.isFailure());
        assertEquals("CAT_MARCA_NO_ENCONTRADA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void changesProductoReguladoStatusSuccessfully() {
        var service = new CatalogoControlService(
                new FakeCatalogoSoportePort(true), new FakeCatalogoComercialPort(true),
                new FakeProductoReguladoPort(true), () -> Instant.parse("2026-09-07T12:00:00Z"));

        var result = service.changeProductoReguladoStatus(UUID.randomUUID(), "VIGENTE");

        assertTrue(result.isSuccess());
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
        private final boolean found;

        private FakeCatalogoSoportePort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) { throw new UnsupportedOperationException(); }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean condicionVentaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean viaAdministracionExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean unidadMedidaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean clasificacionControladaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) { return found; }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeCatalogoComercialPort implements CatalogoComercialPort {
        private final boolean found;

        private FakeCatalogoComercialPort(boolean found) { this.found = found; }

        @Override
        public SaveMarcaOutcome save(Marca marca) { throw new UnsupportedOperationException(); }

        @Override
        public SaveCategoriaOutcome save(CategoriaProducto categoria) { throw new UnsupportedOperationException(); }

        @Override
        public SaveSkuOutcome save(SKUComercial sku) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<SKUComercial> findSkuById(UUID tenantId, UUID skuId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean categoriaExists(UUID tenantId, UUID categoriaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean marcaExists(UUID tenantId, UUID marcaId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeMarcaStatus(UUID tenantId, UUID marcaId, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeCategoriaStatus(UUID tenantId, UUID categoriaId, String status, Instant changedAt) { return found; }

        @Override
        public boolean changeSkuStatus(UUID tenantId, UUID skuId, String status, Instant changedAt) { return found; }
    }

    private static final class FakeProductoReguladoPort implements ProductoReguladoPort {
        private final boolean found;

        private FakeProductoReguladoPort(boolean found) { this.found = found; }

        @Override
        public SaveOutcome save(ProductoRegulado productoRegulado) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ProductoRegulado> findById(UUID productoReguladoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt) { return found; }
    }
}
