package com.softprimesolutions.ventas.application.usecase.command;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.OTRO_SKU;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.skuRef;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.LineaVentaInput;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.AlmacenRef;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.TerminalRef;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort.SalidaSolicitada;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort.VentaExistente;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.Venta;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrarVentaHandlerTest {

    private static final String CLAVE = "clave-1";

    private final VentaWritePort ventas = mock(VentaWritePort.class);
    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final SalidaInventarioPort inventario = mock(SalidaInventarioPort.class);
    private final NumeracionPort numeracion = mock(NumeracionPort.class);
    private final ConsultarVentasUseCase consultas = mock(ConsultarVentasUseCase.class);
    private final RegistrarVentaHandler handler = new RegistrarVentaHandler(
            ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
            () -> AHORA);

    @BeforeEach
    void theContextIsValidAndEveryCollaboratorSucceedsByDefault() {
        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, true)));
        when(turnos.bloquearTurnoAbierto(TENANT, TERMINAL))
                .thenReturn(Optional.of(turno(EstadoTurno.ABIERTO, "50.00")));
        when(referencias.almacen(TENANT, ALMACEN))
                .thenReturn(Optional.of(new AlmacenRef(ALMACEN, ESTABLECIMIENTO, true)));
        when(referencias.skus(eq(TENANT), anyCollection()))
                .thenReturn(Map.of(SKU, skuRef(SKU, true), OTRO_SKU, skuRef(OTRO_SKU, true)));
        when(numeracion.siguienteNumeroOperacion(TENANT, TERMINAL)).thenReturn("EST001-POS01-000001");
        when(ventas.insertar(any(), any(), any())).thenReturn(GuardadoOutcome.GUARDADO);
        when(inventario.descontar(any())).thenAnswer(invocation -> {
            SalidaSolicitada solicitada = invocation.getArgument(0);
            return ok(List.of(new LoteConsumo(LOTE, solicitada.cantidad())));
        });
        when(consultas.obtener(any(ObtenerVentaQuery.class))).thenReturn(ok(ventaResult()));
    }

    private static LineaVentaInput linea(UUID sku, String cantidad, String precio) {
        return new LineaVentaInput(sku, cantidad == null ? null : dec(cantidad), dec(precio));
    }

    private static RegistrarVentaCommand command(String clave, String recibido, LineaVentaInput... lineas) {
        return new RegistrarVentaCommand(
                TENANT, ACTOR_ID, clave, TERMINAL, ALMACEN, List.of(lineas), recibido == null ? null : dec(recibido));
    }

    private static RegistrarVentaCommand unaLinea() {
        return command(CLAVE, "20", linea(SKU, "5", "2.50"));
    }

    private static ApplicationError error(Result<VentaResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    private static VentaResult value(Result<VentaResult, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    @Test
    void registersTheSaleDiscountingTheStockAndRecordingTheConsumedLotes() {
        var result = value(handler.execute(unaLinea()));

        assertThat(result).isEqualTo(ventaResult());
        var ventaCaptor = ArgumentCaptor.forClass(Venta.class);
        verify(ventas).insertar(ventaCaptor.capture(), eq(CLAVE), any());
        var venta = ventaCaptor.getValue();
        assertThat(venta.tenantId()).isEqualTo(TENANT);
        assertThat(venta.terminalId()).isEqualTo(TERMINAL);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(venta.vendedor().id()).isEqualTo(ACTOR_ID);
        assertThat(venta.numeroOperacion()).isEqualTo("EST001-POS01-000001");
        assertThat(venta.fechaVenta()).isEqualTo(AHORA);
        assertThat(venta.total()).isEqualTo(dec("12.50"));
        assertThat(venta.pago().vuelto()).isEqualTo(dec("7.50"));
        var linea = venta.lineas().getFirst();
        assertThat(linea.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(linea.unidadVentaCodigo()).isEqualTo("UND");
        var salidaCaptor = ArgumentCaptor.forClass(SalidaSolicitada.class);
        verify(inventario).descontar(salidaCaptor.capture());
        var salida = salidaCaptor.getValue();
        assertThat(salida.tenantId()).isEqualTo(TENANT);
        assertThat(salida.almacenId()).isEqualTo(ALMACEN);
        assertThat(salida.skuId()).isEqualTo(SKU);
        assertThat(salida.cantidad()).isEqualTo(dec("5"));
        assertThat(salida.ventaId()).isEqualTo(venta.id());
        assertThat(salida.ventaLineaId()).isEqualTo(linea.id());
        assertThat(salida.actorId()).isEqualTo(ACTOR_ID);
        assertThat(salida.idempotencyKey()).isEqualTo(CLAVE + ":1");
        verify(ventas).registrarLotes(TENANT, linea.id(), List.of(new LoteConsumo(LOTE, dec("5"))));
        verify(consultas).obtener(new ObtenerVentaQuery(TENANT, venta.id()));
    }

    @Test
    void discountsTheLinesInSkuOrderEvenWhenTheyArriveInAnotherOne() {
        value(handler.execute(command(CLAVE, "100", linea(OTRO_SKU, "1", "5"), linea(SKU, "1", "5"))));

        var orden = inOrder(inventario);
        orden.verify(inventario).descontar(argThat(salida -> salida.skuId().equals(SKU)
                && salida.idempotencyKey().equals(CLAVE + ":2")));
        orden.verify(inventario).descontar(argThat(salida -> salida.skuId().equals(OTRO_SKU)
                && salida.idempotencyKey().equals(CLAVE + ":1")));
    }

    @Test
    void aRetryWithTheSameKeyAndBodyReturnsTheStoredSaleWithoutTouchingTheStock() {
        value(handler.execute(unaLinea()));
        var huellaCaptor = ArgumentCaptor.forClass(String.class);
        verify(ventas).insertar(any(), eq(CLAVE), huellaCaptor.capture());
        when(ventas.findPorIdempotencia(TENANT, CLAVE))
                .thenReturn(Optional.of(new VentaExistente(VENTA, huellaCaptor.getValue())));
        clearInvocations(ventas, inventario, numeracion);

        var repetida = value(handler.execute(unaLinea()));

        assertThat(repetida).isEqualTo(ventaResult());
        verify(ventas, never()).insertar(any(), any(), any());
        verify(inventario, never()).descontar(any());
        verify(numeracion, never()).siguienteNumeroOperacion(any(), any());
        verify(consultas).obtener(new ObtenerVentaQuery(TENANT, VENTA));
    }

    @Test
    void theSameKeyWithADifferentBodyIsAnIdempotencyConflict() {
        when(ventas.findPorIdempotencia(TENANT, CLAVE)).thenReturn(Optional.of(new VentaExistente(VENTA, "otra")));

        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_IDEMPOTENCY_CONFLICT");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void theIdempotencyKeyIsRequiredAndAtMostOneHundredSixtyCharacters() {
        assertThat(error(handler.execute(command(null, "20", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(command("  ", "20", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(command("x".repeat(161), "20", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_IDEMPOTENCY_KEY_INVALID");
        assertThat(handler.execute(command("x".repeat(160), "20", linea(SKU, "5", "2.50"))).isSuccess()).isTrue();
    }

    @Test
    void rejectsAnUnknownOrInoperableTerminal() {
        when(referencias.terminal(TENANT, TERMINAL)).thenReturn(Optional.empty());
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_TERMINAL_NO_ENCONTRADA");

        when(referencias.terminal(TENANT, TERMINAL))
                .thenReturn(Optional.of(new TerminalRef(TERMINAL, ESTABLECIMIENTO, false)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_TERMINAL_NO_OPERABLE");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void aTerminalWithoutAnOpenTurnoCannotSell() {
        when(turnos.bloquearTurnoAbierto(TENANT, TERMINAL)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_TURNO_NO_ABIERTO");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void rejectsAnUnknownInoperableOrForeignWarehouse() {
        when(referencias.almacen(TENANT, ALMACEN)).thenReturn(Optional.empty());
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_ALMACEN_NO_ENCONTRADO");

        when(referencias.almacen(TENANT, ALMACEN))
                .thenReturn(Optional.of(new AlmacenRef(ALMACEN, ESTABLECIMIENTO, false)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_ALMACEN_NO_OPERABLE");

        when(referencias.almacen(TENANT, ALMACEN))
                .thenReturn(Optional.of(new AlmacenRef(ALMACEN, UUID.randomUUID(), true)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_ALMACEN_DE_OTRO_ESTABLECIMIENTO");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void rejectsAnUnknownOrInoperableSku() {
        when(referencias.skus(eq(TENANT), anyCollection())).thenReturn(Map.of());
        var desconocido = error(handler.execute(unaLinea()));
        assertThat(desconocido.code()).isEqualTo("VEN_SKU_NO_ENCONTRADO");
        assertThat(desconocido.metadata()).containsEntry("skuId", SKU);

        when(referencias.skus(eq(TENANT), anyCollection())).thenReturn(Map.of(SKU, skuRef(SKU, false)));
        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo("VEN_SKU_NO_OPERABLE");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void rejectsInvalidLinesAnEmptySaleAndAnInsufficientCashAmountWithoutInserting() {
        assertThat(error(handler.execute(command(CLAVE, "20", linea(SKU, "0", "2.50")))).code())
                .isEqualTo("VEN_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(command(CLAVE, "20", linea(SKU, "0.5", "2.50")))).code())
                .isEqualTo("VEN_FRACCION_NO_PERMITIDA");
        assertThat(error(handler.execute(command(CLAVE, "20"))).code()).isEqualTo("VEN_VENTA_SIN_LINEAS");
        assertThat(error(handler.execute(command(CLAVE, "5", linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_MONTO_RECIBIDO_INSUFICIENTE");
        assertThat(error(handler.execute(command(CLAVE, null, linea(SKU, "5", "2.50")))).code())
                .isEqualTo("VEN_MONTO_INVALIDO");
        verify(ventas, never()).insertar(any(), any(), any());
    }

    @Test
    void anInventoryFailureStopsTheSaleAndPassesTheErrorThrough() {
        var sinStock = new StandardApplicationError("INV_STOCK_INSUFICIENTE", "Sin stock.", ErrorCategory.CONFLICT);
        doReturn(ok(List.of(new LoteConsumo(LOTE, dec("1")))))
                .doReturn(Result.failure(sinStock))
                .when(inventario).descontar(any());

        var failure = error(handler.execute(command(CLAVE, "100", linea(SKU, "1", "5"), linea(OTRO_SKU, "1", "5"))));

        assertThat(failure).isSameAs(sinStock);
        verify(inventario, times(2)).descontar(any());
        verify(ventas, times(1)).registrarLotes(any(), any(), any());
    }

    @Test
    void aConcurrentModificationRetriesTheWholeSale() {
        doReturn(Result.<List<LoteConsumo>, ApplicationError>failure(VentasErrors.modificacionConcurrente()))
                .doReturn(ok(List.of(new LoteConsumo(LOTE, dec("5")))))
                .when(inventario).descontar(any());

        var result = value(handler.execute(unaLinea()));

        assertThat(result).isEqualTo(ventaResult());
        verify(ventas, times(2)).insertar(any(), any(), any());
        verify(inventario, times(2)).descontar(any());
    }

    @Test
    void losingTheIdempotencyRaceThreeTimesIsReportedAsAConcurrentModification() {
        when(ventas.insertar(any(), any(), any())).thenReturn(GuardadoOutcome.DUPLICADO);

        assertThat(error(handler.execute(unaLinea())).code()).isEqualTo(VentasErrors.CONCURRENCIA);
        verify(ventas, times(3)).insertar(any(), any(), any());
        verify(inventario, never()).descontar(any());
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                null, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, null, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, null, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, null, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, null, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, null, TRANSACCION_DIRECTA, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultas, null, UUID::randomUUID,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, null,
                () -> AHORA));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, UUID::randomUUID,
                null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
