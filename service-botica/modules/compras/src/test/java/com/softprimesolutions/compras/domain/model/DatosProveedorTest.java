package com.softprimesolutions.compras.domain.model;

import static com.softprimesolutions.compras.ComprasFixtures.domainError;
import static com.softprimesolutions.compras.ComprasFixtures.domainValue;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import org.junit.jupiter.api.Test;

class DatosProveedorTest {

    private static Result<DatosProveedor, ErrorDetail> crear(
            String tipo, String numero, String razon, String nombreComercial, String ubigeo, Integer dias,
            String moneda) {
        return DatosProveedor.crear(
                tipo, numero, razon, nombreComercial, null, ubigeo, null, null, null, null, null, null, dias,
                moneda, null, null, null, null);
    }

    private static void assertInvalid(Result<DatosProveedor, ErrorDetail> result) {
        assertThat(domainError(result).code()).isEqualTo(ComprasErrorCodes.PROVEEDOR_INVALIDO);
    }

    @Test
    void appliesTheDdlDefaultsToTheOmittedFields() {
        var datos = domainValue(crear(null, " 20100070970 ", " Laboratorios SAC ", null, null, null, null));

        assertThat(datos.tipoDocumento()).isEqualTo("6");
        assertThat(datos.numeroDocumento()).isEqualTo("20100070970");
        assertThat(datos.razonSocial()).isEqualTo("Laboratorios SAC");
        assertThat(datos.condicionPagoDefault()).isEqualTo("CONTADO");
        assertThat(datos.diasCreditoDefault()).isZero();
        assertThat(datos.monedaDefault()).isEqualTo("PEN");
        assertThat(datos.esLaboratorio()).isFalse();
        assertThat(datos.esImportador()).isFalse();
        assertThat(datos.esDistribuidor()).isTrue();
        assertThat(datos.calificacion()).isEqualTo("CONFIABLE");
    }

    @Test
    void keepsTheValuesGivenByTheCaller() {
        var datos = domainValue(DatosProveedor.crear(
                "1", "12345678", "Persona Natural", "Comercial", "Av. Lima 123", "150101", "999888777",
                "ventas@proveedor.example", "Ana", "999000111", "ana@proveedor.example", "CREDITO 30", 30, "USD",
                true, true, false, "EXCELENTE"));

        assertThat(datos.tipoDocumento()).isEqualTo("1");
        assertThat(datos.ubigeo()).isEqualTo("150101");
        assertThat(datos.diasCreditoDefault()).isEqualTo(30);
        assertThat(datos.monedaDefault()).isEqualTo("USD");
        assertThat(datos.esLaboratorio()).isTrue();
        assertThat(datos.esImportador()).isTrue();
        assertThat(datos.esDistribuidor()).isFalse();
        assertThat(datos.calificacion()).isEqualTo("EXCELENTE");
        assertThat(datos.contactoEmail()).isEqualTo("ana@proveedor.example");
    }

    @Test
    void rejectsADocumentTypeLongerThanTwoCharacters() {
        assertInvalid(crear("123", "20100070970", "Laboratorios", null, null, null, null));
    }

    @Test
    void rejectsAMissingOrOversizedDocumentNumber() {
        assertInvalid(crear(null, null, "Laboratorios", null, null, null, null));
        assertInvalid(crear("1", "1234567890123456", "Laboratorios", null, null, null, null));
    }

    @Test
    void rejectsAnRucThatDoesNotStartWithTenOrTwentyOrIsNotElevenDigits() {
        assertInvalid(crear("6", "30100070970", "Laboratorios", null, null, null, null));
        assertInvalid(crear("6", "2010007097", "Laboratorios", null, null, null, null));
        assertThat(domainValue(crear("6", "10100070970", "Persona", null, null, null, null)).numeroDocumento())
                .isEqualTo("10100070970");
    }

    @Test
    void rejectsAMissingOrOversizedBusinessName() {
        assertInvalid(crear(null, "20100070970", null, null, null, null, null));
        assertInvalid(crear(null, "20100070970", "x".repeat(301), null, null, null, null));
    }

    @Test
    void rejectsAnUbigeoThatIsNotSixDigits() {
        assertInvalid(crear(null, "20100070970", "Laboratorios", null, "15010", null, null));
    }

    @Test
    void rejectsNegativeCreditDays() {
        assertInvalid(crear(null, "20100070970", "Laboratorios", null, null, -1, null));
    }

    @Test
    void rejectsACurrencyThatIsNotThreeUppercaseLetters() {
        assertInvalid(crear(null, "20100070970", "Laboratorios", null, null, null, "pen"));
    }

    @Test
    void rejectsAnOptionalFieldLongerThanItsColumn() {
        var result = crear(null, "20100070970", "Laboratorios", "x".repeat(301), null, null, null);

        assertThat(domainError(result).message()).contains("nombreComercial").contains("300");
    }
}
