package com.softprimesolutions.clientes.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoClienteTest {

    @Test
    void parsesAKnownStatusByItsExactName() {
        assertThat(EstadoCliente.parse("BLOQUEADO")).contains(EstadoCliente.BLOQUEADO);
    }

    @Test
    void rejectsUnknownOrMissingStatuses() {
        assertThat(EstadoCliente.parse("bloqueado")).isEmpty();
        assertThat(EstadoCliente.parse(null)).isEmpty();
    }
}
