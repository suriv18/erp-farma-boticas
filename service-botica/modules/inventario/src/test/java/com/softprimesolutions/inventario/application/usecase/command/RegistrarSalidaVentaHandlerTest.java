package com.softprimesolutions.inventario.application.usecase.command;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.HOY;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.MovimientoRegistrado;
import com.softprimesolutions.inventario.application.port.out.TransaccionPort;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class RegistrarSalidaVentaHandlerTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID LINEA = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID LOTE_A = UUID.fromString("c1c1c1c1-c1c1-4c1c-8c1c-c1c1c1c1c1c1");
    private static final UUID LOTE_B = UUID.fromString("c2c2c2c2-c2c2-4c2c-8c2c-c2c2c2c2c2c2");
    private static final String CLAVE = "venta-1";

    private final InventarioWritePort writePort = mock(InventarioWritePort.class);
    private final TransaccionEspia transaccion = new TransaccionEspia();
    private final List<Boolean> accesosEnTransaccion = new ArrayList<>();
    private final List<RegistrarMovimientoCommand> enviados = new ArrayList<>();
    private final RegistrarMovimientoUseCase registrarMovimiento = command -> {
        accesosEnTransaccion.add(transaccion.activa);
        enviados.add(command);
        return Result.success(movimiento(command.loteId(), command.cantidad().toPlainString(), "0"));
    };
    private final RegistrarSalidaVentaHandler handler =
            new RegistrarSalidaVentaHandler(writePort, registrarMovimiento, transaccion, () -> AHORA);

    private static final class TransaccionEspia implements TransaccionPort {

        private boolean activa;
        private int ejecuciones;
        private Result<?, ApplicationError> resultado;

        @Override
        public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
            ejecuciones++;
            activa = true;
            var obtenido = trabajo.get();
            activa = false;
            resultado = obtenido;
            return obtenido;
        }
    }

    private static PosicionInventario posicion(UUID lote, String fisica, String reservada) {
        return PosicionInventario.restore(
                new PosicionId(UUID.randomUUID()), lote, ALMACEN, SKU, new BigDecimal(fisica),
                new BigDecimal(reservada), 0L);
    }

    private static MovimientoResult movimiento(UUID lote, String cantidad, String posterior) {
        return new MovimientoResult(
                UUID.randomUUID(), UUID.randomUUID(), lote, "SALIDA_VENTA", "S", new BigDecimal(cantidad),
                new BigDecimal("10"), new BigDecimal(posterior), AHORA);
    }

    private static RegistrarSalidaVentaCommand salida(String cantidad, String clave) {
        return new RegistrarSalidaVentaCommand(
                TENANT, ALMACEN, SKU, cantidad == null ? null : new BigDecimal(cantidad), VENTA, LINEA, ACTOR_ID,
                clave);
    }

    private static List<MovimientoResult> value(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static ApplicationError error(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    private void fefo(PosicionInventario... posiciones) {
        when(writePort.findPosicionesVendiblesFefo(TENANT, ALMACEN, SKU, HOY)).thenAnswer(invocation -> {
            accesosEnTransaccion.add(transaccion.activa);
            return List.of(posiciones);
        });
    }

    @Test
    void theFefoReadAndEveryTramoRunInsideASingleTransaction() {
        fefo(posicion(LOTE_A, "5", "0"), posicion(LOTE_B, "10", "0"));

        value(handler.execute(salida("6", CLAVE)));

        assertThat(transaccion.ejecuciones).isEqualTo(1);
        assertThat(accesosEnTransaccion).containsExactly(true, true, true);
        assertThat(transaccion.resultado.isSuccess()).isTrue();
    }

    @Test
    void returnsTheOutcomeDecidedByTheTransaction() {
        var sinTrabajo = new RegistrarSalidaVentaHandler(writePort, registrarMovimiento, new TransaccionPort() {
            @Override
            public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
                return Result.failure(CONFLICTO);
            }
        }, () -> AHORA);

        assertThat(error(sinTrabajo.execute(salida("1", CLAVE)))).isSameAs(CONFLICTO);
        verify(writePort, never()).findPosicionesVendiblesFefo(any(), any(), any(), any());
        assertThat(enviados).isEmpty();
    }

    @Test
    void consumesTheEarliestExpiryFirstAndStopsOnceTheQuantityIsCovered() {
        fefo(posicion(LOTE_A, "5", "0"), posicion(LOTE_B, "10", "0"));

        var movimientos = value(handler.execute(salida("3", CLAVE)));

        assertThat(movimientos).extracting(MovimientoResult::loteId).containsExactly(LOTE_A);
        assertThat(enviados).hasSize(1);
        var command = enviados.getFirst();
        assertThat(command.tipo()).isEqualTo("SALIDA_VENTA");
        assertThat(command.loteId()).isEqualTo(LOTE_A);
        assertThat(command.cantidad()).isEqualByComparingTo("3");
        assertThat(command.motivo()).isEqualTo(RegistrarSalidaVentaHandler.MOTIVO_VENTA);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.skuId()).isEqualTo(SKU);
        assertThat(command.idempotencyKey()).isEqualTo(IdempotencyKeys.tramo(CLAVE, 1));
        assertThat(command.origen().tipo()).isEqualTo(RegistrarSalidaVentaHandler.DOCUMENTO_VENTA);
        assertThat(command.origen().documentoId()).isEqualTo(VENTA);
        assertThat(command.origen().lineaId()).isEqualTo(LINEA);
        assertThat(command.origen().proveedorId()).isNull();
    }

    @Test
    void splitsTheQuantityAcrossLotesUsingOnlyTheAvailableOfEach() {
        fefo(posicion(LOTE_A, "5", "1"), posicion(LOTE_B, "10", "0"));

        var movimientos = value(handler.execute(salida("6", CLAVE)));

        assertThat(movimientos).extracting(MovimientoResult::loteId).containsExactly(LOTE_A, LOTE_B);
        assertThat(enviados).extracting(RegistrarMovimientoCommand::cantidad)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("4"), new BigDecimal("2"));
        assertThat(enviados).extracting(RegistrarMovimientoCommand::idempotencyKey)
                .containsExactly(IdempotencyKeys.tramo(CLAVE, 1), IdempotencyKeys.tramo(CLAVE, 2));
    }

    @Test
    void failsWithoutMovingStockWhenTheAvailableDoesNotCoverTheQuantity() {
        fefo(posicion(LOTE_A, "5", "1"), posicion(LOTE_B, "2", "0"));

        assertThat(error(handler.execute(salida("7", CLAVE))).code()).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(enviados).isEmpty();
    }

    @Test
    void failsWhenThereAreNoSellablePositions() {
        assertThat(error(handler.execute(salida("1", CLAVE))).code()).isEqualTo("INV_STOCK_INSUFICIENTE");
        assertThat(enviados).isEmpty();
    }

    @Test
    void stopsAtTheFirstMovementThatFailsAndPassesItsErrorThrough() {
        fefo(posicion(LOTE_A, "5", "0"), posicion(LOTE_B, "10", "0"));
        var llamadas = new ArrayList<UUID>();
        var fallaElSegundo = new RegistrarSalidaVentaHandler(writePort, command -> {
            llamadas.add(command.loteId());
            return llamadas.size() == 1
                    ? Result.success(movimiento(command.loteId(), "5", "0"))
                    : Result.failure(CONFLICTO);
        }, transaccion, () -> AHORA);

        var failure = error(fallaElSegundo.execute(salida("8", CLAVE)));

        assertThat(failure).isSameAs(CONFLICTO);
        assertThat(llamadas).containsExactly(LOTE_A, LOTE_B);
        assertThat(transaccion.ejecuciones).isEqualTo(1);
        assertThat(transaccion.resultado.isFailure()).isTrue();
    }

    @Test
    void aRetryReturnsTheMovementsAlreadyRegisteredWithoutQueryingOrMovingStock() {
        var primero = movimiento(LOTE_A, "5", "0");
        var segundo = movimiento(LOTE_B, "1", "9");
        when(writePort.findMovimientoPorBusinessUuid(
                TENANT, IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(CLAVE, 1))))
                .thenReturn(Optional.of(new MovimientoRegistrado(primero, "h1")));
        when(writePort.findMovimientoPorBusinessUuid(
                TENANT, IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(CLAVE, 2))))
                .thenReturn(Optional.of(new MovimientoRegistrado(segundo, "h2")));

        var movimientos = value(handler.execute(salida("6", CLAVE)));

        assertThat(movimientos).containsExactly(primero, segundo);
        assertThat(enviados).isEmpty();
        verify(writePort, never()).findPosicionesVendiblesFefo(any(), any(), any(), any());
    }

    @Test
    void aRetryWithADifferentQuantityIsAnIdempotencyConflict() {
        when(writePort.findMovimientoPorBusinessUuid(
                TENANT, IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(CLAVE, 1))))
                .thenReturn(Optional.of(new MovimientoRegistrado(movimiento(LOTE_A, "5", "0"), "h1")));

        assertThat(error(handler.execute(salida("6", CLAVE))).code()).isEqualTo("INV_IDEMPOTENCY_CONFLICT");
        assertThat(enviados).isEmpty();
    }

    @Test
    void theIdempotencyKeyIsRequiredAndAtMostTwoHundredCharacters() {
        assertThat(error(handler.execute(salida("1", null))).code()).isEqualTo("INV_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(salida("1", "  "))).code()).isEqualTo("INV_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(salida("1", "x".repeat(201)))).code())
                .isEqualTo("INV_IDEMPOTENCY_KEY_INVALID");
        assertThat(enviados).isEmpty();
    }

    @Test
    void theQuantityMustBePositiveWithAtMostFourDecimals() {
        assertThat(error(handler.execute(salida(null, CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(salida("0", CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(salida("-1", CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(error(handler.execute(salida("1.00001", CLAVE))).code()).isEqualTo("INV_CANTIDAD_INVALIDA");
        assertThat(enviados).isEmpty();
        assertThat(transaccion.ejecuciones).isZero();
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new RegistrarSalidaVentaHandler(null, registrarMovimiento, transaccion, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new RegistrarSalidaVentaHandler(writePort, null, transaccion, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new RegistrarSalidaVentaHandler(writePort, registrarMovimiento, null, () -> AHORA));
        assertThatNullPointerException().isThrownBy(
                () -> new RegistrarSalidaVentaHandler(writePort, registrarMovimiento, transaccion, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
