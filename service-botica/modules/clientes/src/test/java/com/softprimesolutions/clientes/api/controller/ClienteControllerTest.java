package com.softprimesolutions.clientes.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.clientes.api.dto.request.RegistrarClienteRequest;
import com.softprimesolutions.clientes.api.dto.response.ClienteResponse;
import com.softprimesolutions.clientes.api.dto.response.PaginaResponse;
import com.softprimesolutions.clientes.application.dto.command.RegistrarClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import com.softprimesolutions.clientes.application.dto.result.RegistroClienteResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.jwt.Jwt;

class ClienteControllerTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID CLIENTE = UUID.randomUUID();
    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none").claim("tid", TENANT.toString())
            .build();
    private static final ClienteResult RESULT = new ClienteResult(CLIENTE, TENANT, "NATURAL", "1", "12345678", "Ana",
            "Pérez", null, null, null, null, "ACTIVO", Instant.parse("2026-10-30T15:00:00Z"));

    private static final RegistrarClienteRequest REQUEST = new RegistrarClienteRequest(
            "NATURAL", "1", "12345678", "Ana", "Pérez", null, null, null, null);

    private final List<RegistrarClienteCommand> registered = new ArrayList<>();

    private static <T> Result<T, ApplicationError> conflict() {
        return Result.failure(new StandardApplicationError("CLI_CONFLICTO", "Conflicto.", ErrorCategory.CONFLICT));
    }

    private ClienteController succeeding(boolean creado) {
        return new ClienteController(
                command -> {
                    registered.add(command);
                    return Result.success(new RegistroClienteResult(RESULT, creado));
                },
                query -> Result.success(RESULT),
                query -> Result.success(new PaginaResult<>(List.of(RESULT), query.page(), query.size(), 1)));
    }

    private final ClienteController failing = new ClienteController(
            command -> conflict(), query -> conflict(), query -> conflict());

    private static ClienteResponse response() {
        return new ClienteResponse(CLIENTE, "NATURAL", "1", "12345678", "Ana", "Pérez", null, null, null, null,
                "ACTIVO", Instant.parse("2026-10-30T15:00:00Z"));
    }

    @Test
    void registersANewClientAndReturnsItsLocation() {
        var entity = succeeding(true).register(JWT, REQUEST);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(entity.getHeaders().getLocation()).hasToString(ClienteController.BASE_PATH + "/" + CLIENTE);
        assertThat(entity.getBody()).isEqualTo(response());
        assertThat(registered).singleElement().satisfies(command -> {
            assertThat(command.tenantId()).isEqualTo(TENANT);
            assertThat(command.numeroDocumento()).isEqualTo("12345678");
        });
    }

    @Test
    void returnsTheExistingClientWithoutCreatingAnother() {
        var entity = succeeding(false).register(JWT, REQUEST);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getHeaders().getLocation()).isNull();
        assertThat(entity.getBody()).isEqualTo(response());
    }

    @Test
    void mapsRegistrationFailuresToProblemDetails() {
        var entity = failing.register(JWT, REQUEST);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(entity.getBody()).isInstanceOf(ProblemDetail.class);
    }

    @Test
    void listsClientsAsAPage() {
        var entity = succeeding(true).list(JWT, "ana", 1, 10);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(new PaginaResponse<>(List.of(response()), 1, 10, 1));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertThat(failing.list(JWT, null, 0, 20).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getsAClientById() {
        var entity = succeeding(true).getById(CLIENTE, JWT);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isEqualTo(response());
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertThat(failing.getById(CLIENTE, JWT).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
