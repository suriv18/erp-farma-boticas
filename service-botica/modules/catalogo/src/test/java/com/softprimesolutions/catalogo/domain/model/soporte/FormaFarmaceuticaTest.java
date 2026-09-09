package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FormaFarmaceuticaTest {

    @Test
    void createsAndNormalizesAValidFormaFarmaceutica() {
        var result = FormaFarmaceutica.create("  tableta  ", "  Tableta  ", "DIGEMID");

        assertTrue(result.isSuccess());
        var forma = result.getOrElse(error -> null);
        assertEquals("TABLETA", forma.codigo());
        assertEquals("Tableta", forma.denominacion());
        assertEquals(EstadoCatalogoSoporte.ACTIVO, forma.estado());
    }

    @Test
    void rejectsAMissingDenominacion() {
        var result = FormaFarmaceutica.create("TABLETA", " ", null);

        assertTrue(result.isFailure());
        assertEquals("CAT_FORMA_FARMACEUTICA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
