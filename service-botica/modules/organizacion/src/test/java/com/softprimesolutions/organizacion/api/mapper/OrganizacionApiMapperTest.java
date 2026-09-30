package com.softprimesolutions.organizacion.api.mapper;

import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ALMACEN;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.EMPRESA;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TENANT;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.TERMINAL;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarAlmacenRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarEmpresaRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarEstablecimientoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.actualizarTerminalRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.almacenResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.cambiarEstadoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearAlmacenRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearEmpresaRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearEstablecimientoRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.crearTerminalRequest;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.empresaResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.establecimientoResult;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.page;
import static com.softprimesolutions.organizacion.api.OrganizacionApiFixtures.terminalResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.dto.result.NodoResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizacionApiMapperTest {

    @Test
    void mapsEmpresaRequestsToCommandsAndResultsToResponses() {
        var create = OrganizacionApiMapper.toCommand(crearEmpresaRequest());
        var update = OrganizacionApiMapper.toCommand(EMPRESA, TENANT, actualizarEmpresaRequest());

        assertThat(create).usingRecursiveComparison().isEqualTo(crearEmpresaRequest());
        assertThat(update).usingRecursiveComparison().ignoringFields("empresaId", "tenantId")
                .isEqualTo(actualizarEmpresaRequest());
        assertThat(update.empresaId()).isEqualTo(EMPRESA);
        assertThat(update.tenantId()).isEqualTo(TENANT);
        assertThat(OrganizacionApiMapper.toResponse(empresaResult()))
                .usingRecursiveComparison().isEqualTo(empresaResult());
        var pageResponse = OrganizacionApiMapper.toEmpresaPage(page(empresaResult()));
        assertThat(pageResponse.items()).singleElement().usingRecursiveComparison().isEqualTo(empresaResult());
        assertThat(pageResponse.page()).isEqualTo(1);
        assertThat(pageResponse.size()).isEqualTo(10);
        assertThat(pageResponse.totalElements()).isEqualTo(11);
    }

    @Test
    void mapsEstablecimientoRequestsToCommandsAndResultsToResponses() {
        var create = OrganizacionApiMapper.toCommand(crearEstablecimientoRequest());
        var update = OrganizacionApiMapper.toCommand(ESTABLECIMIENTO, TENANT, actualizarEstablecimientoRequest());

        assertThat(create).usingRecursiveComparison().isEqualTo(crearEstablecimientoRequest());
        assertThat(update).usingRecursiveComparison().ignoringFields("establecimientoId", "tenantId")
                .isEqualTo(actualizarEstablecimientoRequest());
        assertThat(update.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(OrganizacionApiMapper.toResponse(establecimientoResult()))
                .usingRecursiveComparison().isEqualTo(establecimientoResult());
        assertThat(OrganizacionApiMapper.toEstablecimientoPage(page(establecimientoResult())).items())
                .singleElement().usingRecursiveComparison().isEqualTo(establecimientoResult());
    }

    @Test
    void mapsAlmacenRequestsToCommandsAndResultsToResponses() {
        var create = OrganizacionApiMapper.toCommand(crearAlmacenRequest());
        var update = OrganizacionApiMapper.toCommand(ALMACEN, TENANT, actualizarAlmacenRequest());

        assertThat(create).usingRecursiveComparison().isEqualTo(crearAlmacenRequest());
        assertThat(update).usingRecursiveComparison().ignoringFields("almacenId", "tenantId")
                .isEqualTo(actualizarAlmacenRequest());
        assertThat(update.almacenId()).isEqualTo(ALMACEN);
        assertThat(OrganizacionApiMapper.toResponse(almacenResult()))
                .usingRecursiveComparison().isEqualTo(almacenResult());
        assertThat(OrganizacionApiMapper.toAlmacenPage(page(almacenResult())).items())
                .singleElement().usingRecursiveComparison().isEqualTo(almacenResult());
    }

    @Test
    void mapsTerminalRequestsToCommandsAndResultsToResponses() {
        var create = OrganizacionApiMapper.toCommand(crearTerminalRequest());
        var update = OrganizacionApiMapper.toCommand(TERMINAL, TENANT, actualizarTerminalRequest());

        assertThat(create).usingRecursiveComparison().isEqualTo(crearTerminalRequest());
        assertThat(update).usingRecursiveComparison().ignoringFields("terminalId", "tenantId")
                .isEqualTo(actualizarTerminalRequest());
        assertThat(update.terminalId()).isEqualTo(TERMINAL);
        assertThat(OrganizacionApiMapper.toResponse(terminalResult()))
                .usingRecursiveComparison().isEqualTo(terminalResult());
        assertThat(OrganizacionApiMapper.toTerminalPage(page(terminalResult())).items())
                .singleElement().usingRecursiveComparison().isEqualTo(terminalResult());
    }

    @Test
    void mapsTheCorporateStructureKeepingTheFrontendFieldNames() {
        var warehouse = new NodoResult(UUID.randomUUID(), "ALM", "Almacén", "ACTIVE");
        var cashRegister = new NodoResult(UUID.randomUUID(), "POS", "Caja", "INACTIVE");
        var establishment = new EstablecimientoNodoResult(
                UUID.randomUUID(), "EST", "Botica", "SUSPENDED", "America/Lima", List.of(warehouse),
                List.of(cashRegister));
        var company = new EmpresaNodoResult(UUID.randomUUID(), "Boticas SAC", null, "ACTIVE", List.of(establishment));
        var structure = new EstructuraCorporativaResult(Instant.parse("2026-01-01T00:00:00Z"), List.of(company));

        var response = OrganizacionApiMapper.toResponse(structure);

        assertThat(response).usingRecursiveComparison().isEqualTo(structure);
        assertThat(response.companies().get(0).establishments().get(0).cashRegisters()).hasSize(1);
    }

    @Test
    void mapsTheStatusChangeRequestToTheCommandOfEachEntity() {
        var empresa = OrganizacionApiMapper.toCambiarEstadoEmpresaCommand(
                EMPRESA, TENANT, cambiarEstadoRequest("SUSPENDIDO"));
        var establecimiento = OrganizacionApiMapper.toCambiarEstadoEstablecimientoCommand(
                ESTABLECIMIENTO, TENANT, cambiarEstadoRequest("CLAUSURADO"));

        assertThat(empresa).isEqualTo(new CambiarEstadoEmpresaCommand(EMPRESA, TENANT, "SUSPENDIDO"));
        assertThat(establecimiento)
                .isEqualTo(new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO, TENANT, "CLAUSURADO"));
    }
}
