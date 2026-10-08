package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.conflict;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
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
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AbrirTurnoHandlerTest {

    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final ConsultarTurnosUseCase consultas = mock(ConsultarTurnosUseCase.class);
    private final AbrirTurnoHandler handler =
            new AbrirTurnoHandler(turnos, referencias, consultas, () -> TURNO, () -> AHORA);

    @BeforeEach
    void theTerminalIsOperableWithoutAnOpenTurnoAndTheInsertSucceedsByDefault() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, true)));
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(conflict());
        when(turnos.insertar(any())).thenReturn(GuardadoOutcome.GUARDADO);
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(ok(turnoResult()));
    }

    private static AbrirTurnoCommand command(String fondo) {
        return new AbrirTurnoCommand(TENANT, ACTOR_ID, TERMINAL, fondo == null ? null : dec(fondo));
    }

    private static ApplicationError error(Result<TurnoResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void opensTheTurnoAndReturnsTheStoredOne() {
        var result = handler.execute(command("50"));

        assertThat(result.<TurnoResult>fold(value -> value, error -> null)).isEqualTo(turnoResult());
        var captor = ArgumentCaptor.forClass(TurnoCaja.class);
        verify(turnos).insertar(captor.capture());
        var turno = captor.getValue();
        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(turno.tenantId()).isEqualTo(TENANT);
        assertThat(turno.terminalId()).isEqualTo(TERMINAL);
        assertThat(turno.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(turno.cajero().id()).isEqualTo(ACTOR_ID);
        assertThat(turno.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(turno.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(turno.aperturaAt()).isEqualTo(AHORA);
    }

    @Test
    void anUnknownTerminalIsNotFound() {
        when(referencias.terminal(TENANT, TERMINAL)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TERMINAL_NO_ENCONTRADA");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void aTerminalThatIsNotOperableIsRejected() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, false)));

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TERMINAL_NO_OPERABLE");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void aTerminalThatAlreadyHasAnOpenTurnoIsRejectedBeforeInserting() {
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(ok(turnoResult()));

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TURNO_YA_ABIERTO");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void anInvalidInitialFundIsRejectedWithoutInserting() {
        assertThat(error(handler.execute(command("-1"))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        assertThat(error(handler.execute(command(null))).code()).isEqualTo("VEN_MONTO_INVALIDO");
        verify(turnos, never()).insertar(any());
    }

    @Test
    void aConcurrentOpenThatLosesTheUniqueIndexRaceIsAConflict() {
        when(turnos.insertar(any())).thenReturn(GuardadoOutcome.DUPLICADO);

        assertThat(error(handler.execute(command("50"))).code()).isEqualTo("VEN_TURNO_YA_ABIERTO");
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(null, referencias, consultas, () -> TURNO, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, null, consultas, () -> TURNO, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, referencias, null, () -> TURNO, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, referencias, consultas, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AbrirTurnoHandler(turnos, referencias, consultas, () -> TURNO, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
