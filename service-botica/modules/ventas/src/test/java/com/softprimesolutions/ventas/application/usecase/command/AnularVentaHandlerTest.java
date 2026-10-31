package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort.VentaParaAnular;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnularVentaHandlerTest {

    private final AnulacionWritePort ventas = mock(AnulacionWritePort.class);
    private final ReintegroInventarioPort inventario = mock(ReintegroInventarioPort.class);
    private final ConsultarVentasUseCase consultas = mock(ConsultarVentasUseCase.class);
    private final AnularVentaHandler handler =
            new AnularVentaHandler(ventas, inventario, consultas, TRANSACCION_DIRECTA, () -> AHORA);

    @BeforeEach
    void theSaleIsConfirmedInAnOpenTurnoAndEveryCollaboratorSucceedsByDefault() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(
                Optional.of(new VentaParaAnular(VENTA, TURNO, EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO)));
        when(inventario.reintegrar(TENANT, VENTA, ACTOR_ID)).thenReturn(ok(List.of(new LoteConsumo(LOTE, dec("5")))));
        when(ventas.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA)).thenReturn(true);
        when(consultas.obtener(new ObtenerVentaQuery(TENANT, VENTA))).thenReturn(ok(ventaResult()));
    }

    private static AnularVentaCommand command(String motivo) {
        return new AnularVentaCommand(TENANT, ACTOR_ID, VENTA, motivo);
    }

    private static ApplicationError error(Result<VentaResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void reintegratesTheStockThenMarksTheSaleAndReturnsTheStoredOne() {
        var result = handler.execute(command("  Error de cobro  "));

        assertThat(result.<VentaResult>fold(value -> value, error -> null)).isEqualTo(ventaResult());
        var orden = inOrder(ventas, inventario);
        orden.verify(ventas).bloquearVenta(TENANT, VENTA);
        orden.verify(inventario).reintegrar(TENANT, VENTA, ACTOR_ID);
        orden.verify(ventas).marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA);
    }

    @Test
    void anUnknownSaleIsNotFound() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo("VEN_VENTA_NO_ENCONTRADA");
        verify(inventario, never()).reintegrar(any(), any(), any());
    }

    @Test
    void anAlreadyAnnulledSaleIsAConflictAndTheStockIsNotTouched() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(
                Optional.of(new VentaParaAnular(VENTA, TURNO, EstadoVenta.ANULADA, EstadoTurno.ABIERTO)));

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo("VEN_VENTA_ESTADO_INVALIDO");
        verify(inventario, never()).reintegrar(any(), any(), any());
        verify(ventas, never()).marcarAnulada(any(), any(), any(), any(), any());
    }

    @Test
    void aSaleOfAClosedTurnoCannotBeAnnulled() {
        when(ventas.bloquearVenta(TENANT, VENTA)).thenReturn(
                Optional.of(new VentaParaAnular(VENTA, TURNO, EstadoVenta.CONFIRMADA, EstadoTurno.CERRADO)));

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo("VEN_TURNO_NO_ABIERTO");
        verify(inventario, never()).reintegrar(any(), any(), any());
    }

    @Test
    void aMissingOrTooLongReasonIsRejectedBeforeTouchingTheStock() {
        assertThat(error(handler.execute(command(null))).code()).isEqualTo("VEN_MOTIVO_INVALIDO");
        assertThat(error(handler.execute(command("x".repeat(501)))).code()).isEqualTo("VEN_MOTIVO_INVALIDO");
        verify(inventario, never()).reintegrar(any(), any(), any());
    }

    @Test
    void anInventoryFailurePassesThroughAndTheSaleIsNotMarked() {
        when(inventario.reintegrar(TENANT, VENTA, ACTOR_ID)).thenReturn(Result.failure(CONFLICTO));

        assertThat(error(handler.execute(command("Error de cobro")))).isSameAs(CONFLICTO);
        verify(ventas, never()).marcarAnulada(any(), any(), any(), any(), any());
    }

    @Test
    void aLostUpdateIsRetriedThreeTimesAndReportedAsAConcurrentModification() {
        when(ventas.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA)).thenReturn(false);

        assertThat(error(handler.execute(command("Error de cobro"))).code()).isEqualTo(VentasErrors.CONCURRENCIA);
        verify(ventas, times(3)).bloquearVenta(TENANT, VENTA);
    }

    @Test
    void aConcurrencyErrorFromInventoryRetriesTheWholeAnnulment() {
        doReturnConcurrenciaLuegoExito();

        var result = handler.execute(command("Error de cobro"));

        assertThat(result.<VentaResult>fold(value -> value, error -> null)).isEqualTo(ventaResult());
        verify(ventas, times(2)).bloquearVenta(TENANT, VENTA);
        verify(ventas, times(1)).marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA);
    }

    private void doReturnConcurrenciaLuegoExito() {
        doReturn(Result.<List<LoteConsumo>, ApplicationError>failure(VentasErrors.modificacionConcurrente()))
                .doReturn(ok(List.of(new LoteConsumo(LOTE, dec("5")))))
                .when(inventario).reintegrar(TENANT, VENTA, ACTOR_ID);
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(null, inventario, consultas, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, null, consultas, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, inventario, null, TRANSACCION_DIRECTA, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, inventario, consultas, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new AnularVentaHandler(ventas, inventario, consultas, TRANSACCION_DIRECTA, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
