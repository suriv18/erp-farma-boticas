package com.softprimesolutions.security.infrastructure.persistence.read.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.softprimesolutions.security.application.dto.result.PerfilResult;
import com.softprimesolutions.security.infrastructure.persistence.read.projection.PerfilProjection;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IamReadMapperPerfilTest {

    private static final UUID USER_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");

    @Test
    void mapsTheProfileProjectionWithItsRoleAssignments() {
        var projection = new PerfilProjection(
                USER_ID, TENANT_ID, "Luis Paredes Soto", "crit001.manana1", "crit001.manana1@boticas.pe",
                List.of(
                        new PerfilProjection.Asignacion(
                                "CAJERO", "Cajero de establecimiento", "ESTABLECIMIENTO", "Botica Crítica Norte"),
                        new PerfilProjection.Asignacion("ADMIN", "Administrador global", "GLOBAL", null)));

        var result = IamReadMapper.toResult(projection);

        assertEquals(
                new PerfilResult(
                        USER_ID, TENANT_ID, "Luis Paredes Soto", "crit001.manana1", "crit001.manana1@boticas.pe",
                        List.of(
                                new PerfilResult.Asignacion(
                                        "CAJERO", "Cajero de establecimiento", "ESTABLECIMIENTO",
                                        "Botica Crítica Norte"),
                                new PerfilResult.Asignacion("ADMIN", "Administrador global", "GLOBAL", null))),
                result);
    }

    @Test
    void mapsAProfileWithoutAssignments() {
        var result = IamReadMapper.toResult(
                new PerfilProjection(USER_ID, TENANT_ID, "Luis", "luis", "luis@boticas.pe", List.of()));

        assertEquals(List.of(), result.assignments());
    }
}
