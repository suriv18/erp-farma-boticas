package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import org.junit.jupiter.api.Test;

class EnumParserTest {

    @Test
    void parsesAnExactEnumName() {
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "SUSPENDIDO"))
                .contains(EstadoEmpresaOperadora.SUSPENDIDO);
    }

    @Test
    void returnsEmptyForUnknownBlankOrNullValues() {
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "FOO")).isEmpty();
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "suspendido")).isEmpty();
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, "")).isEmpty();
        assertThat(EnumParser.parse(EstadoEmpresaOperadora.class, null)).isEmpty();
    }

    @Test
    void listsTheAllowedValuesInDeclarationOrder() {
        assertThat(EnumParser.allowedValues(EstadoEmpresaOperadora.class))
                .isEqualTo("ACTIVO, SUSPENDIDO, BLOQUEADO");
    }
}
