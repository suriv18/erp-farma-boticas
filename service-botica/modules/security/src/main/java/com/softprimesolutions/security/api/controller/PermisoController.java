package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.security.api.mapper.IamApiMapper;
import com.softprimesolutions.security.application.dto.query.ListarPermisosQuery;
import com.softprimesolutions.security.application.port.in.ListarPermisosUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permisos")
public class PermisoController {

    private final ListarPermisosUseCase listPermissions;

    public PermisoController(ListarPermisosUseCase listPermissions) {
        this.listPermissions = listPermissions;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('seguridad.permisos.consultar')")
    public ResponseEntity<?> list(@RequestParam(required = false) String search) {
        return listPermissions.execute(new ListarPermisosQuery(search)).fold(
                results -> ResponseEntity.ok(results.stream().map(IamApiMapper::toResponse).toList()),
                IamControllerSupport::problem);
    }
}
