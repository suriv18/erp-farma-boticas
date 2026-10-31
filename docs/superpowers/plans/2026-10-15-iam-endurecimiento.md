# Endurecimiento del IAM (subproyecto A) — plan de implementación

> Para agentes: ejecutar tarea por tarea con TDD (test que falla → implementación mínima → test en verde → commit). Los pasos usan casillas `- [ ]`.

**Objetivo:** cerrar las escaladas del IAM actual sin cambiar el modelo: tenant del token, roles de sistema inmutables, `systemRole` no editable por API y `nombre_mostrar` derivado.

**Diseño:** `docs/superpowers/specs/2026-10-15-organizacion-superadmin-delegacion-design.md`, sección 4.1. Este plan no incluye organización ni `ActorScope` (plan B1 aparte).

**Pila:** Java 25, Spring Boot 4, Spring Security (resource server JWT), JUnit 5, MockMvc. Gradle: `cd service-botica && ./gradlew.bat ...`.

## Restricciones globales

- Cobertura 100 % de líneas y ramas en toda clase **nueva** (gate JaCoCo por clase; las clases listadas en `modules/security/jacoco-baseline.txt` están exentas).
- Sin comentarios explicativos en el código; sin código duplicado; arquitectura hexagonal/CQRS existente.
- Los commits terminan con `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Estructura de archivos

| Archivo | Acción | Responsabilidad |
|---|---|---|
| `modules/security/.../infrastructure/configuration/TenantTokenGuardFilter.java` | Crear | Rechaza con 403 las peticiones cuyo parámetro `tenantId` no coincide con el `tid` del JWT |
| `modules/security/.../infrastructure/configuration/LocalAuthSecurityConfiguration.java` | Modificar | Registra el filtro tras la autenticación Bearer |
| `modules/security/.../api/controller/IamControllerSupport.java` | Modificar | `tenantMismatch(jwt, tenantId)` para los `tenantId` que llegan en el cuerpo |
| `UsuarioController`, `RolController`, `LocalAuthController`, `SecurityControlController` | Modificar | Aplicar `tenantMismatch` en los 5 endpoints con `tenantId` en el cuerpo |
| `application/usecase/command/ReemplazarPermisosRolHandler.java` | Modificar | Rechazar roles de sistema |
| `application/usecase/command/SecurityControlService.java` | Modificar | Rechazar cambio de estado de roles de sistema |
| `api/mapper/IamApiMapper.java` | Modificar | Ignorar `systemRole` del cliente |
| `application/usecase/command/CrearUsuarioHandler.java` | Modificar | Derivar `displayName` de nombres y apellidos |
| `bootstrap-app/src/test/.../IamApiIntegrationTest.java` | Modificar | Casos de integración con login real |

---

### Tarea 1: Guardia de tenant por parámetro (`TenantTokenGuardFilter`)

**Archivos:** crear `TenantTokenGuardFilter.java` y su test; modificar `LocalAuthSecurityConfiguration.java`.

**Interfaz que produce:** `new TenantTokenGuardFilter()` (extiende `OncePerRequestFilter`). Respuesta de rechazo: HTTP 403, `application/problem+json`, `{"status":403,"title":"Forbidden","detail":"El tenant de la petición no coincide con el de la sesión.","code":"SEC_TENANT_NO_COINCIDE"}`.

- [ ] **Paso 1: test que falla** — `modules/security/src/test/java/com/softprimesolutions/security/infrastructure/configuration/TenantTokenGuardFilterTest.java`:

```java
package com.softprimesolutions.security.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantTokenGuardFilterTest {

    private static final String TENANT = "7c61d383-861c-4c6b-9749-9096ce4a74cf";
    private static final String OTHER_TENANT = "172e0f26-a765-46f3-841c-4a11407ccf5b";

    private final TenantTokenGuardFilter filter = new TenantTokenGuardFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateWithTenant(String tenantId) {
        var jwt = Jwt.withTokenValue("token").header("alg", "HS256").claim("tid", tenantId).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private static FilterChain flag(AtomicBoolean called) {
        return (request, response) -> called.set(true);
    }

    @Test
    void letsTheRequestThroughWhenTheTenantParameterMatchesTheToken() throws Exception {
        authenticateWithTenant(TENANT);
        var request = new MockHttpServletRequest();
        request.setParameter("tenantId", TENANT);
        var called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), flag(called));

        assertTrue(called.get());
    }

    @Test
    void rejectsTheRequestWhenTheTenantParameterDiffersFromTheToken() throws Exception {
        authenticateWithTenant(TENANT);
        var request = new MockHttpServletRequest();
        request.setParameter("tenantId", OTHER_TENANT);
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();

        filter.doFilter(request, response, flag(called));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("SEC_TENANT_NO_COINCIDE"));
        assertEquals(false, called.get());
    }

    @Test
    void letsTheRequestThroughWhenThereIsNoTenantParameter() throws Exception {
        authenticateWithTenant(TENANT);
        var called = new AtomicBoolean();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), flag(called));

        assertTrue(called.get());
    }

    @Test
    void letsTheRequestThroughWhenTheCallerIsNotAuthenticatedWithAJwt() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("anonimo", "n/a"));
        var request = new MockHttpServletRequest();
        request.setParameter("tenantId", OTHER_TENANT);
        var called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), flag(called));

        assertTrue(called.get());
        assertNull(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().findAny().orElse(null));
    }
}
```

- [ ] **Paso 2:** `./gradlew.bat :modules:security:test --tests "*TenantTokenGuardFilterTest"` → falla por clase inexistente.

- [ ] **Paso 3: implementación** — `.../infrastructure/configuration/TenantTokenGuardFilter.java`:

```java
package com.softprimesolutions.security.infrastructure.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public final class TenantTokenGuardFilter extends OncePerRequestFilter {

    private static final String BODY = """
            {"status":403,"title":"Forbidden","detail":"El tenant de la petición no coincide con el de la sesión.","code":"SEC_TENANT_NO_COINCIDE"}""";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var requested = request.getParameter("tenantId");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (requested != null && authentication instanceof JwtAuthenticationToken token
                && !requested.equalsIgnoreCase(token.getToken().getClaimAsString("tid"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(BODY);
            return;
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Paso 4:** el test del paso 1 pasa (4/4). La aserción final del último test es una verificación redundante: eliminarla si molesta.

- [ ] **Paso 5: registro en la cadena** — en `LocalAuthSecurityConfiguration.localAuthSecurityFilterChain` añadir tras `.cors(Customizer.withDefaults())` la llamada `.addFilterAfter(new TenantTokenGuardFilter(), BearerTokenAuthenticationFilter.class)` (importar `org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter`).

- [ ] **Paso 6: integración** — en `IamApiIntegrationTest`, test `rejectsATenantParameterThatDiffersFromTheTokenTenant`: login con un usuario con permiso (reutilizar el helper del test `grantsEffectiveAuthoritiesFromEstablishmentScopedRoleAssignment`), `GET /api/v1/sesiones?tenantId=<otro UUID>` con su token → 403 con `$.code = SEC_TENANT_NO_COINCIDE`.

- [ ] **Paso 7:** `./gradlew.bat :modules:security:check :bootstrap-app:test --tests "*IamApiIntegrationTest"` → verde. Commit `fix(security): rechazar tenantId distinto al del token en parametros`.

### Tarea 2: Guardia de tenant en cuerpos

**Archivos:** `IamControllerSupport.java`, `UsuarioController.java` (`create`, `assignRole`), `RolController.java` (`create`), `LocalAuthController.java` (`provisionCredential`), `SecurityControlController.java` (`registerDevice`).

- [ ] **Paso 1: test de integración que falla** — `rejectsATenantInTheBodyThatDiffersFromTheTokenTenant`: con el token de un usuario con `seguridad.usuarios.gestionar`, `POST /api/v1/usuarios` con `tenantId` ajeno en el cuerpo → 403 `SEC_TENANT_NO_COINCIDE`.
- [ ] **Paso 2: helper** en `IamControllerSupport`:

```java
    static java.util.Optional<ResponseEntity<ProblemDetail>> tenantMismatch(Jwt jwt, UUID requestedTenantId) {
        if (tenantId(jwt).equals(requestedTenantId)) {
            return java.util.Optional.empty();
        }
        var problem = ProblemDetail.forStatusAndDetail(
                org.springframework.http.HttpStatus.FORBIDDEN, "El tenant de la petición no coincide con el de la sesión.");
        problem.setProperty("code", "SEC_TENANT_NO_COINCIDE");
        return java.util.Optional.of(ResponseEntity.status(problem.getStatus()).body(problem));
    }
```

- [ ] **Paso 3: aplicar** en los 5 endpoints. Patrón (ejemplo `UsuarioController.create`, que ya recibe `@Valid @RequestBody CrearUsuarioRequest request`): añadir `@AuthenticationPrincipal Jwt jwt` y comenzar el método con `var mismatch = IamControllerSupport.tenantMismatch(jwt, request.tenantId()); if (mismatch.isPresent()) return mismatch.get();`. Repetir con `request.tenantId()` en `assignRole`, `RolController.create`, `LocalAuthController.provisionCredential` y `SecurityControlController.registerDevice`.
- [ ] **Paso 4:** test en verde + `:modules:security:check`. Commit `fix(security): validar el tenant del cuerpo contra el token`.

### Tarea 3: Roles de sistema inmutables

**Archivos:** `ReemplazarPermisosRolHandler.java`, `SecurityControlService.java` y sus tests (`ReemplazarPermisosRolHandlerTest`, nuevo `SecurityControlServiceRoleStatusTest`).

- [ ] **Paso 1: test que falla** en `ReemplazarPermisosRolHandlerTest`: stub cuyo `findRole` devuelve un `Rol.restore(...)` con `systemRole=true` (o `Rol.create(..., true, NOW)`) y comando con `Set.of("seguridad.usuarios.consultar")` → falla con `SEC_ROL_SISTEMA_NO_EDITABLE`, y `port.saved` sigue en `false`.
- [ ] **Paso 2: implementación** — en `ReemplazarPermisosRolHandler.execute`, justo después de comprobar que el rol existe:

```java
        if (role.get().systemRole()) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_SISTEMA_NO_EDITABLE",
                    "Un rol de sistema no puede modificar sus permisos.",
                    ErrorCategory.CONFLICT));
        }
```

- [ ] **Paso 3:** en `SecurityControlService.changeRoleStatus`, antes de `controlPort.updateRoleStatus`, resolver el rol con `iamPort.findRole(roleId)`; si existe y es de sistema devolver el mismo error (`CONFLICT`). Test unitario con puertos falsos que cubra: rol de sistema (conflicto), rol normal (éxito) y rol inexistente (se delega en `updateRoleStatus`, que devuelve `SEC_ROL_NO_ENCONTRADO`).
- [ ] **Paso 4:** integración — `rejectsChangingPermissionsAndStatusOfASystemRole`: rol de sistema insertado con SQL (como `rejectsUpdatingASystemRoleWithConflict`) → `PUT /roles/{id}/permissions` y `PATCH /roles/{id}/estado` responden 409. Commit `fix(security): proteger los roles de sistema de cambios de permisos y estado`.

### Tarea 4: `systemRole` ya no viene del cliente

**Archivos:** `IamApiMapper.java`, nuevo `IamApiMapperCrearRolTest` (copiar el estilo de `IamApiMapperActualizarRolTest`).

- [ ] **Paso 1: test que falla:** `IamApiMapper.toCommand(new CrearRolRequest(TENANT, "CAJERO_X", "Cajero X", null, "ESTABLECIMIENTO", true)).systemRole()` es `false`.
- [ ] **Paso 2:** en `IamApiMapper.toCommand(CrearRolRequest)` pasar `false` en lugar de `Boolean.TRUE.equals(request.systemRole())`. El campo del request se conserva (compatibilidad) y se ignora.
- [ ] **Paso 3:** `./gradlew.bat :modules:security:check`. Commit `fix(security): ignorar systemRole enviado por el cliente al crear roles`.

### Tarea 5: `nombre_mostrar` derivado

**Archivos:** `CrearUsuarioHandler.java`, `CrearUsuarioHandlerTest.java`.

- [ ] **Paso 1: tests que fallan** (añadir a `CrearUsuarioHandlerTest`): comando con `displayName` nulo o en blanco, `firstNames "Luis"`, `lastNames "Paredes Soto"` → `displayName()` del resultado es `Luis Paredes Soto`; comando con `displayName` explícito lo conserva; sin nombres ni displayName el resultado mantiene el comportamiento actual.
- [ ] **Paso 2: implementación** — en `registerUsuario` usar `displayNameOf(command)` en lugar de `command.displayName()`:

```java
    private static String displayNameOf(CrearUsuarioCommand command) {
        if (command.displayName() != null && !command.displayName().isBlank()) {
            return command.displayName();
        }
        var derived = java.util.stream.Stream.of(command.firstNames(), command.lastNames())
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(" "));
        return derived.isEmpty() ? command.displayName() : derived;
    }
```

- [ ] **Paso 3:** `:modules:security:check` en verde. Commit `fix(security): derivar nombre_mostrar de nombres y apellidos al crear usuarios`.

### Tarea 6: Verificación final

- [ ] `./gradlew.bat check` completo → `BUILD SUCCESSFUL`.
- [ ] `docker compose build app && docker compose up -d app`; con `curl` y un token de cajero, comprobar 403 `SEC_TENANT_NO_COINCIDE` al pasar `?tenantId=` ajeno y 409 al editar los permisos del rol `ADMIN`.
- [ ] Actualizar el CLAUDE.md del repo si cambia lo que describe del módulo `security` (solo si aplica).
