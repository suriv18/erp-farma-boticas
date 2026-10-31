package com.softprimesolutions.clientes.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.clientes.domain.valueobject.ClienteId;
import com.softprimesolutions.clientes.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClienteTest {

    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
    private static final ClienteId ID = new ClienteId(UUID.randomUUID());
    private static final TenantId TENANT = new TenantId(UUID.randomUUID());

    private static Result<Cliente, ErrorDetail> natural(String tipoDocumento, String numeroDocumento) {
        return Cliente.create(ID, TENANT, TipoCliente.NATURAL, tipoDocumento, numeroDocumento,
                "Ana", "Pérez", null, null, null, null, NOW);
    }

    private static void assertInvalid(Result<Cliente, ErrorDetail> result, String field) {
        assertThat(result.isFailure()).isTrue();
        var error = result.fold(cliente -> null, detail -> detail);
        assertThat(error.code()).isEqualTo("CLI_CLIENTE_INVALIDO");
        assertThat(error.metadata()).containsEntry("field", field);
    }

    private static Cliente valid(Result<Cliente, ErrorDetail> result) {
        assertThat(result.isSuccess()).isTrue();
        return result.fold(cliente -> cliente, error -> null);
    }

    @Test
    void createsANaturalClientWithADniAndNormalizesItsText() {
        var cliente = valid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, " 1 ", " 12345678 ",
                "  Ana   María ", " Pérez  Gómez ", null, " ana@correo.pe ", " 999 ",
                LocalDate.parse("1990-05-10"), NOW));

        assertThat(cliente.id()).isEqualTo(ID);
        assertThat(cliente.tenantId()).isEqualTo(TENANT);
        assertThat(cliente.tipoCliente()).isEqualTo(TipoCliente.NATURAL);
        assertThat(cliente.tipoDocumento()).isEqualTo("1");
        assertThat(cliente.numeroDocumento()).isEqualTo("12345678");
        assertThat(cliente.nombres()).isEqualTo("Ana María");
        assertThat(cliente.apellidos()).isEqualTo("Pérez Gómez");
        assertThat(cliente.razonSocial()).isNull();
        assertThat(cliente.email()).isEqualTo("ana@correo.pe");
        assertThat(cliente.telefono()).isEqualTo("999");
        assertThat(cliente.fechaNacimiento()).isEqualTo(LocalDate.parse("1990-05-10"));
        assertThat(cliente.estado()).isEqualTo(EstadoCliente.ACTIVO);
        assertThat(cliente.createdAt()).isEqualTo(NOW);
    }

    @Test
    void createsAJuridicalClientWithARuc() {
        var cliente = valid(Cliente.create(ID, TENANT, TipoCliente.JURIDICO, "6", "20123456789",
                null, null, "Botica Central SAC", null, null, null, NOW));

        assertThat(cliente.razonSocial()).isEqualTo("Botica Central SAC");
        assertThat(cliente.nombres()).isNull();
    }

    @Test
    void createsAnAnonymousClientWithoutDocument() {
        var cliente = valid(Cliente.create(ID, TENANT, TipoCliente.SIN_IDENTIFICAR, null, null,
                null, null, null, null, null, null, NOW));

        assertThat(cliente.tipoDocumento()).isNull();
        assertThat(cliente.numeroDocumento()).isNull();
    }

    @Test
    void acceptsAnotherDocumentTypeWithAlphanumericNumber() {
        var cliente = valid(natural("7", "AB-1234567"));

        assertThat(cliente.numeroDocumento()).isEqualTo("AB-1234567");
    }

    @Test
    void rejectsMissingMandatoryData() {
        assertInvalid(Cliente.create(null, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, null, null, null, NOW), "id");
        assertInvalid(Cliente.create(ID, null, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, null, null, null, NOW), "tenantId");
        assertInvalid(Cliente.create(ID, TENANT, null, "1", "12345678",
                "Ana", "Pérez", null, null, null, null, NOW), "tipoCliente");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, null, null, null, null), "createdAt");
    }

    @Test
    void requiresTheDocumentTypeAndNumberTogether() {
        assertInvalid(natural("1", null), "numeroDocumento");
        assertInvalid(natural(null, "12345678"), "numeroDocumento");
        assertInvalid(natural(" ", "12345678"), "numeroDocumento");
    }

    @Test
    void anAnonymousClientCannotCarryADocumentAndAnIdentifiedOneMust() {
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.SIN_IDENTIFICAR, "1", "12345678",
                null, null, null, null, null, null, NOW), "tipoDocumento");
        assertInvalid(natural(null, null), "tipoDocumento");
    }

    @Test
    void validatesTheDocumentFormatForItsType() {
        assertInvalid(natural("1", "1234567"), "numeroDocumento");
        assertInvalid(natural("1", "1234567A"), "numeroDocumento");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.JURIDICO, "6", "2012345678",
                null, null, "Botica SAC", null, null, null, NOW), "numeroDocumento");
        assertInvalid(natural("7", "ABCDEFGHIJKLMNOP"), "numeroDocumento");
        assertInvalid(natural("7", "AB 123"), "numeroDocumento");
    }

    @Test
    void explainsEachDocumentFormatInItsMessage() {
        assertThat(natural("1", "1").fold(c -> "", ErrorDetail::message)).contains("DNI");
        assertThat(natural("6", "1").fold(c -> "", ErrorDetail::message)).contains("RUC");
        assertThat(natural("7", "!").fold(c -> "", ErrorDetail::message)).contains("alfanuméricos");
    }

    @Test
    void requiresTheNameOfANaturalAndTheBusinessNameOfAJuridicalClient() {
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                null, "Pérez", null, null, null, null, NOW), "nombres");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", " ", null, null, null, null, NOW), "nombres");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.JURIDICO, "6", "20123456789",
                null, null, " ", null, null, null, NOW), "razonSocial");
    }

    @Test
    void limitsTheLengthOfNamesAndBusinessName() {
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "a".repeat(151), "Pérez", null, null, null, null, NOW), "nombres");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "a".repeat(181), null, null, null, null, NOW), "nombres");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.JURIDICO, "6", "20123456789",
                null, null, "a".repeat(301), null, null, null, NOW), "nombres");
    }

    @Test
    void validatesEmailPhoneAndBirthDate() {
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, "sin-arroba", null, null, NOW), "email");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, null, "9".repeat(41), null, NOW), "telefono");
        assertInvalid(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, null, null, LocalDate.parse("2026-10-31"), NOW), "fechaNacimiento");
        assertThat(Cliente.create(ID, TENANT, TipoCliente.NATURAL, "1", "12345678",
                "Ana", "Pérez", null, null, null, LocalDate.parse("2026-10-30"), NOW).isSuccess()).isTrue();
    }

    @Test
    void restoresAClientWithoutRevalidating() {
        var cliente = Cliente.restore(ID, TENANT, TipoCliente.NATURAL, "1", "12345678", "Ana", "Pérez",
                null, null, null, null, EstadoCliente.BLOQUEADO, NOW);

        assertThat(cliente.estado()).isEqualTo(EstadoCliente.BLOQUEADO);
        assertThat(cliente.pullDomainEvents()).isEmpty();
    }

    @Test
    void identifiersRejectNullValues() {
        assertThatThrownBy(() -> new ClienteId(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TenantId(null)).isInstanceOf(NullPointerException.class);
    }
}
