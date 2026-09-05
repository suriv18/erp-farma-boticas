package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.security.api.dto.request.CrearRolRequest;
import com.softprimesolutions.security.api.dto.request.ReemplazarPermisosRolRequest;
import com.softprimesolutions.security.api.mapper.IamApiMapper;
import com.softprimesolutions.security.application.dto.query.ListarRolesQuery;
import com.softprimesolutions.security.application.port.in.CrearRolUseCase;
import com.softprimesolutions.security.application.port.in.ListarRolesUseCase;
import com.softprimesolutions.security.application.port.in.ReemplazarPermisosRolUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/roles")
public class RolController {

    private final CrearRolUseCase createRole;
    private final ListarRolesUseCase listRoles;
    private final ReemplazarPermisosRolUseCase replacePermissions;

    public RolController(
            CrearRolUseCase createRole,
            ListarRolesUseCase listRoles,
            ReemplazarPermisosRolUseCase replacePermissions) {
        this.createRole = createRole;
        this.listRoles = listRoles;
        this.replacePermissions = replacePermissions;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('seguridad.roles.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearRolRequest request) {
        return createRole.execute(IamApiMapper.toCommand(request)).fold(
                result -> ResponseEntity.created(URI.create("/api/v1/roles/" + result.id()))
                        .body(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('seguridad.roles.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listRoles.execute(new ListarRolesQuery(tenantId, search, page, size)).fold(
                result -> ResponseEntity.ok(IamApiMapper.toRolPage(result)),
                IamControllerSupport::problem);
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('seguridad.permisos.asignar')")
    public ResponseEntity<?> replacePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody ReemplazarPermisosRolRequest request,
            Authentication authentication) {
        return replacePermissions.execute(IamApiMapper.toCommand(roleId, request, authentication.getName())).fold(
                result -> ResponseEntity.ok(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }
}
