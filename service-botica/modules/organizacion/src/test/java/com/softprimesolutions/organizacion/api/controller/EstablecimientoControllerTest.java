package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.EMPRESA;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.JWT;
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
        assertCreated(succeeding.create(JWT, crearEstablecimientoRequest()), EstablecimientoController.BASE_PATH,
                ESTABLECIMIENTO, OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsCreateFailuresToProblemDetails() {
        assertConflict(failing.create(JWT, crearEstablecimientoRequest()));
    }

    @Test
    void listsEstablecimientosAsAPage() {
        assertOk(succeeding.list(JWT, EMPRESA, "cen", 0, 20),
                OrganizacionApiMapper.toEstablecimientoPage(page(establecimientoResult())));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertConflict(failing.list(JWT, null, null, 0, 20));
    }

    @Test
    void getsAnEstablecimientoById() {
        assertOk(succeeding.getById(ESTABLECIMIENTO, JWT),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertConflict(failing.getById(ESTABLECIMIENTO, JWT));
    }

    @Test
    void updatesAnEstablecimiento() {
        assertOk(succeeding.update(ESTABLECIMIENTO, JWT, actualizarEstablecimientoRequest()),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsUpdateFailuresToProblemDetails() {
        assertConflict(failing.update(ESTABLECIMIENTO, JWT, actualizarEstablecimientoRequest()));
    }

    @Test
    void changesTheStatusOfAnEstablecimiento() {
        assertOk(succeeding.changeStatus(ESTABLECIMIENTO, JWT, cambiarEstadoRequest("CLAUSURADO")),
                OrganizacionApiMapper.toResponse(establecimientoResult()));
    }

    @Test
    void mapsStatusChangeFailuresToProblemDetails() {
        assertConflict(failing.changeStatus(ESTABLECIMIENTO, JWT, cambiarEstadoRequest("CLAUSURADO")));
    }
}
