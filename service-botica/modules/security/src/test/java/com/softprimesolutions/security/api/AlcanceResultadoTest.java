package com.softprimesolutions.security.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlcanceResultadoTest {

    @Test
    void keepsAnImmutableCopyOfEveryScopeSet() {
        var empresa = UUID.randomUUID();
        var origen = new HashSet<>(Set.of(empresa));

        var alcance = new AlcanceResultado(true, origen, Set.of(), Set.of(), Set.of());
        origen.clear();

        assertThat(alcance.global()).isTrue();
        assertThat(alcance.empresas()).containsExactly(empresa);
        assertThatThrownBy(() -> alcance.empresas().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
