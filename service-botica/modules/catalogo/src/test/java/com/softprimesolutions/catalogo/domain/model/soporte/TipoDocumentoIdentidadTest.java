package com.softprimesolutions.catalogo.domain.model.soporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TipoDocumentoIdentidadTest {

    @Test
    void createsAndNormalizesAValidTipoDocumentoIdentidad() {
        var result = TipoDocumentoIdentidad.create("  1  ", "  DNI  ", "  Documento Nacional de Identidad  ", 8, 8);

        assertTrue(result.isSuccess());
        var tipo = result.getOrElse(error -> null);
        assertEquals("1", tipo.codigo());
        assertEquals("DNI", tipo.sigla());
        assertEquals("Documento Nacional de Identidad", tipo.denominacion());
        assertEquals(8, tipo.max());
        assertEquals(8, tipo.min());
        assertEquals(EstadoCatalogoSoporte.ACTIVO, tipo.estado());
    }

    @Test
    void normalizesCodigoToUpperCase() {
        var result = TipoDocumentoIdentidad.create("a", "CE", "Carnet de extranjería", null, null);

        assertTrue(result.isSuccess());
        assertEquals("A", result.getOrElse(error -> null).codigo());
    }

    @Test
    void acceptsNullMaxAndMin() {
        var result = TipoDocumentoIdentidad.create("7", "PAS", "Pasaporte", null, null);

        assertTrue(result.isSuccess());
        var tipo = result.getOrElse(error -> null);
        assertEquals(null, tipo.max());
        assertEquals(null, tipo.min());
    }

    @Test
    void rejectsAMissingCodigo() {
        var result = TipoDocumentoIdentidad.create(null, "DNI", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsACodigoLongerThanOneCharacter() {
        var result = TipoDocumentoIdentidad.create("AB", "DNI", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsACodigoWithLowercaseLetterAfterTrimOnly() {
        var result = TipoDocumentoIdentidad.create("-", "DNI", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsAMissingSigla() {
        var result = TipoDocumentoIdentidad.create("1", "  ", "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsASiglaLongerThan30Characters() {
        var result = TipoDocumentoIdentidad.create("1", "A".repeat(31), "Documento Nacional de Identidad", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsADenominationThatIsTooShort() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "D", 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsADenominationLongerThan200Characters() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "D".repeat(201), 8, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsAZeroMax() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "Documento Nacional de Identidad", 0, null);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsANegativeMin() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "Documento Nacional de Identidad", null, -1);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsAMinGreaterThanMax() {
        var result = TipoDocumentoIdentidad.create("1", "DNI", "Documento Nacional de Identidad", 5, 8);

        assertTrue(result.isFailure());
        assertEquals("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void restoreDoesNotValidate() {
        var tipo = TipoDocumentoIdentidad.restore("0", "DTSR", "DOC.TRIB.NO.DOM.SIN.RUC", null, null,
                EstadoCatalogoSoporte.INACTIVO);

        assertEquals("0", tipo.codigo());
        assertEquals(EstadoCatalogoSoporte.INACTIVO, tipo.estado());
    }
}
