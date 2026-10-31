package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.mapper.MiPuestoApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.port.in.ObtenerMiPuestoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizacion/mi-puesto")
public class MiPuestoController {

    private final ObtenerMiPuestoUseCase getWorkstation;

    public MiPuestoController(ObtenerMiPuestoUseCase getWorkstation) {
        this.getWorkstation = getWorkstation;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> get(@AuthenticationPrincipal Jwt jwt) {
        var query = new ObtenerMiPuestoQuery(
                OrganizacionControllerSupport.tenantOf(jwt), OrganizacionControllerSupport.actorOf(jwt));
        return getWorkstation.execute(query).fold(
                result -> ResponseEntity.ok(MiPuestoApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
