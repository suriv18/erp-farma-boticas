package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.port.in.SecurityControlUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.application.port.out.SecurityControlPort;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.AssignmentView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.DeviceView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.ExternalIdentityData;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.ExternalIdentityView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.ModuleView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.RegisterDeviceData;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.SessionView;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class SecurityControlService implements SecurityControlUseCase {

    private static final Set<String> USER_STATUSES = Set.of("ACTIVO", "INACTIVO", "BLOQUEADO", "SUSPENDIDO");
    private static final Set<String> ROLE_STATUSES = Set.of("ACTIVO", "INACTIVO");
    private static final Set<String> DEVICE_STATUSES = Set.of("PENDIENTE", "CONFIABLE", "BLOQUEADO", "REVOCADO");

    private final SecurityControlPort controlPort;
    private final IamWritePort iamPort;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public SecurityControlService(
            SecurityControlPort controlPort,
            IamWritePort iamPort,
            IdentifierGenerator identifiers,
            ClockPort clock) {
        this.controlPort = Objects.requireNonNull(controlPort, "controlPort es obligatorio");
        this.iamPort = Objects.requireNonNull(iamPort, "iamPort es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<Unit, ApplicationError> changeUserStatus(UUID tenantId, UUID userId, String status) {
        var normalized = normalizeStatus(status);
        if (!USER_STATUSES.contains(normalized)) return invalidStatus("usuario", USER_STATUSES);
        return controlPort.updateUserStatus(tenantId, userId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("SEC_USUARIO_NO_ENCONTRADO", "El usuario no existe en el tenant indicado.");
    }

    @Override
    public Result<Unit, ApplicationError> changeRoleStatus(UUID tenantId, UUID roleId, String status) {
        var normalized = normalizeStatus(status);
        if (!ROLE_STATUSES.contains(normalized)) return invalidStatus("rol", ROLE_STATUSES);
        return controlPort.updateRoleStatus(tenantId, roleId, normalized, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("SEC_ROL_NO_ENCONTRADO", "El rol no existe en el tenant indicado.");
    }

    @Override
    public Result<List<ExternalIdentityView>, ApplicationError> listExternalIdentities(UUID tenantId, UUID userId) {
        if (!iamPort.userBelongsToTenant(userId, tenantId)) {
            return notFound("SEC_USUARIO_NO_ENCONTRADO", "El usuario no existe en el tenant indicado.");
        }
        return Result.success(controlPort.findExternalIdentities(tenantId, userId));
    }

    @Override
    public Result<ExternalIdentityView, ApplicationError> linkExternalIdentity(
            UUID tenantId, UUID userId, ExternalIdentityData data) {
        if (!iamPort.userBelongsToTenant(userId, tenantId)) {
            return notFound("SEC_USUARIO_NO_ENCONTRADO", "El usuario no existe en el tenant indicado.");
        }
        if (data == null || blankOrExceeds(data.provider(), 100) || blankOrExceeds(data.subject(), 300)
                || exceeds(data.issuer(), 500) || exceeds(data.emailClaim(), 254)) {
            return invalid("identity", "Los datos de la identidad externa no son validos.");
        }
        var normalized = new ExternalIdentityData(data.provider().trim(), data.subject().trim(),
                trimToNull(data.issuer()), trimToNull(data.emailClaim()));
        if (!controlPort.createExternalIdentity(tenantId, userId, normalized, clock.now())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_IDENTIDAD_DUPLICADA", "La identidad externa ya se encuentra vinculada.",
                    ErrorCategory.CONFLICT));
        }
        return controlPort.findExternalIdentities(tenantId, userId).stream()
                .filter(identity -> identity.provider().equals(normalized.provider())
                        && identity.subject().equals(normalized.subject()))
                .findFirst().<Result<ExternalIdentityView, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "SEC_IDENTIDAD_NO_ENCONTRADA", "No fue posible recuperar la identidad vinculada.",
                        ErrorCategory.UNEXPECTED)));
    }

    @Override
    public Result<Unit, ApplicationError> unlinkExternalIdentity(
            UUID tenantId, UUID userId, String provider, String subject) {
        if (blankOrExceeds(provider, 100) || blankOrExceeds(subject, 300)) {
            return invalid("identity", "Proveedor y subject son obligatorios.");
        }
        var identities = controlPort.findExternalIdentities(tenantId, userId);
        var exists = identities.stream().anyMatch(identity -> identity.provider().equals(provider)
                && identity.subject().equals(subject));
        if (!exists) {
            return notFound("SEC_IDENTIDAD_NO_ENCONTRADA", "La identidad externa no existe para el usuario.");
        }
        if (identities.size() == 1) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ULTIMA_IDENTIDAD", "No se puede desvincular la ultima identidad del usuario.",
                    ErrorCategory.CONFLICT));
        }
        return controlPort.deleteExternalIdentity(tenantId, userId, provider, subject)
                ? Result.success(Unit.INSTANCE)
                : notFound("SEC_IDENTIDAD_NO_ENCONTRADA", "La identidad externa no existe para el usuario.");
    }

    @Override
    public Result<List<AssignmentView>, ApplicationError> listAssignments(UUID tenantId, UUID userId) {
        if (!iamPort.userBelongsToTenant(userId, tenantId)) {
            return notFound("SEC_USUARIO_NO_ENCONTRADO", "El usuario no existe en el tenant indicado.");
        }
        return Result.success(controlPort.findAssignments(tenantId, userId));
    }

    @Override
    public Result<Unit, ApplicationError> revokeAssignment(UUID tenantId, UUID userId, UUID assignmentId) {
        return controlPort.revokeAssignment(tenantId, userId, assignmentId)
                ? Result.success(Unit.INSTANCE)
                : notFound("SEC_ASIGNACION_NO_ENCONTRADA", "La asignacion activa no existe.");
    }

    @Override
    public Result<Set<String>, ApplicationError> listEffectivePermissions(UUID tenantId, UUID userId) {
        if (!iamPort.userBelongsToTenant(userId, tenantId)) {
            return notFound("SEC_USUARIO_NO_ENCONTRADO", "El usuario no existe en el tenant indicado.");
        }
        return Result.success(controlPort.findEffectivePermissions(tenantId, userId, clock.now()));
    }

    @Override
    public Result<List<ModuleView>, ApplicationError> listModules() {
        return Result.success(controlPort.findModules());
    }

    @Override
    public Result<List<SessionView>, ApplicationError> listSessions(UUID tenantId, UUID userId) {
        if (userId != null && !iamPort.userBelongsToTenant(userId, tenantId)) {
            return notFound("SEC_USUARIO_NO_ENCONTRADO", "El usuario no existe en el tenant indicado.");
        }
        return Result.success(controlPort.findSessions(tenantId, userId));
    }

    @Override
    public Result<Unit, ApplicationError> revokeSession(UUID tenantId, UUID sessionId, String reason) {
        var normalizedReason = reason == null ? null : reason.trim();
        if (normalizedReason != null && normalizedReason.length() > 500) {
            return invalid("reason", "El motivo no debe exceder 500 caracteres.");
        }
        return controlPort.revokeSession(tenantId, sessionId, normalizedReason, clock.now())
                ? Result.success(Unit.INSTANCE)
                : notFound("SEC_SESION_NO_ENCONTRADA", "La sesion activa no existe.");
    }

    @Override
    public Result<List<DeviceView>, ApplicationError> listDevices(UUID tenantId) {
        if (!iamPort.tenantExists(tenantId)) {
            return notFound("SEC_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.");
        }
        return Result.success(controlPort.findDevices(tenantId));
    }

    @Override
    public Result<DeviceView, ApplicationError> registerDevice(RegisterDeviceData data) {
        if (data == null || data.tenantId() == null || data.companyId() == null || data.establishmentId() == null) {
            return invalid("scope", "Tenant, empresa y establecimiento son obligatorios.");
        }
        if (exceeds(data.fingerprintHash(), 300)
                || exceeds(data.certificateThumbprint(), 300)
                || exceeds(data.agentVersion(), 100)) {
            return invalid("device", "Los datos de identificacion del dispositivo exceden el largo permitido.");
        }
        var deviceId = identifiers.next();
        if (!controlPort.createDevice(data, deviceId, clock.now())) {
            return notFound("SEC_AMBITO_NO_ENCONTRADO", "El ambito del dispositivo no existe en el tenant.");
        }
        return controlPort.findDevices(data.tenantId()).stream()
                .filter(device -> device.id().equals(deviceId))
                .findFirst()
                .<Result<DeviceView, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "SEC_DISPOSITIVO_NO_ENCONTRADO", "No fue posible recuperar el dispositivo.",
                        ErrorCategory.UNEXPECTED)));
    }

    @Override
    public Result<Unit, ApplicationError> changeDeviceStatus(UUID tenantId, UUID deviceId, String status) {
        var normalized = normalizeStatus(status);
        if (!DEVICE_STATUSES.contains(normalized)) return invalidStatus("dispositivo", DEVICE_STATUSES);
        return controlPort.updateDeviceStatus(tenantId, deviceId, normalized)
                ? Result.success(Unit.INSTANCE)
                : notFound("SEC_DISPOSITIVO_NO_ENCONTRADO", "El dispositivo no existe en el tenant.");
    }

    private static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean exceeds(String value, int maximum) {
        return value != null && value.trim().length() > maximum;
    }

    private static boolean blankOrExceeds(String value, int maximum) {
        return value == null || value.isBlank() || exceeds(value, maximum);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> Result<T, ApplicationError> invalidStatus(String resource, Set<String> allowed) {
        return Result.failure(new StandardApplicationError(
                "SEC_ESTADO_INVALIDO", "El estado de " + resource + " no es valido.",
                ErrorCategory.VALIDATION, Map.of("allowed", allowed)));
    }

    private static <T> Result<T, ApplicationError> invalid(String field, String message) {
        return Result.failure(new StandardApplicationError(
                "SEC_SOLICITUD_INVALIDA", message, ErrorCategory.VALIDATION, Map.of("field", field)));
    }

    private static <T> Result<T, ApplicationError> notFound(String code, String message) {
        return Result.failure(new StandardApplicationError(code, message, ErrorCategory.NOT_FOUND));
    }
}
