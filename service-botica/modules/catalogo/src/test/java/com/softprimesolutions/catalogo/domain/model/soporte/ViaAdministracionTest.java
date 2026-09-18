package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ViaAdministracionTest {

    @Test
    void createsAndNormalizesAValidViaAdministracion() {
        var result = ViaAdministracion.create("  oral  ", "  Vía oral  ", "DIGEMID");

        assertTrue(result.isSuccess());
        var via = result.getOrElse(error -> null);
        assertEquals("ORAL", via.codigo());
        assertEquals("Vía oral", via.denominacion());
    }

    @Test
    void rejectsAMissingCodigo() {
        var result = ViaAdministracion.create(null, "Vía oral", null);

        assertTrue(result.isFailure());
        assertEquals("CAT_VIA_ADMINISTRACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }
}
