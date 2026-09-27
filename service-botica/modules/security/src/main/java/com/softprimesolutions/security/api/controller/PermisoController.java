package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.security.api.mapper.IamApiMapper;
import com.softprimesolutions.security.application.dto.query.ListarPermisosQuery;
import com.softprimesolutions.security.application.port.in.ListarPermisosUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/permisos")
public class PermisoController {

    private final ListarPermisosUseCase listPermissions;

    public PermisoController(ListarPermisosUseCase listPermissions) {
        this.listPermissions = listPermissions;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('seguridad.permisos.consultar')")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return listPermissions.execute(new ListarPermisosQuery(search, page, size)).fold(
                result -> ResponseEntity.ok(IamApiMapper.toPermisoPage(result)),
                IamControllerSupport::problem);
    }
}
