package com.softprimesolutions.ventas.application.usecase.autorizado;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.conflict;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static com.softprimesolutions.ventas.application.usecase.autorizado.AmbitoDouble.error;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TurnosAutorizadosTest {

    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final VentasReadPort lectura = mock(VentasReadPort.class);
    private final AbrirTurnoUseCase abrir = mock(AbrirTurnoUseCase.class);
    private final CerrarTurnoUseCase cerrar = mock(CerrarTurnoUseCase.class);
    private final ConsultarTurnosUseCase consultar = mock(ConsultarTurnosUseCase.class);

    private final AbrirTurnoCommand abrirCommand = new AbrirTurnoCommand(TENANT, ACTOR_ID, TERMINAL, dec("50"));
    private final CerrarTurnoCommand cerrarCommand =
            new CerrarTurnoCommand(TENANT, ACTOR_ID, TURNO, dec("50"), null);

    private AbrirTurnoAutorizado abrir(AmbitoDouble ambito) {
        return new AbrirTurnoAutorizado(abrir, referencias, new ControlAmbito(ambito));
    }

    private CerrarTurnoAutorizado cerrar(AmbitoDouble ambito) {
        return new CerrarTurnoAutorizado(cerrar, lectura, new ControlAmbito(ambito));
    }

    private ConsultarTurnosAutorizado consultar(AmbitoDouble ambito) {
        return new ConsultarTurnosAutorizado(consultar, referencias, () -> ACTOR_ID, new ControlAmbito(ambito));
    }

    private void terminalConocida() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, true)));
    }

    @Test
    void opensATurnoWhenTheActorHoldsThePermissionOverTheTerminal() {
        var ambito = new AmbitoDouble(true);
        terminalConocida();
        when(abrir.execute(abrirCommand)).thenReturn(ok(turnoResult()));

        assertThat(abrir(ambito).execute(abrirCommand)).isEqualTo(ok(turnoResult()));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.turnos.abrir", ESTABLECIMIENTO, null, TERMINAL));
    }

    @Test
    void rejectsOpeningATurnoOutsideTheScope() {
        terminalConocida();

        assertThat(error(abrir(new AmbitoDouble(false)).execute(abrirCommand)).code())
                .isEqualTo("VEN_FUERA_DE_AMBITO");
        verify(abrir, never()).execute(any());
    }

    @Test
    void letsTheUseCaseReportAnUnknownTerminalOnOpening() {
        var ambito = new AmbitoDouble(false);
        when(referencias.terminal(TENANT, TERMINAL)).thenReturn(Optional.empty());
        when(abrir.execute(abrirCommand)).thenReturn(conflict());

        assertThat(error(abrir(ambito).execute(abrirCommand))).isEqualTo(CONFLICTO);
        assertThat(ambito.consultas()).isEmpty();
    }

    @Test
    void closesATurnoWhenTheActorHoldsThePermissionOverIt() {
        var ambito = new AmbitoDouble(true);
        when(lectura.findTurno(TENANT, TURNO)).thenReturn(Optional.of(turnoResult()));
        when(cerrar.execute(cerrarCommand)).thenReturn(ok(turnoResult()));

        assertThat(cerrar(ambito).execute(cerrarCommand)).isEqualTo(ok(turnoResult()));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.turnos.cerrar", ESTABLECIMIENTO, null, TERMINAL));
    }

    @Test
    void rejectsClosingATurnoOutsideTheScope() {
        when(lectura.findTurno(TENANT, TURNO)).thenReturn(Optional.of(turnoResult()));

        assertThat(error(cerrar(new AmbitoDouble(false)).execute(cerrarCommand)).code())
                .isEqualTo("VEN_FUERA_DE_AMBITO");
        verify(cerrar, never()).execute(any());
    }

    @Test
    void letsTheUseCaseReportAnUnknownTurnoOnClosing() {
        var ambito = new AmbitoDouble(false);
        when(lectura.findTurno(TENANT, TURNO)).thenReturn(Optional.empty());
        when(cerrar.execute(cerrarCommand)).thenReturn(conflict());

        assertThat(error(cerrar(ambito).execute(cerrarCommand))).isEqualTo(CONFLICTO);
        assertThat(ambito.consultas()).isEmpty();
    }

    @Test
    void returnsATurnoOnlyWhenItIsWithinTheScope() {
        var query = new ObtenerTurnoQuery(TENANT, TURNO);
        var ambito = new AmbitoDouble(true);
        when(consultar.obtener(query)).thenReturn(ok(turnoResult()));

        assertThat(consultar(ambito).obtener(query)).isEqualTo(ok(turnoResult()));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.turnos.consultar", ESTABLECIMIENTO, null, TERMINAL));
    }

    @Test
    void hidesATurnoOutsideTheScopeAndPropagatesUseCaseErrors() {
        var query = new ObtenerTurnoQuery(TENANT, TURNO);
        when(consultar.obtener(query)).thenReturn(ok(turnoResult()));
        assertThat(error(consultar(new AmbitoDouble(false)).obtener(query)).code()).isEqualTo("VEN_FUERA_DE_AMBITO");

        when(consultar.obtener(query)).thenReturn(conflict());
        assertThat(error(consultar(new AmbitoDouble(false)).obtener(query))).isEqualTo(CONFLICTO);
    }

    @Test
    void returnsTheCurrentTurnOfATerminalOnlyWithinTheScope() {
        var query = new TurnoActualQuery(TENANT, TERMINAL);
        var ambito = new AmbitoDouble(true);
        terminalConocida();
        when(consultar.actual(query)).thenReturn(ok(turnoResult()));

        assertThat(consultar(ambito).actual(query)).isEqualTo(ok(turnoResult()));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.turnos.consultar", ESTABLECIMIENTO, null, TERMINAL));
    }

    @Test
    void rejectsTheCurrentTurnOutsideTheScopeAndLetsTheUseCaseReportAnUnknownTerminal() {
        var query = new TurnoActualQuery(TENANT, TERMINAL);
        terminalConocida();
        assertThat(error(consultar(new AmbitoDouble(false)).actual(query)).code()).isEqualTo("VEN_FUERA_DE_AMBITO");
        verify(consultar, never()).actual(any());

        when(referencias.terminal(TENANT, TERMINAL)).thenReturn(Optional.empty());
        when(consultar.actual(query)).thenReturn(conflict());
        assertThat(error(consultar(new AmbitoDouble(false)).actual(query))).isEqualTo(CONFLICTO);
    }
}
