package com.softprimesolutions.inventario.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyKeysTest {

    @Test
    void aMissingKeyHasNoBusinessUuid() {
        assertThat(IdempotencyKeys.businessUuid(null)).isNull();
    }

    @Test
    void theBusinessUuidIsDeterministicAndIgnoresSurroundingBlanks() {
        var esperado = UUID.nameUUIDFromBytes("idempotency:abc".getBytes(StandardCharsets.UTF_8));

        assertThat(IdempotencyKeys.businessUuid("abc")).isEqualTo(esperado);
        assertThat(IdempotencyKeys.businessUuid("  abc ")).isEqualTo(esperado);
    }

    @Test
    void eachTramoHasItsOwnStableKeyOfFixedLength() {
        var primero = IdempotencyKeys.tramo("venta-1", 1);

        assertThat(primero).isEqualTo(IdempotencyKeys.tramo(" venta-1 ", 1)).hasSize(36);
        assertThat(primero).isNotEqualTo(IdempotencyKeys.tramo("venta-1", 2));
        assertThat(primero).isNotEqualTo(IdempotencyKeys.tramo("venta-2", 1));
    }

    @Test
    void eachOriginalMovementHasAStableReversalKey() {
        var id = UUID.fromString("12345678-9abc-4def-8123-456789abcdef");

        assertThat(IdempotencyKeys.reverso(id)).isEqualTo("reverso:12345678-9abc-4def-8123-456789abcdef");
        assertThat(IdempotencyKeys.reverso(id)).isEqualTo(IdempotencyKeys.reverso(id));
        assertThat(IdempotencyKeys.reverso(id)).isNotEqualTo(IdempotencyKeys.reverso(UUID.randomUUID()));
    }
}
