package com.softprimesolutions.catalogo.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    @Test
    void wrapsAndExposesTheUnderlyingUuid() {
        var uuid = UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589");

        assertEquals(uuid, new TenantId(uuid).value());
        assertEquals(uuid, new PrincipioActivoId(uuid).value());
        assertEquals(uuid, new MarcaId(uuid).value());
        assertEquals(uuid, new CategoriaProductoId(uuid).value());
        assertEquals(uuid, new ProductoReguladoId(uuid).value());
        assertEquals(uuid, new SkuId(uuid).value());
    }

    @Test
    void rejectsANullUuid() {
        assertThrows(NullPointerException.class, () -> new TenantId(null));
        assertThrows(NullPointerException.class, () -> new PrincipioActivoId(null));
        assertThrows(NullPointerException.class, () -> new MarcaId(null));
        assertThrows(NullPointerException.class, () -> new CategoriaProductoId(null));
        assertThrows(NullPointerException.class, () -> new ProductoReguladoId(null));
        assertThrows(NullPointerException.class, () -> new SkuId(null));
    }
}
