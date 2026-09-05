package com.softprimesolutions.security.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SecurityControlPort {

    boolean updateUserStatus(UUID tenantId, UUID userId, String status, Instant changedAt);

    boolean updateRoleStatus(UUID tenantId, UUID roleId, String status, Instant changedAt);

    List<ExternalIdentityView> findExternalIdentities(UUID tenantId, UUID userId);

    boolean createExternalIdentity(UUID tenantId, UUID userId, ExternalIdentityData data, Instant createdAt);

    boolean deleteExternalIdentity(UUID tenantId, UUID userId, String provider, String subject);

    List<AssignmentView> findAssignments(UUID tenantId, UUID userId);

    boolean revokeAssignment(UUID tenantId, UUID userId, UUID assignmentId);

    Set<String> findEffectivePermissions(UUID tenantId, UUID userId, Instant at);

    List<ModuleView> findModules();

    List<SessionView> findSessions(UUID tenantId, UUID userId);

    boolean revokeSession(UUID tenantId, UUID sessionId, String reason, Instant revokedAt);

    List<DeviceView> findDevices(UUID tenantId);

    boolean createDevice(RegisterDeviceData data, UUID deviceId, Instant registeredAt);

    boolean updateDeviceStatus(UUID tenantId, UUID deviceId, String status);

    record AssignmentView(
            UUID id, UUID roleId, String roleCode, String roleName, String scopeType,
            UUID companyId, UUID establishmentId, UUID warehouseId, UUID terminalId,
            Instant validFrom, Instant validUntil, String status, String createdBy, Instant createdAt) {
    }

    record ExternalIdentityView(
            String provider, String subject, String issuer, String emailClaim,
            Instant lastLoginAt, Instant createdAt) {
    }

    record ExternalIdentityData(String provider, String subject, String issuer, String emailClaim) {
    }

    record ModuleView(String code, String name, String description, int order, boolean active) {
    }

    record SessionView(
            UUID id, UUID userId, String provider, String authMethod, String channel,
            String ipAddress, String userAgent, UUID deviceId, Instant loginAt,
            Instant lastUsedAt, Instant expiresAt, Instant logoutAt, Instant revokedAt,
            String revocationReason, String status) {
    }

    record DeviceView(
            UUID id, UUID companyId, UUID establishmentId, UUID terminalId,
            String fingerprintHash, String certificateThumbprint, String agentVersion,
            String status, Instant registeredAt, Instant lastContactAt) {
    }

    record RegisterDeviceData(
            UUID tenantId, UUID companyId, UUID establishmentId, UUID terminalId,
            String fingerprintHash, String certificateThumbprint, String agentVersion) {
    }
}
