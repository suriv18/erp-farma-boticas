package com.softprimesolutions.inventario.application.usecase.command;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

final class IdempotencyKeys {

    private IdempotencyKeys() {
    }

    static UUID businessUuid(String clave) {
        return Optional.ofNullable(clave)
                .map(valor -> uuidDe("idempotency:" + valor.trim()))
                .orElse(null);
    }

    static String tramo(String clave, int numero) {
        return uuidDe("tramo:" + clave.trim() + "#" + numero).toString();
    }

    private static UUID uuidDe(String texto) {
        return UUID.nameUUIDFromBytes(texto.getBytes(StandardCharsets.UTF_8));
    }
}
