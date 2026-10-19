package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClasificacionControladaTest {

    @Test
    void createsAndNormalizesAValidClasificacionControlada() {
        var result = ClasificacionControlada.create(
                "  lista-i  ", "  Lista I - Estupefacientes  ", "DL-22095", true, true, 30);

        assertTrue(result.isSuccess());
        var clasificacion = result.getOrElse(error -> null);
        assertEquals("LISTA-I", clasificacion.codigo());
        assertEquals(30, clasificacion.vigenciaRecetaDias());
    }

    @Test
    void rejectsANonPositiveVigenciaRecetaDias() {
        var result = ClasificacionControlada.create("LISTA-I", "Lista I", null, true, true, 0);

        assertTrue(result.isFailure());
        assertEquals("CAT_CLASIFICACION_CONTROLADA_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void allowsANullVigenciaRecetaDias() {
        var result = ClasificacionControlada.create("LISTA-I", "Lista I", null, true, true, null);

        assertTrue(result.isSuccess());
    }
}
