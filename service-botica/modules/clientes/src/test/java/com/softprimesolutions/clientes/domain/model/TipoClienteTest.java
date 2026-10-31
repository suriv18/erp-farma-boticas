package com.softprimesolutions.clientes.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class TipoClienteTest {

    @Test
    void parsesAKnownTypeByItsExactName() {
        assertThat(TipoCliente.parse("JURIDICO")).contains(TipoCliente.JURIDICO);
    }

    @Test
    void rejectsUnknownOrMissingTypes() {
        assertThat(TipoCliente.parse("juridico")).isEqualTo(Optional.empty());
        assertThat(TipoCliente.parse(null)).isEmpty();
    }
}
