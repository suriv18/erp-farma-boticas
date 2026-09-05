package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.port.out.SecurityControlPort.AssignmentView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.DeviceView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.ExternalIdentityData;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.ExternalIdentityView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.ModuleView;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.RegisterDeviceData;
import com.softprimesolutions.security.application.port.out.SecurityControlPort.SessionView;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SecurityControlUseCase {

    Result<Unit, ApplicationError> changeUserStatus(UUID tenantId, UUID userId, String status);

    Result<Unit, ApplicationError> changeRoleStatus(UUID tenantId, UUID roleId, String status);

    Result<List<ExternalIdentityView>, ApplicationError> listExternalIdentities(UUID tenantId, UUID userId);

    Result<ExternalIdentityView, ApplicationError> linkExternalIdentity(
            UUID tenantId, UUID userId, ExternalIdentityData data);

    Result<Unit, ApplicationError> unlinkExternalIdentity(
            UUID tenantId, UUID userId, String provider, String subject);

    Result<List<AssignmentView>, ApplicationError> listAssignments(UUID tenantId, UUID userId);

    Result<Unit, ApplicationError> revokeAssignment(UUID tenantId, UUID userId, UUID assignmentId);

    Result<Set<String>, ApplicationError> listEffectivePermissions(UUID tenantId, UUID userId);

    Result<List<ModuleView>, ApplicationError> listModules();

    Result<List<SessionView>, ApplicationError> listSessions(UUID tenantId, UUID userId);

    Result<Unit, ApplicationError> revokeSession(UUID tenantId, UUID sessionId, String reason);

    Result<List<DeviceView>, ApplicationError> listDevices(UUID tenantId);

    Result<DeviceView, ApplicationError> registerDevice(RegisterDeviceData data);

    Result<Unit, ApplicationError> changeDeviceStatus(UUID tenantId, UUID deviceId, String status);
}
