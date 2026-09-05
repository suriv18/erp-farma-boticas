package com.softprimesolutions.testing.fixture;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Identificadores deterministas para fixtures sin compartir modelos de negocio. */
public final class TestIds {

    private TestIds() {
    }

    public static UUID deterministic(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
