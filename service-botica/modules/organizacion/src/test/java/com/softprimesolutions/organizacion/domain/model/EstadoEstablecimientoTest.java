package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoEstablecimientoTest {

    @Test
    void activoAndRemodelacionAdmitChildAdditions() {
        assertThat(EstadoEstablecimiento.ACTIVO.admiteAltasDeHijos()).isTrue();
        assertThat(EstadoEstablecimiento.REMODELACION.admiteAltasDeHijos()).isTrue();
    }

    @Test
    void suspendidoAndClausuradoDoNotAdmitChildAdditions() {
        assertThat(EstadoEstablecimiento.SUSPENDIDO.admiteAltasDeHijos()).isFalse();
        assertThat(EstadoEstablecimiento.CLAUSURADO.admiteAltasDeHijos()).isFalse();
    }
}
