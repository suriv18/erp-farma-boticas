package com.softprimesolutions.security.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softprimesolutions.security.api.dto.request.ActualizarRolRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IamApiMapperActualizarRolTest {

    @Test
    void mapsActualizarRolRequestToCommand() {
        var roleId = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
        var tenantId = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
        var request = new ActualizarRolRequest("ADMIN_LOCAL", "Administrador local", "Descripción", "ESTABLECIMIENTO");

        var command = IamApiMapper.toCommand(roleId, tenantId, request);

        assertEquals(roleId, command.roleId());
        assertEquals(tenantId, command.tenantId());
        assertEquals("ADMIN_LOCAL", command.code());
        assertEquals("Administrador local", command.name());
        assertEquals("Descripción", command.description());
        assertEquals("ESTABLECIMIENTO", command.roleType());
    }
}
