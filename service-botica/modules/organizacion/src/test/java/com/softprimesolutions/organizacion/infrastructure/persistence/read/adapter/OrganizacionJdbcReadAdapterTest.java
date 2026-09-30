package com.softprimesolutions.organizacion.infrastructure.persistence.read.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.result.EmpresaNodoResult;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EmpresaProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.repository.OrganizacionJdbcReadRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrganizacionJdbcReadAdapterTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final OrganizacionJdbcReadRepository repository = mock(OrganizacionJdbcReadRepository.class);
    private final OrganizacionJdbcReadAdapter adapter = new OrganizacionJdbcReadAdapter(repository);

    private static EmpresaProjection empresa(UUID id, String razonSocial) {
        return empresa(id, razonSocial, "ACTIVO");
    }

    private static EmpresaProjection empresa(UUID id, String razonSocial, String estado) {
        return new EmpresaProjection(
                id, TENANT, "20123456789", razonSocial, "Comercial", "Av. 1", "150101", "01", "a@b.pe",
                "https://b.pe", "PEN", "America/Lima", false, estado, NOW, null);
    }

    private static EstablecimientoProjection establecimiento(UUID id, UUID empresaId, String nombre) {
        return establecimiento(id, empresaId, nombre, "ACTIVO");
    }

    private static EstablecimientoProjection establecimiento(
            UUID id, UUID empresaId, String nombre, String estadoOperativo) {
        return new EstablecimientoProjection(
                id, TENANT, empresaId, "COD-" + nombre, nombre, "BOTICA", null, "0001", null, "Av. 2", "150101",
                null, BigDecimal.ONE, BigDecimal.TEN, "01", "e@b.pe", true, false, false, "ONLINE",
                "America/Lima", estadoOperativo, NOW, NOW);
    }

    private static AlmacenProjection almacen(UUID id, UUID establecimientoId, String nombre, boolean activo) {
        return new AlmacenProjection(
                id, TENANT, establecimientoId, "ALM-" + nombre, nombre, "GENERAL", true, true, true, true,
                false, null, null, activo, NOW, null);
    }

    private static TerminalProjection terminal(UUID id, UUID establecimientoId, String nombre) {
        return terminal(id, establecimientoId, nombre, "ACTIVO");
    }

    private static TerminalProjection terminal(UUID id, UUID establecimientoId, String nombre, String estado) {
        return new TerminalProjection(
                id, TENANT, establecimientoId, "POS-" + nombre, nombre, "B001", "F001", "SN", "host", "10.0.0.1",
                "IMP", true, estado, NOW, null);
    }

    @Test
    void pagesEmpresasWithTheRepositoryOffsetAndTotal() {
        var id = UUID.randomUUID();
        when(repository.findEmpresas(TENANT, "bot", 20, 10)).thenReturn(List.of(empresa(id, "Boticas SAC")));
        when(repository.countEmpresas(TENANT, "bot")).thenReturn(21L);

        var page = adapter.findEmpresas(TENANT, "bot", 2, 10);

        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(21);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(id);
            assertThat(item.razonSocial()).isEqualTo("Boticas SAC");
        });
    }

    @Test
    void findsEmpresaById() {
        var id = UUID.randomUUID();
        when(repository.findEmpresaById(TENANT, id)).thenReturn(Optional.of(empresa(id, "Boticas SAC")));

        assertThat(adapter.findEmpresaById(TENANT, id)).isPresent();
        assertThat(adapter.findEmpresaById(TENANT, UUID.randomUUID())).isEmpty();
    }

    @Test
    void pagesEstablecimientosScopedToAnEmpresa() {
        var empresaId = UUID.randomUUID();
        var id = UUID.randomUUID();
        when(repository.findEstablecimientos(TENANT, empresaId, "", 0, 5))
                .thenReturn(List.of(establecimiento(id, empresaId, "Central")));
        when(repository.countEstablecimientos(TENANT, empresaId, "")).thenReturn(1L);

        var page = adapter.findEstablecimientos(TENANT, empresaId, "", 0, 5);

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(id));
    }

    @Test
    void findsEstablecimientoById() {
        var id = UUID.randomUUID();
        when(repository.findEstablecimientoById(TENANT, id))
                .thenReturn(Optional.of(establecimiento(id, UUID.randomUUID(), "Central")));

        assertThat(adapter.findEstablecimientoById(TENANT, id)).isPresent();
        assertThat(adapter.findEstablecimientoById(TENANT, UUID.randomUUID())).isEmpty();
    }

    @Test
    void pagesAlmacenesScopedToAnEstablecimiento() {
        var establecimientoId = UUID.randomUUID();
        var id = UUID.randomUUID();
        when(repository.findAlmacenes(TENANT, establecimientoId, "a", 10, 10))
                .thenReturn(List.of(almacen(id, establecimientoId, "Central", true)));
        when(repository.countAlmacenes(TENANT, establecimientoId, "a")).thenReturn(11L);

        var page = adapter.findAlmacenes(TENANT, establecimientoId, "a", 1, 10);

        assertThat(page.totalElements()).isEqualTo(11);
        assertThat(page.items()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(id));
    }

    @Test
    void findsAlmacenById() {
        var id = UUID.randomUUID();
        when(repository.findAlmacenById(TENANT, id))
                .thenReturn(Optional.of(almacen(id, UUID.randomUUID(), "Central", false)));

        assertThat(adapter.findAlmacenById(TENANT, id)).isPresent();
        assertThat(adapter.findAlmacenById(TENANT, UUID.randomUUID())).isEmpty();
    }

    @Test
    void pagesTerminalesScopedToAnEstablecimiento() {
        var establecimientoId = UUID.randomUUID();
        var id = UUID.randomUUID();
        when(repository.findTerminales(TENANT, establecimientoId, "", 0, 10))
                .thenReturn(List.of(terminal(id, establecimientoId, "Caja 1")));
        when(repository.countTerminales(TENANT, establecimientoId, "")).thenReturn(1L);

        var page = adapter.findTerminales(TENANT, establecimientoId, "", 0, 10);

        assertThat(page.items()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(id));
    }

    @Test
    void findsTerminalById() {
        var id = UUID.randomUUID();
        when(repository.findTerminalById(TENANT, id))
                .thenReturn(Optional.of(terminal(id, UUID.randomUUID(), "Caja 1")));

        assertThat(adapter.findTerminalById(TENANT, id)).isPresent();
        assertThat(adapter.findTerminalById(TENANT, UUID.randomUUID())).isEmpty();
    }

    @Test
    void nestsEveryNodeUnderItsOwnParentInTheCorporateStructure() {
        var empresaA = UUID.randomUUID();
        var empresaB = UUID.randomUUID();
        var empresaVacia = UUID.randomUUID();
        var estA1 = UUID.randomUUID();
        var estA2 = UUID.randomUUID();
        var estB1 = UUID.randomUUID();
        var almacenActivo = UUID.randomUUID();
        var almacenInactivo = UUID.randomUUID();
        var almacenDeB = UUID.randomUUID();
        var terminalA1 = UUID.randomUUID();
        var terminalB1 = UUID.randomUUID();
        var terminalBloqueado = UUID.randomUUID();
        when(repository.findAllEmpresasActivas(TENANT)).thenReturn(List.of(
                empresa(empresaA, "A SAC"), empresa(empresaB, "B SAC"), empresa(empresaVacia, "C SAC", "BLOQUEADO")));
        when(repository.findAllEstablecimientosActivos(TENANT)).thenReturn(List.of(
                establecimiento(estA1, empresaA, "A1"), establecimiento(estA2, empresaA, "A2", "SUSPENDIDO"),
                establecimiento(estB1, empresaB, "B1", "CLAUSURADO")));
        when(repository.findAllAlmacenes(TENANT)).thenReturn(List.of(
                almacen(almacenActivo, estA1, "Activo", true), almacen(almacenInactivo, estA1, "Inactivo", false),
                almacen(almacenDeB, estB1, "DeB", true)));
        when(repository.findAllTerminalesActivos(TENANT)).thenReturn(List.of(
                terminal(terminalA1, estA1, "Caja A1"), terminal(terminalB1, estB1, "Caja B1"),
                terminal(terminalBloqueado, estB1, "Caja B2", "BLOQUEADO")));

        var structure = adapter.findEstructuraCorporativa(TENANT);

        assertThat(structure.asOf()).isNotNull();
        assertThat(structure.companies()).extracting(EmpresaNodoResult::id)
                .containsExactly(empresaA, empresaB, empresaVacia);
        var companyA = structure.companies().get(0);
        assertThat(companyA.establishments()).extracting(node -> node.id()).containsExactly(estA1, estA2);
        var establishmentA1 = companyA.establishments().get(0);
        assertThat(establishmentA1.warehouses()).extracting(node -> node.id())
                .containsExactly(almacenActivo, almacenInactivo);
        assertThat(establishmentA1.warehouses()).extracting(node -> node.status())
                .containsExactly("ACTIVE", "INACTIVE");
        assertThat(establishmentA1.cashRegisters()).extracting(node -> node.id()).containsExactly(terminalA1);
        assertThat(establishmentA1.cashRegisters().get(0).status()).isEqualTo("ACTIVE");
        assertThat(establishmentA1.status()).isEqualTo("ACTIVE");
        assertThat(companyA.status()).isEqualTo("ACTIVE");
        var establishmentA2 = companyA.establishments().get(1);
        assertThat(establishmentA2.status()).isEqualTo("SUSPENDED");
        assertThat(establishmentA2.warehouses()).isEmpty();
        assertThat(establishmentA2.cashRegisters()).isEmpty();
        var companyB = structure.companies().get(1);
        assertThat(companyB.establishments()).singleElement().satisfies(establishment -> {
            assertThat(establishment.warehouses()).extracting(node -> node.id()).containsExactly(almacenDeB);
            assertThat(establishment.status()).isEqualTo("INACTIVE");
            assertThat(establishment.cashRegisters()).extracting(node -> node.id())
                    .containsExactly(terminalB1, terminalBloqueado);
            assertThat(establishment.cashRegisters()).extracting(node -> node.status())
                    .containsExactly("ACTIVE", "INACTIVE");
        });
        assertThat(structure.companies().get(2).establishments()).isEmpty();
        assertThat(structure.companies().get(2).status()).isEqualTo("INACTIVE");
    }
}
