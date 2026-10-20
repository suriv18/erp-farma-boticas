package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoPuestoResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPuestoResult;
import com.softprimesolutions.organizacion.application.port.out.AlcancePuestoPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerMiPuestoHandlerTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID EMPRESA_A = UUID.randomUUID();
    private static final UUID EMPRESA_B = UUID.randomUUID();
    private static final UUID EST_A = UUID.randomUUID();
    private static final UUID EST_B = UUID.randomUUID();
    private static final UUID ALM_A = UUID.randomUUID();
    private static final UUID TERM_A1 = UUID.randomUUID();
    private static final UUID TERM_A2 = UUID.randomUUID();

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final AlcancePuestoPort alcancePort = mock(AlcancePuestoPort.class);
    private final ObtenerMiPuestoHandler handler = new ObtenerMiPuestoHandler(readPort, alcancePort);

    private static AlcancePuesto alcance(
            boolean global, Set<UUID> empresas, Set<UUID> establecimientos, Set<UUID> almacenes, Set<UUID> terminales) {
        return new AlcancePuesto(global, empresas, establecimientos, almacenes, terminales);
    }

    private void withScope(AlcancePuesto alcance) {
        when(readPort.findEstablecimientosPuesto(TENANT)).thenReturn(List.of(
                new EstablecimientoPuestoResult(EST_A, EMPRESA_A, "A", "Botica A",
                        List.of(new AlmacenPuestoResult(ALM_A, "ALM", "Almacen", "GENERAL", true)),
                        List.of(new TerminalPuestoResult(TERM_A1, "T1", "Caja 1", "ACTIVO", true),
                                new TerminalPuestoResult(TERM_A2, "T2", "Caja 2", "MANTENIMIENTO", false))),
                new EstablecimientoPuestoResult(EST_B, EMPRESA_B, "B", "Botica B", List.of(), List.of())));
        when(alcancePort.alcanceDe(TENANT, USUARIO)).thenReturn(alcance);
    }

    private List<EstablecimientoPuestoResult> run() {
        return handler.execute(new ObtenerMiPuestoQuery(TENANT, USUARIO)).getOrElse(error -> null)
                .establecimientos();
    }

    @Test
    void aGlobalActorSeesEveryEstablishmentWithAllItsResources() {
        withScope(alcance(true, Set.of(), Set.of(), Set.of(), Set.of()));

        assertThat(run()).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A, EST_B);
        assertThat(run().get(0).terminales()).hasSize(2);
    }

    @Test
    void aCompanyAssignmentShowsEveryEstablishmentOfThatCompany() {
        withScope(alcance(false, Set.of(EMPRESA_B), Set.of(), Set.of(), Set.of()));

        assertThat(run()).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_B);
    }

    @Test
    void anEstablishmentAssignmentShowsThatEstablishmentWithAllItsResources() {
        withScope(alcance(false, Set.of(), Set.of(EST_A), Set.of(), Set.of()));

        var visibles = run();

        assertThat(visibles).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A);
        assertThat(visibles.get(0).almacenes()).hasSize(1);
        assertThat(visibles.get(0).terminales()).hasSize(2);
    }

    @Test
    void aTerminalAssignmentShowsOnlyThatTerminalAndNoWarehouses() {
        withScope(alcance(false, Set.of(), Set.of(), Set.of(), Set.of(TERM_A2)));

        var visibles = run();

        assertThat(visibles).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A);
        assertThat(visibles.get(0).terminales()).extracting(TerminalPuestoResult::id).containsExactly(TERM_A2);
        assertThat(visibles.get(0).almacenes()).isEmpty();
    }

    @Test
    void aWarehouseAssignmentShowsOnlyThatWarehouseAndNoTerminals() {
        withScope(alcance(false, Set.of(), Set.of(), Set.of(ALM_A), Set.of()));

        var visibles = run();

        assertThat(visibles).extracting(EstablecimientoPuestoResult::id).containsExactly(EST_A);
        assertThat(visibles.get(0).almacenes()).extracting(AlmacenPuestoResult::id).containsExactly(ALM_A);
        assertThat(visibles.get(0).terminales()).isEmpty();
    }

    @Test
    void anActorWithoutAssignmentsSeesNothing() {
        withScope(alcance(false, Set.of(), Set.of(), Set.of(), Set.of()));

        assertThat(run()).isEmpty();
    }

    @Test
    void requiresItsCollaboratorsAndTheQuery() {
        assertThatThrownBy(() -> new ObtenerMiPuestoHandler(null, alcancePort)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ObtenerMiPuestoHandler(readPort, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> handler.execute(null)).isInstanceOf(NullPointerException.class);
    }
}
