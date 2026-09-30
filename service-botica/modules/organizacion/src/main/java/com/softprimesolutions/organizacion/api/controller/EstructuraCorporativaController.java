package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstructuraCorporativaUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estructura-corporativa")
public class EstructuraCorporativaController {

    private final ObtenerEstructuraCorporativaUseCase getStructure;

    public EstructuraCorporativaController(ObtenerEstructuraCorporativaUseCase getStructure) {
        this.getStructure = getStructure;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('organizacion.empresas.consultar')")
    public ResponseEntity<?> get(@AuthenticationPrincipal Jwt jwt) {
        var query = new ObtenerEstructuraCorporativaQuery(OrganizacionControllerSupport.tenantOf(jwt));
        return getStructure.execute(query).fold(
                result -> ResponseEntity.ok(OrganizacionApiMapper.toResponse(result)),
                OrganizacionControllerSupport::problem);
    }
}
