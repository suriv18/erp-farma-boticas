package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarcaTest {

    @Test
    void createsAndNormalizesAValidMarca() {
        var result = Marca.create(
                new MarcaId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                "  bayer  ", "  Bayer  ", null);

        assertTrue(result.isSuccess());
        var marca = result.getOrElse(error -> null);
        assertEquals("Bayer", marca.nombre());
        assertEquals(EstadoMarca.ACTIVO, marca.estado());
    }

    @Test
    void requiresIdTenantAndCodigo() {
        var missingTenant = Marca.create(
                new MarcaId(UUID.randomUUID()), null, "BAYER", "Bayer", null);
        assertTrue(missingTenant.isFailure());
        assertEquals("CAT_MARCA_INVALIDA", missingTenant.fold(value -> null, error -> error.code()));
    }
}
