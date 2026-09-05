package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.security.api.dto.request.AsignarRolUsuarioRequest;
import com.softprimesolutions.security.api.dto.request.CrearUsuarioRequest;
import com.softprimesolutions.security.api.mapper.IamApiMapper;
import com.softprimesolutions.security.application.dto.query.ListarUsuariosQuery;
import com.softprimesolutions.security.application.port.in.AsignarRolUsuarioUseCase;
import com.softprimesolutions.security.application.port.in.CrearUsuarioUseCase;
import com.softprimesolutions.security.application.port.in.ListarUsuariosUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final CrearUsuarioUseCase createUser;
    private final ListarUsuariosUseCase listUsers;
    private final AsignarRolUsuarioUseCase assignRole;

    public UsuarioController(
            CrearUsuarioUseCase createUser,
            ListarUsuariosUseCase listUsers,
            AsignarRolUsuarioUseCase assignRole) {
        this.createUser = createUser;
        this.listUsers = listUsers;
        this.assignRole = assignRole;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('seguridad.usuarios.gestionar')")
    public ResponseEntity<?> create(@Valid @RequestBody CrearUsuarioRequest request) {
        return createUser.execute(IamApiMapper.toCommand(request)).fold(
                result -> ResponseEntity.created(URI.create("/api/v1/usuarios/" + result.id()))
                        .body(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('seguridad.usuarios.consultar')")
    public ResponseEntity<?> list(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listUsers.execute(new ListarUsuariosQuery(tenantId, search, page, size)).fold(
                result -> ResponseEntity.ok(IamApiMapper.toUsuarioPage(result)),
                IamControllerSupport::problem);
    }

    @PostMapping("/{userId}/role-assignments")
    @PreAuthorize("hasAuthority('seguridad.roles.asignar')")
    public ResponseEntity<?> assignRole(
            @PathVariable UUID userId,
            @Valid @RequestBody AsignarRolUsuarioRequest request,
            Authentication authentication) {
        return assignRole.execute(IamApiMapper.toCommand(userId, request, authentication.getName())).fold(
                result -> ResponseEntity.created(URI.create(
                                "/api/v1/usuarios/" + userId + "/role-assignments/" + result.id()))
                        .body(IamApiMapper.toResponse(result)),
                IamControllerSupport::problem);
    }
}
