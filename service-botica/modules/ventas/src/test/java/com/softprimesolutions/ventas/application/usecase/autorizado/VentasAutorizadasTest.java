package com.softprimesolutions.ventas.application.usecase.autorizado;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static com.softprimesolutions.ventas.application.usecase.autorizado.AmbitoDouble.error;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VentasAutorizadasTest {

    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final VentasReadPort lectura = mock(VentasReadPort.class);
    private final RegistrarVentaUseCase registrar = mock(RegistrarVentaUseCase.class);
    private final AnularVentaUseCase anular = mock(AnularVentaUseCase.class);
    private final ConsultarVentasUseCase consultar = mock(ConsultarVentasUseCase.class);

    private final RegistrarVentaCommand registrarCommand =
            new RegistrarVentaCommand(TENANT, ACTOR_ID, "clave-1", TERMINAL, ALMACEN, List.of(), dec("20"));
    private final AnularVentaCommand anularCommand = new AnularVentaCommand(TENANT, ACTOR_ID, VENTA, "Error");

    private RegistrarVentaAutorizado registrar(AmbitoDouble ambito) {
        return new RegistrarVentaAutorizado(registrar, referencias, new ControlAmbito(ambito));
    }

    private AnularVentaAutorizado anular(AmbitoDouble ambito) {
        return new AnularVentaAutorizado(anular, lectura, new ControlAmbito(ambito));
    }

    private ConsultarVentasAutorizado consultar(AmbitoDouble ambito) {
        return new ConsultarVentasAutorizado(consultar, () -> ACTOR_ID, new ControlAmbito(ambito));
    }

    @Test
    void registersWhenTheActorHoldsThePermissionOverTheTerminalEstablishmentAndWarehouse() {
        var ambito = new AmbitoDouble(true);
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, true)));
        when(registrar.execute(registrarCommand)).thenReturn(ok(ventaResult()));

        var result = registrar(ambito).execute(registrarCommand);

        assertThat(result).isEqualTo(ok(ventaResult()));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.ventas.registrar", ESTABLECIMIENTO, ALMACEN, TERMINAL));
    }

    @Test
    void rejectsRegisteringOutsideTheScopeWithoutRunningTheUseCase() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, true)));

        var error = error(registrar(new AmbitoDouble(false)).execute(registrarCommand));

        assertThat(error.code()).isEqualTo("VEN_FUERA_DE_AMBITO");
        verify(registrar, never()).execute(any());
    }

    @Test
    void letsTheUseCaseReportAnUnknownTerminalOnRegistration() {
        var ambito = new AmbitoDouble(false);
        when(referencias.terminal(TENANT, TERMINAL)).thenReturn(Optional.empty());
        when(registrar.execute(registrarCommand)).thenReturn(com.softprimesolutions.ventas.VentasFixtures.conflict());

        assertThat(error(registrar(ambito).execute(registrarCommand))).isEqualTo(CONFLICTO);
        assertThat(ambito.consultas()).isEmpty();
    }

    @Test
    void annulsWhenTheActorHoldsThePermissionOverTheSale() {
        var ambito = new AmbitoDouble(true);
        when(lectura.findVenta(TENANT, VENTA)).thenReturn(Optional.of(ventaResult()));
        when(anular.execute(anularCommand)).thenReturn(ok(ventaResult()));

        assertThat(anular(ambito).execute(anularCommand)).isEqualTo(ok(ventaResult()));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.ventas.anular", ESTABLECIMIENTO, null, TERMINAL));
    }

    @Test
    void rejectsAnnullingOutsideTheScopeWithoutRunningTheUseCase() {
        when(lectura.findVenta(TENANT, VENTA)).thenReturn(Optional.of(ventaResult()));

        assertThat(error(anular(new AmbitoDouble(false)).execute(anularCommand)).code())
                .isEqualTo("VEN_FUERA_DE_AMBITO");
        verify(anular, never()).execute(any());
    }

    @Test
    void letsTheUseCaseReportAnUnknownSaleOnAnnulment() {
        var ambito = new AmbitoDouble(false);
        when(lectura.findVenta(TENANT, VENTA)).thenReturn(Optional.empty());
        when(anular.execute(anularCommand)).thenReturn(com.softprimesolutions.ventas.VentasFixtures.conflict());

        assertThat(error(anular(ambito).execute(anularCommand))).isEqualTo(CONFLICTO);
        assertThat(ambito.consultas()).isEmpty();
    }

    @Test
    void returnsTheSaleOnlyWhenItsEstablishmentAndTerminalAreWithinTheScope() {
        var query = new ObtenerVentaQuery(TENANT, VENTA);
        var ambito = new AmbitoDouble(true);
        when(consultar.obtener(query)).thenReturn(ok(ventaResult()));

        assertThat(consultar(ambito).obtener(query)).isEqualTo(ok(ventaResult()));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.ventas.consultar", ESTABLECIMIENTO, null, TERMINAL));
    }

    @Test
    void hidesTheSaleWhenItIsOutsideTheScope() {
        var query = new ObtenerVentaQuery(TENANT, VENTA);
        when(consultar.obtener(query)).thenReturn(ok(ventaResult()));

        assertThat(error(consultar(new AmbitoDouble(false)).obtener(query)).code()).isEqualTo("VEN_FUERA_DE_AMBITO");
    }

    @Test
    void propagatesTheUseCaseErrorWithoutCheckingTheScope() {
        var query = new ObtenerVentaQuery(TENANT, VENTA);
        var ambito = new AmbitoDouble(false);
        when(consultar.obtener(query)).thenReturn(com.softprimesolutions.ventas.VentasFixtures.conflict());

        assertThat(error(consultar(ambito).obtener(query))).isEqualTo(CONFLICTO);
        assertThat(ambito.consultas()).isEmpty();
    }

    @Test
    void listsOnlyWhenTheRequestedEstablishmentIsWithinTheScope() {
        var query = new ListarVentasQuery(TENANT, ESTABLECIMIENTO, null, null, 0, 20);
        var pagina = new PaginaResult<VentaResumenResult>(List.of(), 0, 20, 0);
        var ambito = new AmbitoDouble(true);
        when(consultar.listar(query)).thenReturn(ok(pagina));

        assertThat(consultar(ambito).listar(query)).isEqualTo(ok(pagina));
        assertThat(ambito.consultas()).containsExactly(
                new Ambito(TENANT, ACTOR_ID, "ventas.ventas.consultar", ESTABLECIMIENTO, null, null));
    }

    @Test
    void rejectsListingOutsideTheScope() {
        var query = new ListarVentasQuery(TENANT, ESTABLECIMIENTO, null, null, 0, 20);

        assertThat(error(consultar(new AmbitoDouble(false)).listar(query)).code()).isEqualTo("VEN_FUERA_DE_AMBITO");
        verify(consultar, never()).listar(any());
    }

    @Test
    void asksForTheEstablishmentWhenListingWithoutOneIsOutsideTheScope() {
        var query = new ListarVentasQuery(TENANT, null, null, null, 0, 20);

        assertThat(error(consultar(new AmbitoDouble(false)).listar(query)).code())
                .isEqualTo("VEN_ESTABLECIMIENTO_REQUERIDO");
        verify(consultar, never()).listar(any());
    }
}
