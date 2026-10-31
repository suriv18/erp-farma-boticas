package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductoReguladoTest {

    private static final ProductoReguladoId PRODUCTO_REGULADO_ID = new ProductoReguladoId(
            UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589"));
    private static final Instant CREATED_AT = Instant.parse("2026-09-07T10:00:00Z");

    @Test
    void createsAValidProductoReguladoWithoutActiveIngredients() {
        var result = ProductoRegulado.create(
                PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, "RS", "RS-12345",
                "Paracetamol 500mg", "500mg", "Caja x 10 tabletas", "TABLETA", "ORAL", "MG",
                "SIN-RECETA", null, null, null, null, null, null, "Laboratorio X", "Laboratorio X",
                null, null, null, null, null, null, CREATED_AT);

        assertTrue(result.isSuccess());
        var producto = result.getOrElse(error -> null);
        assertEquals("Paracetamol 500mg", producto.denominacion());
        assertEquals(EstadoRegulatorio.VIGENTE, producto.estado());
        assertTrue(producto.principiosActivos().isEmpty());
    }

    @Test
    void rejectsRegistroWithOnlyOneOfTipoAndNumero() {
        var result = ProductoRegulado.create(
                PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, "RS", null,
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_REGULADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsVigenciaHastaBeforeVigenciaDesde() {
        var result = ProductoRegulado.create(
                PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, null, null,
                "Paracetamol 500mg", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null,
                LocalDate.parse("2026-12-31"), LocalDate.parse("2026-01-01"), null, null, CREATED_AT);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRODUCTO_REGULADO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void addsAndRemovesAssociatedActiveIngredients() {
        var producto = ProductoRegulado.create(
                        PRODUCTO_REGULADO_ID, "MEDICAMENTO", null, null, null,
                        "Paracetamol 500mg", null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, CREATED_AT)
                .getOrElse(error -> null);

        var principioActivoId = new PrincipioActivoId(UUID.randomUUID());
        var asociado = new PrincipioActivoAsociado(principioActivoId, "500mg", new BigDecimal("500"), "MG", true, (short) 1);

        var withAsociado = producto.conPrincipioActivoAsociado(asociado);
        assertEquals(1, withAsociado.principiosActivos().size());

        var withoutAsociado = withAsociado.sinPrincipioActivoAsociado(principioActivoId);
        assertTrue(withoutAsociado.principiosActivos().isEmpty());
    }
}
