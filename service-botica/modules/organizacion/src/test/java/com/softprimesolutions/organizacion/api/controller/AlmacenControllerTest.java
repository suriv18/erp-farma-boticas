package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ALMACEN;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.JWT;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarAlmacenRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.almacenResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearAlmacenRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.page;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertCreated;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import org.junit.jupiter.api.Test;

class AlmacenControllerTest {

    private final AlmacenController succeeding = new AlmacenController(
            command -> ok(almacenResult()), command -> ok(almacenResult()),
            query -> ok(page(almacenResult())), query -> ok(almacenResult()));
    private final AlmacenController failing = new AlmacenController(
            command -> conflict(), command -> conflict(), query -> conflict(), query -> conflict());

    @Test
    void createsAnAlmacenAndReturnsItsLocation() {
        assertCreated(succeeding.create(JWT, crearAlmacenRequest()), AlmacenController.BASE_PATH, ALMACEN,
                OrganizacionApiMapper.toResponse(almacenResult()));
    }

    @Test
    void mapsCreateFailuresToProblemDetails() {
        assertConflict(failing.create(JWT, crearAlmacenRequest()));
    }

    @Test
    void listsAlmacenesAsAPage() {
        assertOk(succeeding.list(JWT, ESTABLECIMIENTO, "alm", 0, 20),
                OrganizacionApiMapper.toAlmacenPage(page(almacenResult())));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertConflict(failing.list(JWT, null, null, 0, 20));
    }

    @Test
    void getsAnAlmacenById() {
        assertOk(succeeding.getById(ALMACEN, JWT), OrganizacionApiMapper.toResponse(almacenResult()));
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertConflict(failing.getById(ALMACEN, JWT));
    }

    @Test
    void updatesAnAlmacen() {
        assertOk(succeeding.update(ALMACEN, JWT, actualizarAlmacenRequest()),
                OrganizacionApiMapper.toResponse(almacenResult()));
    }

    @Test
    void mapsUpdateFailuresToProblemDetails() {
        assertConflict(failing.update(ALMACEN, JWT, actualizarAlmacenRequest()));
    }
}
