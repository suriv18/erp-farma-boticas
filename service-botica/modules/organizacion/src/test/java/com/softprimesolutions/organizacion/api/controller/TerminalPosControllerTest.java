package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TERMINAL;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarTerminalRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearTerminalRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.page;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.terminalResult;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertCreated;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import org.junit.jupiter.api.Test;

class TerminalPosControllerTest {

    private final TerminalPosController succeeding = new TerminalPosController(
            command -> ok(terminalResult()), command -> ok(terminalResult()),
            query -> ok(page(terminalResult())), query -> ok(terminalResult()));
    private final TerminalPosController failing = new TerminalPosController(
            command -> conflict(), command -> conflict(), query -> conflict(), query -> conflict());

    @Test
    void createsATerminalAndReturnsItsLocation() {
        assertCreated(succeeding.create(crearTerminalRequest()), TerminalPosController.BASE_PATH, TERMINAL,
                OrganizacionApiMapper.toResponse(terminalResult()));
    }

    @Test
    void mapsCreateFailuresToProblemDetails() {
        assertConflict(failing.create(crearTerminalRequest()));
    }

    @Test
    void listsTerminalesAsAPage() {
        assertOk(succeeding.list(TENANT, ESTABLECIMIENTO, "caj", 0, 20),
                OrganizacionApiMapper.toTerminalPage(page(terminalResult())));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertConflict(failing.list(TENANT, null, null, 0, 20));
    }

    @Test
    void getsATerminalById() {
        assertOk(succeeding.getById(TERMINAL, TENANT), OrganizacionApiMapper.toResponse(terminalResult()));
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertConflict(failing.getById(TERMINAL, TENANT));
    }

    @Test
    void updatesATerminal() {
        assertOk(succeeding.update(TERMINAL, TENANT, actualizarTerminalRequest()),
                OrganizacionApiMapper.toResponse(terminalResult()));
    }

    @Test
    void mapsUpdateFailuresToProblemDetails() {
        assertConflict(failing.update(TERMINAL, TENANT, actualizarTerminalRequest()));
    }
}
