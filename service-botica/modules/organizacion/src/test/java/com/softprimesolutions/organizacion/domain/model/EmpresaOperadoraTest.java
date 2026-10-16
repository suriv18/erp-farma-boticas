package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmpresaOperadoraTest {

    private static final EmpresaOperadoraId ID = new EmpresaOperadoraId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void createsWithValidData() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", "Boticas",
                "Av. Siempre Viva 123", "150101", "014445566", "contacto@boticas.pe",
                "https://boticas.pe", "PEN", "America/Lima", true, NOW);

        assertThat(result.isSuccess()).isTrue();
        var empresa = result.fold(e -> e, error -> {
            throw new AssertionError(error.message());
        });
        assertThat(empresa.id()).isEqualTo(ID);
        assertThat(empresa.tenantId()).isEqualTo(TENANT_ID);
        assertThat(empresa.ruc()).isEqualTo("20123456786");
        assertThat(empresa.razonSocial()).isEqualTo("Boticas SAC");
        assertThat(empresa.nombreComercial()).isEqualTo("Boticas");
        assertThat(empresa.direccionFiscal()).isEqualTo("Av. Siempre Viva 123");
        assertThat(empresa.ubigeoFiscal()).isEqualTo("150101");
        assertThat(empresa.telefono()).isEqualTo("014445566");
        assertThat(empresa.email()).isEqualTo("contacto@boticas.pe");
        assertThat(empresa.sitioWeb()).isEqualTo("https://boticas.pe");
        assertThat(empresa.monedaFuncional()).isEqualTo("PEN");
        assertThat(empresa.zonaHoraria()).isEqualTo("America/Lima");
        assertThat(empresa.permiteVentaOnline()).isTrue();
        assertThat(empresa.estado()).isEqualTo(EstadoEmpresaOperadora.ACTIVO);
        assertThat(empresa.createdAt()).isEqualTo(NOW);
        assertThat(empresa.updatedAt()).isNull();
    }

    @Test
    void createsWithOnlyRequiredFields() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "10123456781", "Juan Perez EIRL", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = EmpresaOperadora.create(
                null, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = EmpresaOperadora.create(
                ID, null, "20123456786", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullRuc() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, null, "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsRucWithWrongLength() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "2012345678", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(e -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_EMPRESA_INVALIDA");
            assertThat(error.metadata()).containsEntry("field", "ruc");
            return null;
        });
    }

    @Test
    void rejectsRucNotStartingWith10Or20() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "30123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankRazonSocial() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", " ", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsRazonSocialTooShort() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "A", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsRazonSocialTooLong() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "R".repeat(301), null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreComercialTooLong() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", "N".repeat(301),
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void acceptsNombreComercialWithinLimit() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", "N".repeat(300),
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsInvalidUbigeoFiscal() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, "15010", null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void acceptsValidUbigeoFiscal() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, "150101", null, null, null, "PEN", "America/Lima", false, NOW);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsMonedaFuncionalWithWrongLength() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, null, null, null, null, "SOLES", "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullMonedaFuncional() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, null, null, null, null, null, "America/Lima", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankZonaHoraria() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, null, null, null, null, "PEN", " ", false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullZonaHoraria() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, null, null, null, null, "PEN", null, false, NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var empresa = EmpresaOperadora.restore(
                ID, TENANT_ID, "20123456786", "Boticas SAC", "Boticas",
                "Av. Siempre Viva 123", "150101", "014445566", "contacto@boticas.pe",
                "https://boticas.pe", "PEN", "America/Lima", true,
                EstadoEmpresaOperadora.SUSPENDIDO, NOW, NOW.plusSeconds(60));

        assertThat(empresa.estado()).isEqualTo(EstadoEmpresaOperadora.SUSPENDIDO);
        assertThat(empresa.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetailsKeepingRucAndId() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var updated = empresa.updateDetails(
                        "Boticas del Peru SAC", "Boticas", "Nueva direccion", "150102",
                        "014445577", "nuevo@boticas.pe", "https://boticas.pe", "PEN",
                        "America/Lima", true, NOW.plusSeconds(120))
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.ruc()).isEqualTo("20123456786");
        assertThat(updated.razonSocial()).isEqualTo("Boticas del Peru SAC");
        assertThat(updated.permiteVentaOnline()).isTrue();
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithBlankRazonSocial() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                " ", null, null, null, null, null, null, "PEN", "America/Lima", false,
                NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithInvalidRazonSocial() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "A", null, null, null, null, null, null, "PEN", "America/Lima", false,
                NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithRazonSocialTooLong() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "R".repeat(301), null, null, null, null, null, null, "PEN",
                "America/Lima", false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNombreComercialTooLong() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", "N".repeat(301), null, null, null, null, null, "PEN",
                "America/Lima", false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void acceptsUpdateWithNombreComercialWithinLimit() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", "N".repeat(300), null, null, null, null, null, "PEN",
                "America/Lima", false, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsUpdateWithInvalidUbigeoFiscal() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, "15010", null, null, null, "PEN",
                "America/Lima", false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void acceptsUpdateWithValidUbigeoFiscal() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, "150101", null, null, null, "PEN",
                "America/Lima", false, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsUpdateWithInvalidMonedaFuncional() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, null, null, null, null, "SOLES",
                "America/Lima", false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullMonedaFuncional() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, null, null, null, null, null,
                "America/Lima", false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithBlankZonaHoraria() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, null, null, null, null, "PEN",
                " ", false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullZonaHoraria() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, null, null, null, null, "PEN",
                null, false, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.updateDetails(
                "Boticas SAC", null, null, null, null, null, null, "PEN", "America/Lima",
                false, null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void changesEstado() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.cambiarEstado(EstadoEmpresaOperadora.SUSPENDIDO, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
        result.fold(e -> {
            assertThat(e.estado()).isEqualTo(EstadoEmpresaOperadora.SUSPENDIDO);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void rejectsEstadoChangeWithNullValue() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.cambiarEstado(null, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsEstadoChangeWithNullUpdatedAt() {
        var empresa = EmpresaOperadora.create(
                        ID, TENANT_ID, "20123456786", "Boticas SAC", null,
                        null, null, null, null, null, "PEN", "America/Lima", false, NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = empresa.cambiarEstado(EstadoEmpresaOperadora.SUSPENDIDO, null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsRucWithInvalidCheckDigit() {
        var result = EmpresaOperadora.create(
                ID, TENANT_ID, "20123456789", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.message()).isEqualTo("El RUC no es válido: el dígito verificador no coincide.");
            assertThat(error.metadata()).containsEntry("field", "ruc");
            return null;
        });
    }

    @Test
    void acceptsRucWhenTheCheckDigitIsZeroOrOne() {
        var withZero = EmpresaOperadora.create(
                ID, TENANT_ID, "20000000010", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);
        var withOne = EmpresaOperadora.create(
                ID, TENANT_ID, "20000000061", "Boticas SAC", null,
                null, null, null, null, null, "PEN", "America/Lima", false, NOW);

        assertThat(withZero.isSuccess()).isTrue();
        assertThat(withOne.isSuccess()).isTrue();
    }
}
