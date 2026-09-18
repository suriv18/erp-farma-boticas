package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CondicionVentaTest {

    @Test
    void createsAndNormalizesAValidCondicionVenta() {
        var result = CondicionVenta.create(
                "  sin-receta  ", "  Sin receta médica  ", false, false,
                "DIGEMID", "1.0", null, null);

        assertTrue(result.isSuccess());
        var condicion = result.getOrElse(error -> null);
        assertEquals("SIN-RECETA", condicion.codigo());
        assertEquals("Sin receta médica", condicion.denominacion());
        assertEquals(EstadoCatalogoSoporte.ACTIVO, condicion.estado());
    }

    @Test
    void rejectsAMissingCodigo() {
        var result = CondicionVenta.create(null, "Sin receta médica", false, false, null, null, null, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_CONDICION_VENTA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsADenominationThatIsTooShort() {
        var result = CondicionVenta.create("SIN-RECETA", "S", false, false, null, null, null, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_CONDICION_VENTA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
