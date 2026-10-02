package com.softprimesolutions.compras.application.usecase.command;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.CONFLICTO;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.LOTE;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.VENCIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.error;
import static com.softprimesolutions.compras.ComprasFixtures.ordenEmitida;
import static com.softprimesolutions.compras.ComprasFixtures.value;
import static com.softprimesolutions.compras.ComprasResultFixtures.recepcionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.compras.application.dto.command.ItemRecepcionInput;
import com.softprimesolutions.compras.application.dto.command.RegistrarRecepcionCommand;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort.IngresoRegistrado;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort.IngresoSolicitado;
import com.softprimesolutions.compras.application.port.out.NumeracionPort;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.application.port.out.RecepcionWritePort;
import com.softprimesolutions.compras.application.port.out.RecepcionWritePort.RecepcionExistente;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort.AlmacenRef;
import com.softprimesolutions.compras.application.port.out.TransaccionPort;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.compras.domain.model.Recepcion;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrarRecepcionHandlerTest {

    private final OrdenCompraWritePort ordenes = mock(OrdenCompraWritePort.class);
    private final RecepcionWritePort recepciones = mock(RecepcionWritePort.class);
    private final ComprasReadPort lecturas = mock(ComprasReadPort.class);
    private final ReferenciasComprasPort referencias = mock(ReferenciasComprasPort.class);
    private final IngresoInventarioPort inventario = mock(IngresoInventarioPort.class);
    private final NumeracionPort numeracion = mock(NumeracionPort.class);
    private final TransaccionPort transaccion = new TransaccionPort() {
        @Override
        public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
            return trabajo.get();
        }
    };
    private final RegistrarRecepcionHandler handler = new RegistrarRecepcionHandler(
            ordenes, recepciones, lecturas, referencias, inventario, numeracion, transaccion, UUID::randomUUID,
            () -> AHORA);

    @BeforeEach
    void everythingIsValidByDefault() {
        when(ordenes.findByIdParaActualizar(TENANT, ORDEN)).thenReturn(Optional.of(ordenEmitida()));
        when(referencias.almacen(TENANT, ALMACEN)).thenReturn(Optional.of(new AlmacenRef(ALMACEN, ESTABLECIMIENTO, true)));
        when(numeracion.siguienteNumeroRecepcion()).thenReturn("REC-2026-000001");
        when(recepciones.insertar(any(), any(), any())).thenReturn(GuardadoOutcome.GUARDADO);
        when(inventario.ingresar(any())).thenReturn(Result.success(new IngresoRegistrado(UUID.randomUUID(), LOTE, dec("5"))));
        when(ordenes.actualizarEstado(any(), any())).thenReturn(true);
        when(lecturas.findRecepcion(eq(TENANT), any(UUID.class))).thenReturn(Optional.of(recepcionResult()));
    }

    private static ItemRecepcionInput item(String lote, String recibida, String rechazada) {
        return new ItemRecepcionInput(
                1, lote, null, VENCIMIENTO, dec(recibida), rechazada == null ? null : dec(rechazada), "Envase danado",
                dec("5.5"), null);
    }

    private static RegistrarRecepcionCommand command(String clave, ItemRecepcionInput... items) {
        return new RegistrarRecepcionCommand(
                TENANT, ACTOR_ID, clave, ORDEN, ALMACEN, null, "F001", "123", "T001-45", null, dec("22.5"), null,
                "Recepcion", List.of(items));
    }

    private Recepcion savedRecepcion() {
        var captor = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones).insertar(captor.capture(), any(), any());
        return captor.getValue();
    }

    @Test
    void receivesTheMerchandiseIngressesTheAcceptedStockAndUpdatesTheOrder() {
        var result = value(handler.execute(command(null, item("LOTE-1", "6", "1"))));

        assertThat(result).isEqualTo(recepcionResult());
        var recepcion = savedRecepcion();
        assertThat(recepcion.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(recepcion.datos().numero()).isEqualTo("REC-2026-000001");
        assertThat(recepcion.datos().documentoProveedorTipo()).isEqualTo("01");
        assertThat(recepcion.recibidoPor()).isEqualTo(ACTOR_ID.toString());
        var ingreso = ArgumentCaptor.forClass(IngresoSolicitado.class);
        verify(inventario).ingresar(ingreso.capture());
        assertThat(ingreso.getValue()).isEqualTo(new IngresoSolicitado(
                TENANT, ALMACEN, SKU, "LOTE-1", VENCIMIENTO, dec("5"), recepcion.id(), recepcion.lineas().get(0).id(),
                PROVEEDOR, ACTOR_ID, "recepcion:" + recepcion.id() + ":1"));
        var orden = ArgumentCaptor.forClass(OrdenCompra.class);
        verify(ordenes).actualizarEstado(orden.capture(), eq(EstadoOrdenCompra.EMITIDA));
        assertThat(orden.getValue().estado()).isEqualTo(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA);
        assertThat(orden.getValue().lineas().get(0).cantidadRecibida()).isEqualByComparingTo("5");
    }

    @Test
    void aFullyRejectedLineDoesNotIngressStock() {
        value(handler.execute(command(null, item("LOTE-1", "2", "2"))));

        verify(inventario, never()).ingresar(any());
        verify(ordenes).actualizarEstado(any(), any());
    }

    @Test
    void withoutAKeyTheReceptionIsStoredWithoutBusinessUuid() {
        value(handler.execute(command(null, item("LOTE-1", "6", "1"))));

        verify(recepciones, never()).findPorBusinessUuid(any(), any());
        verify(recepciones).insertar(any(), isNull(), any());
    }

    @Test
    void withAKeyTheReceptionStoresTheDerivedBusinessUuidAndTheRequestFingerprint() {
        value(handler.execute(command(" clave-1 ", item("LOTE-1", "6", "1"))));

        var esperado = UUID.nameUUIDFromBytes("recepcion:clave-1".getBytes(StandardCharsets.UTF_8));
        verify(recepciones).findPorBusinessUuid(TENANT, esperado);
        verify(recepciones).insertar(any(), eq(esperado), any(String.class));
    }

    @Test
    void sameKeyAndSamePayloadReplayTheStoredReceptionWithoutReceivingAgain() {
        value(handler.execute(command("clave-1", item("LOTE-1", "6", "1"))));
        var huella = ArgumentCaptor.forClass(String.class);
        verify(recepciones).insertar(any(), any(), huella.capture());
        when(recepciones.findPorBusinessUuid(eq(TENANT), any()))
                .thenReturn(Optional.of(new RecepcionExistente(RECEPCION, huella.getValue())));

        var repetido = value(handler.execute(command("clave-1", item("LOTE-1", "6", "1"))));

        assertThat(repetido).isEqualTo(recepcionResult());
        verify(recepciones, times(1)).insertar(any(), any(), any());
        verify(inventario, times(1)).ingresar(any());
    }

    @Test
    void sameKeyWithADifferentPayloadIsAConflict() {
        when(recepciones.findPorBusinessUuid(eq(TENANT), any()))
                .thenReturn(Optional.of(new RecepcionExistente(RECEPCION, "otra-huella")));

        var error = error(handler.execute(command("clave-1", item("LOTE-1", "6", "1"))));

        assertThat(error.code()).isEqualTo("COM_IDEMPOTENCY_CONFLICT");
        verify(recepciones, never()).insertar(any(), any(), any());
    }

    @Test
    void rejectsABlankOrOversizedIdempotencyKey() {
        assertThat(error(handler.execute(command("   ", item("LOTE-1", "6", "1")))).code())
                .isEqualTo("COM_IDEMPOTENCY_KEY_INVALID");
        assertThat(error(handler.execute(command("x".repeat(201), item("LOTE-1", "6", "1")))).code())
                .isEqualTo("COM_IDEMPOTENCY_KEY_INVALID");
        assertThat(value(handler.execute(command("x".repeat(200), item("LOTE-1", "6", "1"))))).isNotNull();
    }

    @Test
    void rejectsAnUnknownOrder() {
        when(ordenes.findByIdParaActualizar(TENANT, ORDEN)).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command(null, item("LOTE-1", "6", "1")))).code())
                .isEqualTo("COM_ORDEN_NO_ENCONTRADA");
    }

    @Test
    void rejectsAnUnknownInactiveOrForeignWarehouse() {
        when(referencias.almacen(TENANT, ALMACEN)).thenReturn(Optional.empty());
        assertThat(error(handler.execute(command(null, item("LOTE-1", "6", "1")))).code())
                .isEqualTo("COM_ALMACEN_NO_ENCONTRADO");

        when(referencias.almacen(TENANT, ALMACEN)).thenReturn(Optional.of(new AlmacenRef(ALMACEN, ESTABLECIMIENTO, false)));
        assertThat(error(handler.execute(command(null, item("LOTE-1", "6", "1")))).code())
                .isEqualTo("COM_ALMACEN_NO_OPERABLE");

        when(referencias.almacen(TENANT, ALMACEN)).thenReturn(Optional.of(new AlmacenRef(ALMACEN, UUID.randomUUID(), true)));
        assertThat(error(handler.execute(command(null, item("LOTE-1", "6", "1")))).code())
                .isEqualTo("COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO");
        verify(recepciones, never()).insertar(any(), any(), any());
    }

    @Test
    void reportsTheDomainErrorsOfTheReception() {
        var error = error(handler.execute(command(null, item("LOTE-1", "12", "0"))));

        assertThat(error.code()).isEqualTo("COM_RECEPCION_EXCEDE_PENDIENTE");
        verify(recepciones, never()).insertar(any(), any(), any());
        verify(inventario, never()).ingresar(any());
    }

    @Test
    void anInventoryFailureStopsTheReceptionBeforeUpdatingTheOrder() {
        when(inventario.ingresar(any())).thenReturn(Result.failure(CONFLICTO));

        var error = error(handler.execute(command(null, item("LOTE-1", "6", "1"))));

        assertThat(error).isSameAs(CONFLICTO);
        verify(ordenes, never()).actualizarEstado(any(), any());
    }

    @Test
    void stopsAtTheFirstInventoryFailureWhenSeveralItemsEnterStock() {
        when(inventario.ingresar(any())).thenReturn(Result.failure(CONFLICTO));

        var error = error(handler.execute(command(null, item("LOTE-1", "2", "0"), item("LOTE-2", "2", "0"))));

        assertThat(error).isSameAs(CONFLICTO);
        verify(inventario, times(1)).ingresar(any());
    }

    @Test
    void retriesTheWholeReceptionWhenInventoryReportsAConcurrentModification() {
        when(inventario.ingresar(any()))
                .thenReturn(Result.failure(ComprasErrors.modificacionConcurrente()))
                .thenReturn(Result.success(new IngresoRegistrado(UUID.randomUUID(), LOTE, dec("5"))));

        var result = value(handler.execute(command(null, item("LOTE-1", "6", "1"))));

        assertThat(result).isEqualTo(recepcionResult());
        verify(ordenes, times(2)).findByIdParaActualizar(TENANT, ORDEN);
    }

    @Test
    void givesUpWhenTheBusinessUuidKeepsColliding() {
        when(recepciones.insertar(any(), any(), any())).thenReturn(GuardadoOutcome.DUPLICADO);

        var error = error(handler.execute(command("clave-1", item("LOTE-1", "6", "1"))));

        assertThat(error.code()).isEqualTo("COM_MODIFICACION_CONCURRENTE");
        verify(ordenes, times(3)).findByIdParaActualizar(TENANT, ORDEN);
    }

    @Test
    void givesUpAfterThreeConcurrentModificationsOfTheOrder() {
        when(ordenes.actualizarEstado(any(), any())).thenReturn(false);

        var error = error(handler.execute(command(null, item("LOTE-1", "6", "1"))));

        assertThat(error.code()).isEqualTo("COM_MODIFICACION_CONCURRENTE");
        verify(ordenes, times(3)).actualizarEstado(any(), any());
    }

    @Test
    void reportsAMissingStoredReceptionAfterCommit() {
        when(lecturas.findRecepcion(eq(TENANT), any(UUID.class))).thenReturn(Optional.empty());

        assertThat(error(handler.execute(command(null, item("LOTE-1", "6", "1")))).code())
                .isEqualTo("COM_RECEPCION_NO_ENCONTRADA");
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatNullPointerException().isThrownBy(() -> handler.execute(null));
        assertThatNullPointerException().isThrownBy(() -> new RegistrarRecepcionHandler(
                null, recepciones, lecturas, referencias, inventario, numeracion, transaccion, UUID::randomUUID,
                () -> AHORA));
    }
}
