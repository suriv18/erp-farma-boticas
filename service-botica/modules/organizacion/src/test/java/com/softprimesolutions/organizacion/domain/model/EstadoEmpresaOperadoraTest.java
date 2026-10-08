package com.softprimesolutions.organizacion.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadoEmpresaOperadoraTest {

    @Test
    void onlyActivoAdmitsChildAdditions() {
        assertThat(EstadoEmpresaOperadora.ACTIVO.admiteAltasDeHijos()).isTrue();
        assertThat(EstadoEmpresaOperadora.SUSPENDIDO.admiteAltasDeHijos()).isFalse();
        assertThat(EstadoEmpresaOperadora.BLOQUEADO.admiteAltasDeHijos()).isFalse();
    }
}
