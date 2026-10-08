package com.softprimesolutions.catalogo.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrincipioActivoTest {

    @Test
    void createsAndNormalizesAValidPrincipioActivo() {
        var result = PrincipioActivo.create(
                new PrincipioActivoId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                "  PA-001  ", "  Paracetamol  ", "  paracetamol  ", "DIGEMID");

        assertTrue(result.isSuccess());
        var principio = result.getOrElse(error -> null);
        assertEquals("Paracetamol", principio.denominacion());
        assertEquals(EstadoPrincipioActivo.ACTIVO, principio.estado());
    }

    @Test
    void rejectsAMissingId() {
        var result = PrincipioActivo.create(null, null, "Paracetamol", null, null);
        assertTrue(result.isFailure());
    }

    @Test
    void rejectsADenominationThatIsTooShort() {
        var result = PrincipioActivo.create(
                new PrincipioActivoId(UUID.randomUUID()), null, "P", null, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_PRINCIPIO_ACTIVO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }
}
