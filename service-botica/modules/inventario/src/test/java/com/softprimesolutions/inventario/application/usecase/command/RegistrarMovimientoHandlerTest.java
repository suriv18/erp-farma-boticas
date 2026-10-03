package com.softprimesolutions.inventario.application.usecase.command;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.HOY;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.OTRO_SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.lote;
import static com.softprimesolutions.inventario.InventarioFixtures.loteHabilitado;
import static com.softprimesolutions.inventario.InventarioFixtures.posicion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.mapper.InventarioApplicationMapper;
import com.softprimesolutions.inventario.application.port.out.MovimientoRegistrado;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort.RegistroOutcome;
import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort;
import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort.EstadoReferencia;
import com.softprimesolutions.inventario.application.port.out.RegistroMovimiento;
import com.softprimesolutions.inventario.domain.model.EstadoLote;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrarMovimientoHandlerTest {

    private static final String NUMERO = "L-001";

    private final InventarioWritePort writePort = mock(InventarioWritePort.class);
    private final ReferenciasInventarioPort referencias = mock(ReferenciasInventarioPort.class);
    private final RegistrarMovimientoHandler handler =
            new RegistrarMovimientoHandler(writePort, referencias, UUID::randomUUID, () -> AHORA);

    @BeforeEach
    void everythingIsOperableAndTheRegistrationSucceedsByDefault() {
        when(referencias.estadoAlmacen(TENANT, ALMACEN)).thenReturn(EstadoReferencia.OPERABLE);
        when(referencias.estadoSku(TENANT, SKU)).thenReturn(EstadoReferencia.OPERABLE);
        when(writePort.registrar(any())).thenReturn(RegistroOutcome.REGISTRADO);
    }

    private static RegistrarMovimientoCommand command(
            UUID loteId, String numeroLote, LocalDate vencimiento, String tipo, String cantidad, String motivo) {
        return commandWithKey(loteId, numeroLote, vencimiento, tipo, cantidad, motivo, null);
    }

    private static RegistrarMovimientoCommand commandWithKey(
            UUID loteId, String numeroLote, LocalDate vencimiento, String tipo, String cantidad, String motivo,
            String idempotencyKey) {
        return new RegistrarMovimientoCommand(
                TENANT, ALMACEN, SKU, loteId, numeroLote, vencimiento, tipo,
                cantidad == null ? null : new BigDecimal(cantidad), motivo, ACTOR_ID, idempotencyKey);
    }

    private static RegistrarMovimientoCommand ingresoSobreLote(String cantidad) {
        return command(LOTE, null, null, "AJUSTE_INGRESO", cantidad, "Inventario inicial");
    }

    private static RegistrarMovimientoCommand salidaSobreLote(String cantidad) {
        return command(LOTE, null, null, "AJUSTE_SALIDA", cantidad, "Merma");
    }

    private static RegistrarMovimientoCommand ingresoConLoteNuevo() {
        return command(null, "  " + NUMERO + " ", VENCIMIENTO, "AJUSTE_INGRESO", "10", "Inventario inicial");
    }

    private static ApplicationError error(Result<MovimientoResult, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    private static MovimientoResult value(Result<MovimientoResult, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private RegistroMovimiento registered() {
        var captor = ArgumentCaptor.forClass(RegistroMovimiento.class);
        verify(writePort).registrar(captor.capture());
        return captor.getValue();
    }

    @Test
    void registersAnIngresoIntoAnExistingLoteAndExistingPosition() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 4)));

        var result = value(handler.execute(ingresoSobreLote("5")));

        assertThat(result.tipo()).isEqualTo("AJUSTE_INGRESO");
        assertThat(result.naturaleza()).isEqualTo("E");
        assertThat(result.stockAnterior()).isEqualByComparingTo("10");
        assertThat(result.stockPosterior()).isEqualByComparingTo("15");
        var registro = registered();
        assertThat(registro.loteNuevo()).isFalse();
        assertThat(registro.posicionNueva()).isFalse();
        assertThat(registro.posicion().version()).isEqualTo(4);
        assertThat(registro.motivo()).isEqualTo("Inventario inicial");
        assertThat(registro.fechaNegocio()).isEqualTo(AHORA);
        assertThat(registro.actor().id()).isEqualTo(ACTOR_ID);
    }

    @Test
    void createsTheLoteAndThePositionOnTheFirstIngresoOfANewLote() {
        when(writePort.findLotePorClave(TENANT, SKU, NUMERO, VENCIMIENTO)).thenReturn(Optional.empty());
        when(writePort.findPosicion(any(), any(), any())).thenReturn(Optional.empty());

        var result = value(handler.execute(ingresoConLoteNuevo()));

        assertThat(result.stockAnterior()).isEqualByComparingTo("0");
        assertThat(result.stockPosterior()).isEqualByComparingTo("10");
        var registro = registered();
        assertThat(registro.loteNuevo()).isTrue();
        assertThat(registro.posicionNueva()).isTrue();
        assertThat(registro.lote().numeroLote()).isEqualTo(NUMERO);
        assertThat(registro.lote().estado()).isEqualTo(EstadoLote.HABILITADO);
        assertThat(registro.posicion().loteId()).isEqualTo(registro.lote().id().value());
    }

    @Test
    void reusesAnExistingLoteFoundByItsNaturalKey() {
        when(writePort.findLotePorClave(TENANT, SKU, NUMERO, VENCIMIENTO)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.empty());

        value(handler.execute(ingresoConLoteNuevo()));

        var registro = registered();
        assertThat(registro.loteNuevo()).isFalse();
        assertThat(registro.posicionNueva()).isTrue();
        assertThat(registro.lote().id().value()).isEqualTo(LOTE);
    }

    @Test
    void registersASalidaFromABlockedLote() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(lote(EstadoLote.BLOQUEADO, VENCIMIENTO)));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));

        var result = value(handler.execute(salidaSobreLote("4")));

        assertThat(result.naturaleza()).isEqualTo("S");
        assertThat(result.stockPosterior()).isEqualByComparingTo("6");
    }

    @Test
    void rejectsAnUnknownMovementType() {
        var failure = error(handler.execute(command(LOTE, null, null, "TRASLADO", "1", "x")));

        assertThat(failure.code()).isEqualTo("INV_TIPO_MOVIMIENTO_INVALIDO");
        verify(writePort, never()).registrar(any());
    }

    @Test
    void rejectsAMissingBlankOrOversizedReason() {
        for (var motivo : new String[] {null, "   ", "x".repeat(1001)}) {
            var failure = error(handler.execute(command(LOTE, null, null, "AJUSTE_INGRESO", "1", motivo)));

            assertThat(failure.code()).isEqualTo("INV_MOTIVO_INVALIDO");
        }
        var motivoMaximo = error(handler.execute(command(LOTE, null, null, "AJUSTE_INGRESO", "1", "x".repeat(1000))));
        assertThat(motivoMaximo.code()).isNotEqualTo("INV_MOTIVO_INVALIDO");
        verify(writePort, never()).registrar(any());
    }

    @Test
    void requiresAnExistingLoteForASalidaAndLoteDataForAnIngresoWithoutLoteId() {
        var sinLote = command(null, NUMERO, VENCIMIENTO, "AJUSTE_SALIDA", "1", "x");
        var sinNumero = command(null, null, VENCIMIENTO, "AJUSTE_INGRESO", "1", "x");
        var numeroEnBlanco = command(null, "  ", VENCIMIENTO, "AJUSTE_INGRESO", "1", "x");
        var sinFecha = command(null, NUMERO, null, "AJUSTE_INGRESO", "1", "x");

        for (var invalid : new RegistrarMovimientoCommand[] {sinLote, sinNumero, numeroEnBlanco, sinFecha}) {
            assertThat(error(handler.execute(invalid)).code()).isEqualTo("INV_LOTE_REFERENCIA_INVALIDA");
        }
    }

    @Test
    void rejectsAMissingOrNonOperableWarehouse() {
        when(referencias.estadoAlmacen(TENANT, ALMACEN)).thenReturn(EstadoReferencia.INEXISTENTE);
        var inexistente = error(handler.execute(ingresoSobreLote("1")));
        when(referencias.estadoAlmacen(TENANT, ALMACEN)).thenReturn(EstadoReferencia.NO_OPERABLE);
        var noOperable = error(handler.execute(ingresoSobreLote("1")));

        assertThat(inexistente.category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(inexistente.code()).isEqualTo("INV_ALMACEN_NO_ENCONTRADO");
        assertThat(noOperable.category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(noOperable.code()).isEqualTo("INV_ALMACEN_NO_OPERABLE");
    }

    @Test
    void rejectsAMissingOrNonOperableSku() {
        when(referencias.estadoSku(TENANT, SKU)).thenReturn(EstadoReferencia.INEXISTENTE);
        var inexistente = error(handler.execute(ingresoSobreLote("1")));
        when(referencias.estadoSku(TENANT, SKU)).thenReturn(EstadoReferencia.NO_OPERABLE);
        var noOperable = error(handler.execute(ingresoSobreLote("1")));

        assertThat(inexistente.code()).isEqualTo("INV_SKU_NO_ENCONTRADO");
        assertThat(noOperable.code()).isEqualTo("INV_SKU_NO_OPERABLE");
        verify(writePort, never()).findLote(any(), any());
    }

    @Test
    void rejectsAnUnknownLoteOrALoteOfAnotherSku() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.empty());
        var inexistente = error(handler.execute(ingresoSobreLote("1")));
        var base = loteHabilitado();
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(Lote.restore(
                base.id(), base.tenantId(), OTRO_SKU, "L-001", VENCIMIENTO, EstadoLote.HABILITADO,
                null, null, null, AHORA, null, null)));
        var distinto = error(handler.execute(ingresoSobreLote("1")));

        assertThat(inexistente.category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(distinto.code()).isEqualTo("INV_LOTE_SKU_DISTINTO");
    }

    @Test
    void rejectsAnIngresoOfAnExpiredNewLote() {
        when(writePort.findLotePorClave(any(), any(), any(), any())).thenReturn(Optional.empty());
        var vencido = command(null, NUMERO, HOY.minusDays(1), "AJUSTE_INGRESO", "1", "x");

        var failure = error(handler.execute(vencido));

        assertThat(failure.code()).isEqualTo("INV_LOTE_VENCIDO");
        assertThat(failure.category()).isEqualTo(ErrorCategory.VALIDATION);
    }

    @Test
    void rejectsAnIngresoIntoALoteThatDoesNotAdmitIt() {
        when(writePort.findLote(TENANT, LOTE))
                .thenReturn(Optional.of(lote(EstadoLote.INMOVILIZADO_RECALL, VENCIMIENTO)));

        var failure = error(handler.execute(ingresoSobreLote("1")));

        assertThat(failure.code()).isEqualTo("INV_LOTE_NO_ADMITE_INGRESO");
        verify(writePort, never()).registrar(any());
    }

    @Test
    void rejectsASalidaThatWouldLeaveNegativeOrReservedStock() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("5", "2", 1)));

        var failure = error(handler.execute(salidaSobreLote("4")));

        assertThat(failure.code()).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(failure.category()).isEqualTo(ErrorCategory.CONFLICT);
        verify(writePort, never()).registrar(any());
    }

    @Test
    void rejectsAnInvalidQuantity() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.empty());

        var failure = error(handler.execute(ingresoSobreLote("0")));

        assertThat(failure.code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(failure.category()).isEqualTo(ErrorCategory.VALIDATION);
    }

    @Test
    void retriesAfterAConcurrentModificationAndSucceeds() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));
        when(writePort.registrar(any()))
                .thenReturn(RegistroOutcome.MODIFICACION_CONCURRENTE, RegistroOutcome.REGISTRADO);

        assertThat(handler.execute(salidaSobreLote("3")).isSuccess()).isTrue();

        verify(writePort, times(2)).registrar(any());
    }

    @Test
    void givesUpWithAConflictAfterThreeConcurrentModifications() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));
        when(writePort.registrar(any())).thenReturn(RegistroOutcome.MODIFICACION_CONCURRENTE);

        var failure = error(handler.execute(salidaSobreLote("3")));

        assertThat(failure.code()).isEqualTo(InventarioErrors.CONCURRENCIA);
        verify(writePort, times(3)).registrar(any());
    }

    @Test
    void theRegistrationCarriesTheTypeOfMovement() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));

        handler.execute(salidaSobreLote("3"));

        assertThat(registered().tipo()).isEqualTo(TipoMovimiento.AJUSTE_SALIDA);
        assertThat(registered().cantidad()).isEqualByComparingTo("3");
    }

    private static RegistrarMovimientoCommand salidaConClave(String clave, String cantidad) {
        return commandWithKey(LOTE, null, null, "AJUSTE_SALIDA", cantidad, "Merma", clave);
    }

    private void givenStockOfTen() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));
    }

    private MovimientoRegistrado previoDe(RegistroMovimiento registro) {
        return new MovimientoRegistrado(InventarioApplicationMapper.toResult(registro), registro.huella());
    }

    @Test
    void rejectsABlankOrOversizedIdempotencyKey() {
        for (var clave : new String[] {"   ", "k".repeat(201)}) {
            assertThat(error(handler.execute(salidaConClave(clave, "1"))).code())
                    .isEqualTo("INV_IDEMPOTENCY_KEY_INVALID");
        }
        verify(writePort, never()).registrar(any());
    }

    @Test
    void storesTheDerivedBusinessUuidAndTheRequestFingerprintWhenAKeyIsGiven() {
        givenStockOfTen();
        when(writePort.findMovimientoPorBusinessUuid(any(), any())).thenReturn(Optional.empty());

        value(handler.execute(salidaConClave(" clave-1 ", "3")));

        var registro = registered();
        assertThat(registro.businessUuid()).isNotNull();
        assertThat(registro.huella()).isNotBlank();
        verify(writePort).findMovimientoPorBusinessUuid(TENANT, registro.businessUuid());
    }

    @Test
    void withoutAKeyItNeverLooksForAPreviousMovementAndStoresNoBusinessUuid() {
        givenStockOfTen();

        value(handler.execute(salidaSobreLote("3")));

        assertThat(registered().businessUuid()).isNull();
        verify(writePort, never()).findMovimientoPorBusinessUuid(any(), any());
    }

    @Test
    void sameKeyAndSamePayloadReplayTheOriginalResultIgnoringKeyWhitespaceAndDecimalZeros() {
        givenStockOfTen();
        value(handler.execute(salidaConClave("clave-1", "3")));
        var original = registered();
        var previo = previoDe(original);
        when(writePort.findMovimientoPorBusinessUuid(TENANT, original.businessUuid()))
                .thenReturn(Optional.of(previo));

        var repetido = value(handler.execute(salidaConClave(" clave-1 ", "3.000")));

        assertThat(repetido).isEqualTo(previo.resultado());
        verify(writePort, times(1)).registrar(any());
    }

    @Test
    void sameKeyWithADifferentPayloadIsAConflict() {
        givenStockOfTen();
        value(handler.execute(salidaConClave("clave-1", "3")));
        var original = registered();
        when(writePort.findMovimientoPorBusinessUuid(TENANT, original.businessUuid()))
                .thenReturn(Optional.of(previoDe(original)));

        var failure = error(handler.execute(salidaConClave("clave-1", "4")));

        assertThat(failure.code()).isEqualTo("INV_IDEMPOTENCY_CONFLICT");
        assertThat(failure.category()).isEqualTo(ErrorCategory.CONFLICT);
        verify(writePort, times(1)).registrar(any());
    }

    @Test
    void aConcurrentDuplicateRetriesAndReplaysTheWinnersResult() {
        givenStockOfTen();
        value(handler.execute(salidaConClave("clave-1", "3")));
        var ganador = registered();
        var previo = previoDe(ganador);
        when(writePort.findMovimientoPorBusinessUuid(TENANT, ganador.businessUuid()))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(previo));
        when(writePort.registrar(any())).thenReturn(RegistroOutcome.MODIFICACION_CONCURRENTE);

        var repetido = value(handler.execute(salidaConClave("clave-1", "3")));

        assertThat(repetido).isEqualTo(previo.resultado());
    }

    private static RegistrarMovimientoCommand compra(String tipo, DocumentoOrigen origen) {
        return new RegistrarMovimientoCommand(
                TENANT, ALMACEN, SKU, null, NUMERO, VENCIMIENTO, tipo, new BigDecimal("10"),
                "Recepcion de compra", ACTOR_ID, null, origen);
    }

    @Test
    void aPurchaseIngresoCarriesItsOriginDocumentIntoTheRegistration() {
        var origen = new DocumentoOrigen("RECEPCION_COMPRA", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(writePort.findLotePorClave(TENANT, SKU, NUMERO, VENCIMIENTO)).thenReturn(Optional.empty());
        when(writePort.findPosicion(any(), any(), any())).thenReturn(Optional.empty());

        var result = value(handler.execute(compra("INGRESO_COMPRA", origen)));

        assertThat(result.tipo()).isEqualTo("INGRESO_COMPRA");
        assertThat(result.naturaleza()).isEqualTo("E");
        assertThat(registered().origen()).isEqualTo(origen);
        assertThat(registered().tipo()).isEqualTo(TipoMovimiento.INGRESO_COMPRA);
    }

    @Test
    void aManualTypeWithAnOriginAndAPurchaseTypeWithoutOneAreRejected() {
        var origen = new DocumentoOrigen("RECEPCION_COMPRA", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        assertThat(error(handler.execute(compra("AJUSTE_INGRESO", origen))).code())
                .isEqualTo("INV_TIPO_MOVIMIENTO_INVALIDO");
        assertThat(error(handler.execute(compra("INGRESO_COMPRA", null))).code())
                .isEqualTo("INV_TIPO_MOVIMIENTO_INVALIDO");
        verify(writePort, never()).registrar(any());
    }

    private static RegistrarMovimientoCommand salidaVenta() {
        var origen = new DocumentoOrigen("VENTA", UUID.randomUUID(), UUID.randomUUID(), null);
        return new RegistrarMovimientoCommand(
                TENANT, ALMACEN, SKU, LOTE, null, null, "SALIDA_VENTA", new BigDecimal("3"), "Venta", ACTOR_ID,
                null, origen);
    }

    @Test
    void aSalidaVentaDiscountsStockFromASellableLote() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(loteHabilitado()));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));

        var result = value(handler.execute(salidaVenta()));

        assertThat(result.tipo()).isEqualTo("SALIDA_VENTA");
        assertThat(result.naturaleza()).isEqualTo("S");
        assertThat(result.stockPosterior()).isEqualByComparingTo("7");
    }

    @Test
    void aSalidaVentaOnANonSellableLoteIsRejectedWithoutRegistering() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(lote(EstadoLote.BLOQUEADO, VENCIMIENTO)));

        assertThat(error(handler.execute(salidaVenta())).code()).isEqualTo("INV_LOTE_NO_VENDIBLE");
        verify(writePort, never()).registrar(any());
    }

    @Test
    void aSalidaVentaOnAnEnabledButExpiredLoteIsRejectedWithoutRegistering() {
        when(writePort.findLote(TENANT, LOTE))
                .thenReturn(Optional.of(lote(EstadoLote.HABILITADO, HOY.minusDays(1))));

        assertThat(error(handler.execute(salidaVenta())).code()).isEqualTo("INV_LOTE_NO_VENDIBLE");
        verify(writePort, never()).registrar(any());
    }

    @Test
    void aSalidaVentaOnAnEnabledLoteExpiringTodayIsAccepted() {
        when(writePort.findLote(TENANT, LOTE)).thenReturn(Optional.of(lote(EstadoLote.HABILITADO, HOY)));
        when(writePort.findPosicion(TENANT, ALMACEN, LOTE)).thenReturn(Optional.of(posicion("10", "0", 1)));

        assertThat(value(handler.execute(salidaVenta())).stockPosterior()).isEqualByComparingTo("7");
    }
}
