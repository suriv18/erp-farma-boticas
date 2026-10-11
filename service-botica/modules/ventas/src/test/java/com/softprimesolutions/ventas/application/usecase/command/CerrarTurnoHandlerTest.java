package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CerrarTurnoHandlerTest {

    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final ConsultarTurnosUseCase consultas = mock(ConsultarTurnosUseCase.class);
    private final CerrarTurnoHandler handler =
            new CerrarTurnoHandler(turnos, consultas, TRANSACCION_DIRECTA, () -> AHORA);

    @BeforeEach
    void theTurnoIsOpenWithFortyInCashSalesByDefault() {
        when(turnos.findPorIdParaActualizar(TENANT, TURNO))
                .thenReturn(Optional.of(turno(EstadoTurno.ABIERTO, "100.00")));
        when(turnos.totalVentasEfectivo(TENANT, TURNO)).thenReturn(dec("40.00"));
        when(turnos.actualizarCierre(any(), any())).thenReturn(true);
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(ok(turnoResult()));
    }

    private static CerrarTurnoCommand command(String declarado) {
        return new CerrarTurnoCommand(TENANT, ACTOR_ID, TURNO, declarado == null ? null : dec(declarado), "Cierre");
    }

    private static ApplicationError error(Result<TurnoResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void closesTheTurnoWithTheSystemTotalsAndReturnsTheStoredOne() {
        var result = handler.execute(command("135"));

        assertThat(result.<TurnoResult>fold(value -> value, error -> null)).isEqualTo(turnoResult());
        var turnoCaptor = ArgumentCaptor.forClass(TurnoCaja.class);
        var actorCaptor = ArgumentCaptor.forClass(Actor.class);
        verify(turnos).actualizarCierre(turnoCaptor.capture(), actorCaptor.capture());
        var cerrado = turnoCaptor.getValue();
        assertThat(cerrado.estado()).isEqualTo(EstadoTurno.CERRADO);
        assertThat(cerrado.cierreAt()).isEqualTo(AHORA);
        assertThat(cerrado.totalVentasSistema()).isEqualTo(dec("40.00"));
        assertThat(cerrado.totalSistema()).isEqualTo(dec("140.00"));
        assertThat(cerrado.totalDeclarado()).isEqualTo(dec("135.00"));
        assertThat(cerrado.diferencia()).isEqualTo(dec("-5.00"));
        assertThat(cerrado.observacionCierre()).isEqualTo("Cierre");
        assertThat(actorCaptor.getValue().id()).isEqualTo(ACTOR_ID);
    }

    @Test
    void anUnknownTurnoIsNotFound() {
        when(turnos.findPorIdParaActualizar(TENANT, TURNO)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command("135"))).code()).isEqualTo("VEN_TURNO_NO_ENCONTRADO");
        verify(turnos, never()).actualizarCierre(any(), any());
    }

    @Test
    void aClosedTurnoCannotBeClosedAgain() {
        when(turnos.findPorIdParaActualizar(TENANT, TURNO))
                .thenReturn(Optional.of(turno(EstadoTurno.CERRADO, "100.00")));

        assertThat(error(handler.execute(command("135"))).code()).isEqualTo("VEN_TURNO_ESTADO_INVALIDO");
        verify(turnos, never()).actualizarCierre(any(), any());
    }

    @Test
    void anInvalidDeclaredTotalIsRejected() {
        assertThat(error(handler.execute(command("-1"))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        assertThat(error(handler.execute(command(null))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        verify(turnos, never()).actualizarCierre(any(), any());
    }

    @Test
    void aLostUpdateIsReportedAsAConcurrentModification() {
        when(turnos.actualizarCierre(any(), any())).thenReturn(false);

        assertThat(error(handler.execute(command("135"))).code()).isEqualTo("VEN_MODIFICACION_CONCURRENTE");
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(null, consultas, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(turnos, null, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(turnos, consultas, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new CerrarTurnoHandler(turnos, consultas, TRANSACCION_DIRECTA, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
