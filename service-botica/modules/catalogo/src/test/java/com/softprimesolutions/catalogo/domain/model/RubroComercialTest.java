package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RubroComercialTest {

    @Test
    void createsRubroComercialWithValidData() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var result = RubroComercial.create(id, tenantId, "FARMA", "Farmacéutico", "Línea farmacéutica", true, 1);

        assertTrue(result.isSuccess());
        result.fold(
                rubro -> {
                    assertEquals("FARMA", rubro.codigo());
                    assertEquals("Farmacéutico", rubro.nombre());
                    assertTrue(rubro.esFarmaceutico());
                    assertEquals(1, rubro.orden());
                    assertEquals(EstadoRubroComercial.ACTIVO, rubro.estado());
                    return null;
                },
                error -> {
                    throw new AssertionError("expected success but got " + error);
                });
    }

    @Test
    void rejectsCodigoTooShort() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var result = RubroComercial.create(id, tenantId, "F", "Farmacéutico", null, true, 1);

        assertTrue(result.isFailure());
        result.fold(
                rubro -> {
                    throw new AssertionError("expected failure but got " + rubro);
                },
                error -> {
                    assertEquals("CAT_RUBRO_COMERCIAL_INVALIDO", error.code());
                    assertEquals("codigo", error.metadata().get("field"));
                    return null;
                });
    }

    @Test
    void rejectsNegativeOrden() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var result = RubroComercial.create(id, tenantId, "FARMA", "Farmacéutico", null, true, -1);

        assertTrue(result.isFailure());
        result.fold(
                rubro -> {
                    throw new AssertionError("expected failure but got " + rubro);
                },
                error -> {
                    assertEquals("CAT_RUBRO_COMERCIAL_INVALIDO", error.code());
                    assertEquals("orden", error.metadata().get("field"));
                    return null;
                });
    }

    @Test
    void restoresRubroComercialWithGivenState() {
        var id = new RubroComercialId(UUID.randomUUID());
        var tenantId = new TenantId(UUID.randomUUID());

        var rubro = RubroComercial.restore(
                id, tenantId, "BEBE", "Bebé", "Línea infantil", false, 2, EstadoRubroComercial.INACTIVO);

        assertEquals("BEBE", rubro.codigo());
        assertFalse(rubro.esFarmaceutico());
        assertEquals(EstadoRubroComercial.INACTIVO, rubro.estado());
    }
}
