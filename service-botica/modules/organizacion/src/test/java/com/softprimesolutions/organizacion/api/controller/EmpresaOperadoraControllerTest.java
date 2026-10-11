package com.softprimesolutions.organizacion.api.controller;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.EMPRESA;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.JWT;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarEmpresaRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.cambiarEstadoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearEmpresaRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.empresaResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.page;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertConflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertCreated;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.assertOk;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.conflict;
import static com.softprimesolutions.organizacion.api.ResponseAssertions.ok;

import com.softprimesolutions.organizacion.api.mapper.OrganizacionApiMapper;
import org.junit.jupiter.api.Test;

class EmpresaOperadoraControllerTest {

    private final EmpresaOperadoraController succeeding = new EmpresaOperadoraController(
            command -> ok(empresaResult()), command -> ok(empresaResult()),
            query -> ok(page(empresaResult())), query -> ok(empresaResult()),
            command -> ok(empresaResult()));
    private final EmpresaOperadoraController failing = new EmpresaOperadoraController(
            command -> conflict(), command -> conflict(), query -> conflict(), query -> conflict(),
            command -> conflict());

    @Test
    void createsAnEmpresaAndReturnsItsLocation() {
        assertCreated(succeeding.create(JWT, crearEmpresaRequest()), EmpresaOperadoraController.BASE_PATH, EMPRESA,
                OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsCreateFailuresToProblemDetails() {
        assertConflict(failing.create(JWT, crearEmpresaRequest()));
    }

    @Test
    void listsEmpresasAsAPage() {
        assertOk(succeeding.list(JWT, "bot", 1, 10), OrganizacionApiMapper.toEmpresaPage(page(empresaResult())));
    }

    @Test
    void mapsListFailuresToProblemDetails() {
        assertConflict(failing.list(JWT, null, 0, 20));
    }

    @Test
    void getsAnEmpresaById() {
        assertOk(succeeding.getById(EMPRESA, JWT), OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsGetFailuresToProblemDetails() {
        assertConflict(failing.getById(EMPRESA, JWT));
    }

    @Test
    void updatesAnEmpresa() {
        assertOk(succeeding.update(EMPRESA, JWT, actualizarEmpresaRequest()),
                OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsUpdateFailuresToProblemDetails() {
        assertConflict(failing.update(EMPRESA, JWT, actualizarEmpresaRequest()));
    }

    @Test
    void changesTheStatusOfAnEmpresa() {
        assertOk(succeeding.changeStatus(EMPRESA, JWT, cambiarEstadoRequest("SUSPENDIDO")),
                OrganizacionApiMapper.toResponse(empresaResult()));
    }

    @Test
    void mapsStatusChangeFailuresToProblemDetails() {
        assertConflict(failing.changeStatus(EMPRESA, JWT, cambiarEstadoRequest("SUSPENDIDO")));
    }
}
