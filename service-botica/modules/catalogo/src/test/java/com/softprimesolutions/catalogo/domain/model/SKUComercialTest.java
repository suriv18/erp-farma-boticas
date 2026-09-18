package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SKUComercialTest {

    private static final SkuId SKU_ID = new SkuId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"));
    private static final TenantId TENANT_ID = new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5"));
    private static final Instant CREATED_AT = Instant.parse("2026-09-07T10:00:00Z");

    @Test
    void createsANoRegulatedSkuWithoutProductoRegulado() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-001",
                "Alcohol en gel 250ml", null, null, null, null, null, null, null, null, null,
                false, null, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT);

        assertTrue(result.isSuccess());
        var sku = result.getOrElse(error -> null);
        assertEquals("Alcohol en gel 250ml", sku.descripcionComercial());
        assertEquals(EstadoComercialSku.ACTIVO, sku.estado());
    }

    @Test
    void rejectsARegulatedSkuWithoutProductoRegulado() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.REGULADO, "SKU-002",
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null,
                false, null, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void acceptsARegulatedSkuWithProductoRegulado() {
        var productoReguladoId = new ProductoReguladoId(UUID.randomUUID());
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, productoReguladoId, null, null, TipoSku.REGULADO, "SKU-003",
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null,
                false, null, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT);

        assertTrue(result.isSuccess());
    }

    @Test
    void rejectsAFactorFraccionWhenVentaFraccionIsDisabled() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-004",
                "Producto fraccionable", null, null, null, null, null, null, null, null, null,
                false, new BigDecimal("0.5"), null, true, true, true, BigDecimal.ZERO, null, null, "test",
                CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsStockMaximoBelowStockMinimo() {
        var result = SKUComercial.create(
                SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-005",
                "Producto con stock", null, null, null, null, null, null, null, null, null,
                false, null, null, true, true, true, new BigDecimal("10"), new BigDecimal("5"), null, "test",
                CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_SKU_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void managesBarcodesAsAnImmutableCollection() {
        var sku = SKUComercial.create(
                        SKU_ID, TENANT_ID, null, null, null, TipoSku.NO_REGULADO, "SKU-006",
                        "Alcohol en gel", null, null, null, null, null, null, null, null, null,
                        false, null, null, true, true, true, BigDecimal.ZERO, null, null, "test", CREATED_AT)
                .getOrElse(error -> null);

        var codigoUno = new CodigoBarraSku("7501234567890", "EAN13", true, null, null, EstadoCatalogoSoporte.ACTIVO);
        var codigoDos = new CodigoBarraSku("7501234567891", "EAN13", false, null, null, EstadoCatalogoSoporte.ACTIVO);

        var conCodigos = sku.conCodigoBarra(codigoUno).conCodigoBarra(codigoDos);
        assertEquals(2, conCodigos.codigosBarra().size());

        var conPrincipalCambiado = conCodigos.conCodigoBarraPrincipal("7501234567891");
        var principal = conPrincipalCambiado.codigosBarra().stream()
                .filter(CodigoBarraSku::esPrincipal).findFirst().orElseThrow();
        assertEquals("7501234567891", principal.codigoBarra());

        var sinCodigoUno = conPrincipalCambiado.sinCodigoBarra("7501234567890");
        assertEquals(1, sinCodigoUno.codigosBarra().size());
    }
}
