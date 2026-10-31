package com.softprimesolutions.security.api.controller;

import static com.softprimesolutions.security.api.controller.IamControllerSupport.tenantId;
import static com.softprimesolutions.security.api.controller.IamControllerSupport.userId;

import com.softprimesolutions.security.application.dto.query.ObtenerMiPerfilQuery;
import com.softprimesolutions.security.application.port.in.ObtenerMiPerfilUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class PerfilController {

    private final ObtenerMiPerfilUseCase getProfile;

    public PerfilController(ObtenerMiPerfilUseCase getProfile) {
        this.getProfile = getProfile;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        return getProfile.execute(new ObtenerMiPerfilQuery(tenantId(jwt), userId(jwt))).fold(
                ResponseEntity::ok, IamControllerSupport::problem);
    }
}
