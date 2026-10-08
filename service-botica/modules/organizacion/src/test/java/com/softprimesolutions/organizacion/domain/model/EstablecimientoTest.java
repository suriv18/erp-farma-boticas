package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstablecimientoTest {

    private static final EstablecimientoId ID = new EstablecimientoId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final EmpresaOperadoraId EMPRESA_ID = new EmpresaOperadoraId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-09-27T00:00:00Z");

    @Test
    void createsWithValidData() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", "DIG-001", "Av. Principal 100", "150101", "Cerca al parque",
                new BigDecimal("-12.0464000"), new BigDecimal("-77.0428000"), "014445566",
                "sede@boticas.pe", true, true, false, PerfilOperacion.ONLINE, "America/Lima", NOW);

        assertThat(result.isSuccess()).isTrue();
        var establecimiento = result.fold(e -> e, error -> { throw new AssertionError(error.message()); });
        assertThat(establecimiento.id()).isEqualTo(ID);
        assertThat(establecimiento.tenantId()).isEqualTo(TENANT_ID);
        assertThat(establecimiento.empresaId()).isEqualTo(EMPRESA_ID);
        assertThat(establecimiento.codigo()).isEqualTo("EST-01");
        assertThat(establecimiento.nombre()).isEqualTo("Sede Central");
        assertThat(establecimiento.tipoEstablecimiento()).isEqualTo(TipoEstablecimiento.BOTICA);
        assertThat(establecimiento.categoriaRegulatoriaCodigo()).isNull();
        assertThat(establecimiento.codigoAnexoSunat()).isEqualTo("0000");
        assertThat(establecimiento.codigoDigemid()).isEqualTo("DIG-001");
        assertThat(establecimiento.direccion()).isEqualTo("Av. Principal 100");
        assertThat(establecimiento.ubigeo()).isEqualTo("150101");
        assertThat(establecimiento.referencia()).isEqualTo("Cerca al parque");
        assertThat(establecimiento.latitud()).isEqualTo(new BigDecimal("-12.0464000"));
        assertThat(establecimiento.longitud()).isEqualTo(new BigDecimal("-77.0428000"));
        assertThat(establecimiento.telefono()).isEqualTo("014445566");
        assertThat(establecimiento.email()).isEqualTo("sede@boticas.pe");
        assertThat(establecimiento.esPrincipal()).isTrue();
        assertThat(establecimiento.permiteVentaOnline()).isTrue();
        assertThat(establecimiento.permiteDelivery()).isFalse();
        assertThat(establecimiento.zonaHoraria()).isEqualTo("America/Lima");
        assertThat(establecimiento.estadoOperativo()).isEqualTo(EstadoEstablecimiento.ACTIVO);
        assertThat(establecimiento.updatedAt()).isNull();
    }

    @Test
    void createsWithOnlyRequiredFields() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-02", "Sede Norte", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.STORE_EDGE, "America/Lima", NOW);

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNullId() {
        var result = Establecimiento.create(
                null, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTenantId() {
        var result = Establecimiento.create(
                ID, null, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullEmpresaId() {
        var result = Establecimiento.create(
                ID, TENANT_ID, null, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsBlankCodigo() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, " ", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
        result.fold(e -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_ESTABLECIMIENTO_INVALIDO");
            return null;
        });
    }

    @Test
    void rejectsCodigoTooLong() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "E".repeat(41), "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullNombre() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", null, TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreTooShort() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "A", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNombreTooLong() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "A".repeat(251), TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsInvalidCodigoAnexoSunat() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCodigoAnexoSunat() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, null, null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsCodigoDigemidTooLong() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", "D".repeat(11), null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsInvalidUbigeo() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, "15010", null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullTipoEstablecimiento() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", null,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullPerfilOperacion() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, null, "America/Lima", NOW);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsNullCreatedAt() {
        var result = Establecimiento.create(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima", null);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void restoresWithoutValidation() {
        var establecimiento = Establecimiento.restore(
                ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                null, "0000", null, null, null, null, null, null, null, null,
                false, false, false, PerfilOperacion.ONLINE, "America/Lima",
                EstadoEstablecimiento.CLAUSURADO, NOW, NOW.plusSeconds(60));

        assertThat(establecimiento.estadoOperativo()).isEqualTo(EstadoEstablecimiento.CLAUSURADO);
        assertThat(establecimiento.updatedAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void updatesDetailsKeepingEmpresaIdAndCodigo() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var updated = establecimiento.updateDetails(
                        "Sede Central Remodelada", TipoEstablecimiento.BOTICA, null, "0000", "DIG-002",
                        "Nueva direccion", "150102", null, null, null, null, "sede2@boticas.pe",
                        true, true, true, PerfilOperacion.STORE_EDGE, "America/Lima", NOW.plusSeconds(120))
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        assertThat(updated.empresaId()).isEqualTo(EMPRESA_ID);
        assertThat(updated.codigo()).isEqualTo("EST-01");
        assertThat(updated.nombre()).isEqualTo("Sede Central Remodelada");
        assertThat(updated.perfilOperacion()).isEqualTo(PerfilOperacion.STORE_EDGE);
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void rejectsUpdateWithNullUpdatedAt() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "0000", null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", null);

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullTipoEstablecimiento() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", null, null, "0000", null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullPerfilOperacion() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "0000", null, null, null, null,
                null, null, null, null, false, false, false, null,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullNombre() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                null, TipoEstablecimiento.BOTICA, null, "0000", null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNombreTooShort() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "A", TipoEstablecimiento.BOTICA, null, "0000", null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNombreTooLong() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "A".repeat(251), TipoEstablecimiento.BOTICA, null, "0000", null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithNullCodigoAnexoSunat() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, null, null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithInvalidCodigoAnexoSunat() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "000", null, null, null, null,
                null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithCodigoDigemidTooLong() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "0000", "D".repeat(11), null, null,
                null, null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsUpdateWithInvalidUbigeo() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "0000", null, null, "15010",
                null, null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void acceptsUpdateWithValidUbigeo() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "0000", null, null, "150101",
                null, null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void acceptsUpdateWithNullUbigeo() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.updateDetails(
                "Sede Central", TipoEstablecimiento.BOTICA, null, "0000", null, null, null,
                null, null, null, null, null, false, false, false, PerfilOperacion.ONLINE,
                "America/Lima", NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void changesEstadoOperativoWithoutErasingHistory() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.cambiarEstadoOperativo(
                EstadoEstablecimiento.CLAUSURADO, NOW.plusSeconds(60));

        assertThat(result.isSuccess()).isTrue();
        result.fold(e -> {
            assertThat(e.estadoOperativo()).isEqualTo(EstadoEstablecimiento.CLAUSURADO);
            assertThat(e.codigo()).isEqualTo("EST-01");
            assertThat(e.createdAt()).isEqualTo(NOW);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void rejectsEstadoChangeWithNullValue() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.cambiarEstadoOperativo(null, NOW.plusSeconds(60));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsEstadoChangeWithNullUpdatedAt() {
        var establecimiento = Establecimiento.create(
                        ID, TENANT_ID, EMPRESA_ID, "EST-01", "Sede Central", TipoEstablecimiento.BOTICA,
                        null, "0000", null, null, null, null, null, null, null, null,
                        false, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(e -> e, error -> { throw new AssertionError(error.message()); });

        var result = establecimiento.cambiarEstadoOperativo(EstadoEstablecimiento.CLAUSURADO, null);

        assertThat(result.isFailure()).isTrue();
    }
}
