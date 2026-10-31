package com.softprimesolutions.organizacion.application.dto.result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PuestoResultsTest {

    @Test
    void theEstablishmentKeepsImmutableCopiesOfItsResources() {
        var almacenes = new ArrayList<>(List.of(
                new AlmacenPuestoResult(UUID.randomUUID(), "A", "Almacen", "GENERAL", true)));
        var terminales = new ArrayList<>(List.of(
                new TerminalPuestoResult(UUID.randomUUID(), "T", "Caja", "ACTIVO", true)));

        var establecimiento = new EstablecimientoPuestoResult(
                UUID.randomUUID(), UUID.randomUUID(), "E", "Botica", almacenes, terminales);
        almacenes.clear();
        terminales.clear();

        assertThat(establecimiento.almacenes()).hasSize(1);
        assertThat(establecimiento.terminales()).hasSize(1);
        assertThatThrownBy(() -> establecimiento.almacenes().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void theResultKeepsAnImmutableCopyOfItsEstablishments() {
        var lista = new ArrayList<EstablecimientoPuestoResult>();

        var resultado = new MiPuestoResult(lista);
        lista.add(new EstablecimientoPuestoResult(UUID.randomUUID(), UUID.randomUUID(), "E", "B", List.of(), List.of()));

        assertThat(resultado.establecimientos()).isEmpty();
    }

    @Test
    void theScopeKeepsImmutableCopiesOfItsSets() {
        var alcance = new AlcancePuesto(false, Set.of(), Set.of(), Set.of(), Set.of());

        assertThatThrownBy(() -> alcance.empresas().add(UUID.randomUUID()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(alcance.global()).isFalse();
    }
}
