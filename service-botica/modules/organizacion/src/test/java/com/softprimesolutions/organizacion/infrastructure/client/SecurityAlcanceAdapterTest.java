package com.softprimesolutions.organizacion.infrastructure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.security.api.AlcanceResultado;
import com.softprimesolutions.security.api.AlcanceUsuario;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SecurityAlcanceAdapterTest {

    @Test
    void translatesTheSecurityScopeIntoTheOrganizationScope() {
        var tenant = UUID.randomUUID();
        var usuario = UUID.randomUUID();
        var empresa = UUID.randomUUID();
        var terminal = UUID.randomUUID();
        var seguridad = mock(AlcanceUsuario.class);
        when(seguridad.alcanceDe(tenant, usuario))
                .thenReturn(new AlcanceResultado(true, Set.of(empresa), Set.of(), Set.of(), Set.of(terminal)));

        var alcance = new SecurityAlcanceAdapter(seguridad).alcanceDe(tenant, usuario);

        assertThat(alcance).isEqualTo(new AlcancePuesto(true, Set.of(empresa), Set.of(), Set.of(), Set.of(terminal)));
    }
}
