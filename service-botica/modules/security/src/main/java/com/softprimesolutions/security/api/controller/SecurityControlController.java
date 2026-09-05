package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.security.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.security.api.dto.request.RegistrarDispositivoRequest;
import com.softprimesolutions.security.api.dto.request.RevocarSesionRequest;
import com.softprimesolutions.security.api.dto.request.VincularIdentidadExternaRequest;
import com.softprimesolutions.security.application.port.in.SecurityControlUseCase;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.RegisterDeviceData;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.ExternalIdentityData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SecurityControlController {

    private final SecurityControlUseCase control;

    public SecurityControlController(SecurityControlUseCase control) {
        this.control = control;
    }

    @PatchMapping("/usuarios/{userId}/estado")
    @PreAuthorize("hasAuthority('seguridad.usuarios.gestionar')")
    public ResponseEntity<?> changeUserStatus(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return control.changeUserStatus(tenantId, userId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @GetMapping("/usuarios/{userId}/identidades-externas")
    @PreAuthorize("hasAuthority('seguridad.usuarios.consultar')")
    public ResponseEntity<?> listExternalIdentities(
            @PathVariable UUID userId, @RequestParam UUID tenantId) {
        return control.listExternalIdentities(tenantId, userId).fold(
                ResponseEntity::ok, IamControllerSupport::problem);
    }

    @PostMapping("/usuarios/{userId}/identidades-externas")
    @PreAuthorize("hasAuthority('seguridad.identidades.gestionar')")
    public ResponseEntity<?> linkExternalIdentity(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody VincularIdentidadExternaRequest request) {
        var data = new ExternalIdentityData(
                request.provider(), request.subject(), request.issuer(), request.emailClaim());
        return control.linkExternalIdentity(tenantId, userId, data).fold(
                result -> ResponseEntity.created(URI.create(
                        "/api/v1/usuarios/" + userId + "/identidades-externas")).body(result),
                IamControllerSupport::problem);
    }

    @DeleteMapping("/usuarios/{userId}/identidades-externas")
    @PreAuthorize("hasAuthority('seguridad.identidades.gestionar')")
    public ResponseEntity<?> unlinkExternalIdentity(
            @PathVariable UUID userId,
            @RequestParam UUID tenantId,
            @RequestParam @NotBlank String provider,
            @RequestParam @NotBlank String subject) {
        return control.unlinkExternalIdentity(tenantId, userId, provider, subject).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @GetMapping("/usuarios/{userId}/asignaciones-rol")
    @PreAuthorize("hasAuthority('seguridad.roles.consultar')")
    public ResponseEntity<?> listAssignments(@PathVariable UUID userId, @RequestParam UUID tenantId) {
        return control.listAssignments(tenantId, userId).fold(ResponseEntity::ok, IamControllerSupport::problem);
    }

    @DeleteMapping("/usuarios/{userId}/asignaciones-rol/{assignmentId}")
    @PreAuthorize("hasAuthority('seguridad.roles.asignar')")
    public ResponseEntity<?> revokeAssignment(
            @PathVariable UUID userId,
            @PathVariable UUID assignmentId,
            @RequestParam UUID tenantId) {
        return control.revokeAssignment(tenantId, userId, assignmentId).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @GetMapping("/usuarios/{userId}/permisos-efectivos")
    @PreAuthorize("hasAuthority('seguridad.permisos.consultar')")
    public ResponseEntity<?> listEffectivePermissions(
            @PathVariable UUID userId, @RequestParam UUID tenantId) {
        return control.listEffectivePermissions(tenantId, userId).fold(
                ResponseEntity::ok, IamControllerSupport::problem);
    }

    @PatchMapping("/roles/{roleId}/estado")
    @PreAuthorize("hasAuthority('seguridad.roles.gestionar')")
    public ResponseEntity<?> changeRoleStatus(
            @PathVariable UUID roleId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return control.changeRoleStatus(tenantId, roleId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @GetMapping("/modulos")
    @PreAuthorize("hasAuthority('seguridad.modulos.consultar')")
    public ResponseEntity<?> listModules() {
        return control.listModules().fold(ResponseEntity::ok, IamControllerSupport::problem);
    }

    @GetMapping("/sesiones")
    @PreAuthorize("hasAuthority('seguridad.sesiones.consultar')")
    public ResponseEntity<?> listSessions(
            @RequestParam UUID tenantId, @RequestParam(required = false) UUID userId) {
        return control.listSessions(tenantId, userId).fold(ResponseEntity::ok, IamControllerSupport::problem);
    }

    @PostMapping("/sesiones/{sessionId}/revocacion")
    @PreAuthorize("hasAuthority('seguridad.sesiones.revocar')")
    public ResponseEntity<?> revokeSession(
            @PathVariable UUID sessionId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody(required = false) RevocarSesionRequest request) {
        var reason = request == null ? null : request.reason();
        return control.revokeSession(tenantId, sessionId, reason).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @GetMapping("/dispositivos")
    @PreAuthorize("hasAuthority('seguridad.dispositivos.consultar')")
    public ResponseEntity<?> listDevices(@RequestParam UUID tenantId) {
        return control.listDevices(tenantId).fold(ResponseEntity::ok, IamControllerSupport::problem);
    }

    @PostMapping("/dispositivos")
    @PreAuthorize("hasAuthority('seguridad.dispositivos.gestionar')")
    public ResponseEntity<?> registerDevice(@Valid @RequestBody RegistrarDispositivoRequest request) {
        var data = new RegisterDeviceData(request.tenantId(), request.companyId(), request.establishmentId(),
                request.terminalId(), request.fingerprintHash(), request.certificateThumbprint(),
                request.agentVersion());
        return control.registerDevice(data).fold(
                result -> ResponseEntity.created(URI.create("/api/v1/dispositivos/" + result.id())).body(result),
                IamControllerSupport::problem);
    }

    @PatchMapping("/dispositivos/{deviceId}/estado")
    @PreAuthorize("hasAuthority('seguridad.dispositivos.gestionar')")
    public ResponseEntity<?> changeDeviceStatus(
            @PathVariable UUID deviceId,
            @RequestParam UUID tenantId,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return control.changeDeviceStatus(tenantId, deviceId, request.status()).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }
}
