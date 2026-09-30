package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class EstructuraCorporativaControllerTest {

    private static final EstructuraCorporativaResult STRUCTURE = new EstructuraCorporativaResult(
            Instant.parse("2026-01-01T00:00:00Z"),
            List.of(new EmpresaNodoResult(UUID.randomUUID(), "Boticas SAC", null, "ACTIVE", List.of())));

    private static Jwt tokenOf(UUID tenantId) {
        return Jwt.withTokenValue("token").header("alg", "none").claim("tid", tenantId.toString()).build();
    }

    @Test
    void resolvesTheTenantFromTheTokenAndReturnsTheStructure() {
        var queries = new ArrayList<ObtenerEstructuraCorporativaQuery>();
        var controller = new EstructuraCorporativaController(query -> {
            queries.add(query);
            return ok(STRUCTURE);
        });

        var response = controller.get(tokenOf(TENANT));

        assertOk(response, OrganizacionApiMapper.toResponse(STRUCTURE));
        assertThat(queries).containsExactly(new ObtenerEstructuraCorporativaQuery(TENANT));
    }

    @Test
    void mapsFailuresToProblemDetails() {
        var controller = new EstructuraCorporativaController(query -> conflict());

        assertConflict(controller.get(tokenOf(TENANT)));
    }
}
