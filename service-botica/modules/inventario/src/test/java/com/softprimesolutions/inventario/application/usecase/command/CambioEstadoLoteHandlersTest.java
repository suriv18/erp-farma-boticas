package com.softprimesolutions.inventario.application.usecase.command;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.lote;
import static com.softprimesolutions.inventario.InventarioFixtures.loteHabilitado;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.inventario.application.dto.command.BloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.command.DesbloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.domain.model.EstadoLote;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CambioEstadoLoteHandlersTest {

    private final InventarioWritePort writePort = mock(InventarioWritePort.class);
    private final CambioEstadoLote cambio = new CambioEstadoLote(writePort, () -> AHORA);
    private final BloquearLoteHandler bloquear = new BloquearLoteHandler(cambio);
    private final DesbloquearLoteHandler desbloquear = new DesbloquearLoteHandler(cambio);

    private static BloquearLoteCommand bloquearCommand(String motivo) {
        return new BloquearLoteCommand(TENANT, LOTE, motivo, ACTOR_ID);
    }

    private static DesbloquearLoteCommand desbloquearCommand() {
        return new DesbloquearLoteCommand(TENANT, LOTE, ACTOR_ID);
    }

    private static ApplicationError error(Result<LoteResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    private static LoteResult value(Result<LoteResult, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    @Test
    void blocksAnEnabledLoteGuardingTheTransitionWithItsPreviousState() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.actualizarEstado(any(Lote.class), eq(EstadoLote.HABILITADO))).thenReturn(true);

        var result = value(bloquear.execute(bloquearCommand("Reclamo de calidad")));

        assertThat(result.estado()).isEqualTo("BLOQUEADO");
        assertThat(result.motivoEstado()).isEqualTo("Reclamo de calidad");
        assertThat(result.vendible()).isFalse();
        assertThat(result.fechaVencimiento()).isEqualTo(VENCIMIENTO);
    }

    @Test
    void unblocksABlockedLote() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(lote(EstadoLote.BLOQUEADO, VENCIMIENTO)));
        when(writePort.actualizarEstado(any(Lote.class), eq(EstadoLote.BLOQUEADO))).thenReturn(true);

        var result = value(desbloquear.execute(desbloquearCommand()));

        assertThat(result.estado()).isEqualTo("HABILITADO");
        assertThat(result.vendible()).isTrue();
    }

    @Test
    void reportsAMissingLoteAsNotFound() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.empty());

        assertThat(error(bloquear.execute(bloquearCommand("x"))).code())
                .isEqualTo(InventarioErrors.loteNoEncontrado().code());
        assertThat(error(desbloquear.execute(desbloquearCommand())).category()).isEqualTo(ErrorCategory.NOT_FOUND);
        verify(writePort, never()).actualizarEstado(any(), any());
    }

    @Test
    void mapsDomainRejectionsWithoutPersisting() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));

        assertThat(error(bloquear.execute(bloquearCommand(" "))).category()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(error(desbloquear.execute(desbloquearCommand())).category()).isEqualTo(ErrorCategory.CONFLICT);
        verify(writePort, never()).actualizarEstado(any(), any());
    }

    @Test
    void reportsAConcurrentStateChangeWhenTheGuardedUpdateTouchesNoRow() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.actualizarEstado(any(Lote.class), any(EstadoLote.class))).thenReturn(false);

        var failure = error(bloquear.execute(bloquearCommand("motivo")));

        assertThat(failure.code()).isEqualTo(InventarioErrors.CONCURRENCIA);
        assertThat(failure.category()).isEqualTo(ErrorCategory.CONFLICT);
    }

    @Test
    void usesTheTenantAndLoteOfTheCommandToLoadTheAggregate() {
        var otroLote = UUID.randomUUID();
        when(writePort.findLote(TENANT, otroLote)).thenReturn(Optional.empty());

        bloquear.execute(new BloquearLoteCommand(TENANT, otroLote, "x", ACTOR_ID));

        verify(writePort).findLote(TENANT, otroLote);
    }
}
