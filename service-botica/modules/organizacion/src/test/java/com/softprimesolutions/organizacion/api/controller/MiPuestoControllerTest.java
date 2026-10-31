package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.api.mapper.MiPuestoApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class MiPuestoControllerTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final MiPuestoResult RESULT = new MiPuestoResult(List.of());

    private static Jwt token() {
        return Jwt.withTokenValue("token").header("alg", "none").subject(USUARIO.toString())
                .claim("tid", TENANT.toString()).build();
    }

    @Test
    void resolvesTheTenantAndTheUserFromTheTokenAndReturnsTheWorkstation() {
        var queries = new ArrayList<ObtenerMiPuestoQuery>();
        var controller = new MiPuestoController(query -> {
            queries.add(query);
            return ok(RESULT);
        });

        var response = controller.get(token());

        assertOk(response, MiPuestoApiMapper.toResponse(RESULT));
        assertThat(queries).containsExactly(new ObtenerMiPuestoQuery(TENANT, USUARIO));
    }

    @Test
    void mapsFailuresToProblemDetails() {
        var controller = new MiPuestoController(query -> conflict());

        assertConflict(controller.get(token()));
    }
}
