package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CategoriaProductoTest {

    @Test
    void createsARootCategoriaSuccessfully() {
        var result = CategoriaProducto.create(
                new CategoriaProductoId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                null, "  analgesicos  ", "  Analgésicos  ", null, 1, 0);

        assertTrue(result.isSuccess());
        var categoria = result.getOrElse(error -> null);
        assertEquals("Analgésicos", categoria.nombre());
        assertEquals(1, categoria.nivel());
    }

    @Test
    void createsAChildCategoriaWithAParent() {
        var padreId = new CategoriaProductoId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        var result = CategoriaProducto.create(
                new CategoriaProductoId(UUID.randomUUID()),
                new TenantId(UUID.randomUUID()),
                padreId, "antiinflamatorios", "Antiinflamatorios", null, 2, 1);

        assertTrue(result.isSuccess());
        assertEquals(padreId, result.getOrElse(error -> null).categoriaPadreId());
    }

    @Test
    void rejectsANivelBelowOne() {
        var result = CategoriaProducto.create(
                new CategoriaProductoId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                null, "analgesicos", "Analgésicos", null, 0, 0);

        assertTrue(result.isFailure());
        assertEquals("CAT_CATEGORIA_PRODUCTO_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
