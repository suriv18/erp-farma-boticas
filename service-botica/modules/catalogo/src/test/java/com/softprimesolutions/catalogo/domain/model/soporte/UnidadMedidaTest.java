package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UnidadMedidaTest {

    @Test
    void createsAndNormalizesAValidUnidadMedida() {
        var result = UnidadMedida.create("  mg  ", "  Miligramo  ", "mg", false, "DIGEMID");

        assertTrue(result.isSuccess());
        var unidad = result.getOrElse(error -> null);
        assertEquals("MG", unidad.codigo());
        assertEquals("Miligramo", unidad.denominacion());
        assertEquals("mg", unidad.simbolo());
        assertTrue(!unidad.permiteDecimal());
    }

    @Test
    void rejectsAMissingDenominacion() {
        var result = UnidadMedida.create("MG", null, null, false, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_UNIDAD_MEDIDA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
