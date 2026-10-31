package com.softprimesolutions.security.api.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.softprimesolutions.security.api.dto.request.CrearRolRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IamApiMapperCrearRolTest {

    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");

    @Test
    void mapsCrearRolRequestToCommand() {
        var request = new CrearRolRequest(
                TENANT_ID, "CAJERO_X", "Cajero X", "Descripción", "ESTABLECIMIENTO", false);

        var command = IamApiMapper.toCommand(request);

        assertEquals(TENANT_ID, command.tenantId());
        assertEquals("CAJERO_X", command.code());
        assertEquals("Cajero X", command.name());
        assertEquals("Descripción", command.description());
        assertEquals("ESTABLECIMIENTO", command.roleType());
    }

    @Test
    void ignoresTheSystemRoleFlagSentByTheClient() {
        var request = new CrearRolRequest(TENANT_ID, "CAJERO_X", "Cajero X", null, "ESTABLECIMIENTO", true);

        assertFalse(IamApiMapper.toCommand(request).systemRole());
    }
}
