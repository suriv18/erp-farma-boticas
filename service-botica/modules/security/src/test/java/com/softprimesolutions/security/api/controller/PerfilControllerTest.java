package com.softprimesolutions.security.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.softprimesolutions.security.application.dto.query.ObtenerMiPerfilQuery;
import com.softprimesolutions.security.application.dto.result.PerfilResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

class PerfilControllerTest {

    private static final UUID USER_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");

    private static Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(USER_ID.toString())
                .claim("tid", TENANT_ID.toString())
                .build();
    }

    @Test
    void returnsTheProfileOfTheUserIdentifiedByTheToken() {
        var queries = new ArrayList<ObtenerMiPerfilQuery>();
        var profile = new PerfilResult(USER_ID, TENANT_ID, "Luis Paredes Soto", "luis", "luis@boticas.pe", List.of());
        var controller = new PerfilController(query -> {
            queries.add(query);
            return Result.success(profile);
        });

        var response = controller.me(jwt());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(profile, response.getBody());
        assertEquals(List.of(new ObtenerMiPerfilQuery(TENANT_ID, USER_ID)), queries);
    }

    @Test
    void translatesAnApplicationFailureIntoAProblemResponse() {
        ApplicationError error = new StandardApplicationError(
                "SEC_USUARIO_NO_ENCONTRADO", "El usuario autenticado no existe.", ErrorCategory.NOT_FOUND);
        var controller = new PerfilController(query -> Result.failure(error));

        var response = controller.me(jwt());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
