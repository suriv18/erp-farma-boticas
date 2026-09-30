package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.EMPRESA;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarEstablecimientoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.cambiarEstadoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearEstablecimientoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.establecimientoResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.page;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertCreated;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import org.junit.jupiter.api.Test;

class EstablecimientoControllerTest {

    private final EstablecimientoController succeeding = new EstablecimientoController(
            command -> ok(establecimientoResult()), command -> ok(establecimientoResult()),
            query -> ok(page(establecimientoResult())), query -> ok(establecimientoResult()),
            command -> ok(establecimientoResult()));
    private final EstablecimientoController failing = new EstablecimientoController(
            command -> conflict(), command -> conflict(), query -> conflict(), query -> conflict(),
            command -> conflict());

    @Test
    void createsAnEstablecimientoAndReturnsItsLocation() {
        assertCreated(succeeding.create(crearEstablecimientoRequest()), EstablecimientoController.BASE_PATH,
                ESTABLECIMIENTO, OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsCreateFailuresToProblemDetails() {
        assertConflict(failing.create(crearEstablecimientoRequest()));
    }

    @Test
    void listsEstablecimientosAsAPage() {
        assertOk(succeeding.list(TENANT, EMPRESA, "cen", 0, 20),
                OrganizacionApiMapper.toEstablecimientoPage(page(establecimientoResult())));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertConflict(failing.list(TENANT, null, null, 0, 20));
    }

    @Test
    void getsAnEstablecimientoById() {
        assertOk(succeeding.getById(ESTABLECIMIENTO, TENANT),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertConflict(failing.getById(ESTABLECIMIENTO, TENANT));
    }

    @Test
    void updatesAnEstablecimiento() {
        assertOk(succeeding.update(ESTABLECIMIENTO, TENANT, actualizarEstablecimientoRequest()),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsUpdateFailuresToProblemDetails() {
        assertConflict(failing.update(ESTABLECIMIENTO, TENANT, actualizarEstablecimientoRequest()));
    }

    @Test
    void changesTheStatusOfAnEstablecimiento() {
        assertOk(succeeding.changeStatus(ESTABLECIMIENTO, TENANT, cambiarEstadoRequest("CLAUSURADO")),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsStatusChangeFailuresToProblemDetails() {
        assertConflict(failing.changeStatus(ESTABLECIMIENTO, TENANT, cambiarEstadoRequest("CLAUSURADO")));
    }
}
