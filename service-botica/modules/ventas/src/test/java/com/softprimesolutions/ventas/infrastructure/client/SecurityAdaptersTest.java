package com.softprimesolutions.ventas.infrastructure.client;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.security.api.RecursoAmbito;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class SecurityAdaptersTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void translatesTheRequestedScopeToTheSecurityContract() {
        var recibidos = new ArrayList<List<Object>>();
        var adapter = new SecurityAmbitoAdapter((tenantId, usuarioId, permiso, recurso) -> {
            recibidos.add(List.of(tenantId, usuarioId, permiso, recurso));
            return true;
        });

        var permitido = adapter.permite(
                new Ambito(TENANT, ACTOR_ID, "ventas.ventas.registrar", ESTABLECIMIENTO, ALMACEN, TERMINAL));

        assertThat(permitido).isTrue();
        assertThat(recibidos).containsExactly(List.of(
                TENANT, ACTOR_ID, "ventas.ventas.registrar", new RecursoAmbito(ESTABLECIMIENTO, ALMACEN, TERMINAL)));
    }

    @Test
    void reportsFalseWhenSecurityDeniesTheScope() {
        var adapter = new SecurityAmbitoAdapter((tenantId, usuarioId, permiso, recurso) -> false);

        assertThat(adapter.permite(new Ambito(TENANT, ACTOR_ID, "ventas.ventas.anular", null, null, null))).isFalse();
    }

    @Test
    void resolvesTheCurrentActorFromTheAuthenticatedJwtSubject() {
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject(ACTOR_ID.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));

        assertThat(new SecurityContextActorAdapter().actorId()).isEqualTo(ACTOR_ID);
    }
}
