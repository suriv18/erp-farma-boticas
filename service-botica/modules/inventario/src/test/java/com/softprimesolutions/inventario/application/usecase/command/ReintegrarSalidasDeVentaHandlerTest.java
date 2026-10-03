package com.softprimesolutions.inventario.application.usecase.command;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.OTRO_SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.SalidaDeVenta;
import com.softprimesolutions.inventario.application.port.out.TransaccionPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReintegrarSalidasDeVentaHandlerTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID LOTE_A = UUID.fromString("c1c1c1c1-c1c1-4c1c-8c1c-c1c1c1c1c1c1");
    private static final UUID LOTE_B = UUID.fromString("c2c2c2c2-c2c2-4c2c-8c2c-c2c2c2c2c2c2");
    private static final UUID LOTE_C = UUID.fromString("c3c3c3c3-c3c3-4c3c-8c3c-c3c3c3c3c3c3");

    private final InventarioWritePort writePort = mock(InventarioWritePort.class);
    private final List<RegistrarMovimientoCommand> enviados = new ArrayList<>();
    private final RegistrarMovimientoUseCase registrarMovimiento = command -> {
        enviados.add(command);
        return Result.success(new MovimientoResult(
                UUID.randomUUID(), UUID.randomUUID(), command.loteId(), "ANULACION_VENTA", "E", command.cantidad(),
                new BigDecimal("1"), new BigDecimal("9"), AHORA));
    };
    private final TransaccionPort transaccion = new TransaccionPort() {
        @Override
        public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
            return trabajo.get();
        }
    };
    private final ReintegrarSalidasDeVentaHandler handler =
            new ReintegrarSalidasDeVentaHandler(writePort, registrarMovimiento, transaccion);

    private static SalidaDeVenta salida(UUID movimiento, UUID sku, UUID lote, String numeroLote, LocalDate vence, String cantidad) {
        return new SalidaDeVenta(movimiento, ALMACEN, sku, lote, numeroLote, vence, new BigDecimal(cantidad));
    }

    private static ReintegrarSalidasDeVentaCommand command() {
        return new ReintegrarSalidasDeVentaCommand(TENANT, VENTA, ACTOR_ID);
    }

    private static List<MovimientoResult> value(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static ApplicationError error(Result<List<MovimientoResult>, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void revertsEachSalidaWithItsOwnStableKeyAndTheSaleAsOriginDocument() {
        var original = UUID.fromString("abababab-abab-4bab-8bab-abababababab");
        when(writePort.findSalidasDeVenta(TENANT, VENTA)).thenReturn(List.of(
                salida(original, SKU, LOTE_A, "L-A", LocalDate.of(2027, 1, 1), "3")));

        var movimientos = value(handler.execute(command()));

        assertThat(movimientos).extracting(MovimientoResult::loteId).containsExactly(LOTE_A);
        var enviado = enviados.getFirst();
        assertThat(enviado.tipo()).isEqualTo("ANULACION_VENTA");
        assertThat(enviado.tenantId()).isEqualTo(TENANT);
        assertThat(enviado.almacenId()).isEqualTo(ALMACEN);
        assertThat(enviado.skuId()).isEqualTo(SKU);
        assertThat(enviado.loteId()).isEqualTo(LOTE_A);
        assertThat(enviado.cantidad()).isEqualByComparingTo("3");
        assertThat(enviado.motivo()).isEqualTo(ReintegrarSalidasDeVentaHandler.MOTIVO_ANULACION);
        assertThat(enviado.actorId()).isEqualTo(ACTOR_ID);
        assertThat(enviado.idempotencyKey()).isEqualTo(IdempotencyKeys.reverso(original));
        assertThat(enviado.origen().tipo()).isEqualTo(ReintegrarSalidasDeVentaHandler.DOCUMENTO_ANULACION);
        assertThat(enviado.origen().documentoId()).isEqualTo(VENTA);
        assertThat(enviado.origen().lineaId()).isEqualTo(original);
        assertThat(enviado.origen().proveedorId()).isNull();
    }

    @Test
    void appliesTheReversalsInSkuThenExpiryThenLoteNumberOrder() {
        when(writePort.findSalidasDeVenta(TENANT, VENTA)).thenReturn(List.of(
                salida(UUID.randomUUID(), OTRO_SKU, LOTE_C, "L-C", LocalDate.of(2027, 1, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_B, "L-B", LocalDate.of(2027, 6, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_A, "L-A", LocalDate.of(2027, 6, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_C, "L-Z", LocalDate.of(2026, 12, 1), "1")));

        value(handler.execute(command()));

        assertThat(enviados).extracting(RegistrarMovimientoCommand::loteId)
                .containsExactly(LOTE_C, LOTE_A, LOTE_B, LOTE_C);
        assertThat(enviados).extracting(RegistrarMovimientoCommand::skuId)
                .containsExactly(SKU, SKU, SKU, OTRO_SKU);
    }

    @Test
    void aSaleWithoutSalidasIsNotFound() {
        assertThat(error(handler.execute(command())).code()).isEqualTo("INV_SALIDAS_NO_ENCONTRADAS");
        assertThat(enviados).isEmpty();
    }

    @Test
    void stopsAtTheFirstReversalThatFailsAndPassesItsErrorThrough() {
        when(writePort.findSalidasDeVenta(TENANT, VENTA)).thenReturn(List.of(
                salida(UUID.randomUUID(), SKU, LOTE_A, "L-A", LocalDate.of(2027, 1, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_B, "L-B", LocalDate.of(2027, 2, 1), "1"),
                salida(UUID.randomUUID(), SKU, LOTE_C, "L-C", LocalDate.of(2027, 3, 1), "1")));
        var llamadas = new ArrayList<UUID>();
        var fallaElSegundo = new ReintegrarSalidasDeVentaHandler(writePort, command -> {
            llamadas.add(command.loteId());
            return llamadas.size() == 2
                    ? Result.failure(CONFLICTO)
                    : Result.success(new MovimientoResult(
                            UUID.randomUUID(), UUID.randomUUID(), command.loteId(), "ANULACION_VENTA", "E",
                            command.cantidad(), new BigDecimal("1"), new BigDecimal("2"), AHORA));
        }, transaccion);

        var failure = error(fallaElSegundo.execute(command()));

        assertThat(failure).isSameAs(CONFLICTO);
        assertThat(llamadas).containsExactly(LOTE_A, LOTE_B);
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(
                () -> new ReintegrarSalidasDeVentaHandler(null, registrarMovimiento, transaccion));
        assertThatNullPointerException().isThrownBy(
                () -> new ReintegrarSalidasDeVentaHandler(writePort, null, transaccion));
        assertThatNullPointerException().isThrownBy(
                () -> new ReintegrarSalidasDeVentaHandler(writePort, registrarMovimiento, null));
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
    }
}
