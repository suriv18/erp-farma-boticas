package com.softprimesolutions.inventario.api.facade;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.softprimesolutions.inventario.api.IngresoCompraSolicitud;
import com.softprimesolutions.inventario.api.IngresoInventarioApi;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IngresoInventarioFacadeTest {

    private static final UUID RECEPCION = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID LINEA = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID PROVEEDOR = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

    private static IngresoCompraSolicitud solicitud() {
        return new IngresoCompraSolicitud(
                TENANT, ALMACEN, SKU, "L-001", VENCIMIENTO, new BigDecimal("5"), RECEPCION, LINEA, PROVEEDOR,
                ACTOR_ID, "recepcion:1");
    }

    @Test
    void translatesAPurchaseIngresoIntoAMovementCommandWithItsOriginDocument() {
        var received = new AtomicReference<RegistrarMovimientoCommand>();
        var facade = new IngresoInventarioFacade(command -> {
            received.set(command);
            return Result.success(new MovimientoResult(
                    UUID.randomUUID(), POSICION, LOTE, "INGRESO_COMPRA", "E", new BigDecimal("5"),
                    new BigDecimal("2"), new BigDecimal("7"), AHORA));
        });

        var result = facade.registrarIngresoCompra(solicitud());

        var command = received.get();
        assertThat(command.tipo()).isEqualTo("INGRESO_COMPRA");
        assertThat(command.loteId()).isNull();
        assertThat(command.numeroLote()).isEqualTo("L-001");
        assertThat(command.fechaVencimiento()).isEqualTo(VENCIMIENTO);
        assertThat(command.cantidad()).isEqualByComparingTo("5");
        assertThat(command.motivo()).isEqualTo(IngresoInventarioFacade.MOTIVO_RECEPCION_COMPRA);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.idempotencyKey()).isEqualTo("recepcion:1");
        assertThat(command.origen().tipo()).isEqualTo(IngresoInventarioFacade.DOCUMENTO_RECEPCION_COMPRA);
        assertThat(command.origen().documentoId()).isEqualTo(RECEPCION);
        assertThat(command.origen().lineaId()).isEqualTo(LINEA);
        assertThat(command.origen().proveedorId()).isEqualTo(PROVEEDOR);
        var registrado = result.fold(value -> value, error -> { throw new AssertionError(error); });
        assertThat(registrado.loteId()).isEqualTo(LOTE);
        assertThat(registrado.posicionId()).isEqualTo(POSICION);
        assertThat(registrado.stockAnterior()).isEqualByComparingTo("2");
        assertThat(registrado.stockPosterior()).isEqualByComparingTo("7");
    }

    @Test
    void passesTheMovementErrorThrough() {
        var facade = new IngresoInventarioFacade(command -> Result.failure(CONFLICTO));

        var result = facade.registrarIngresoCompra(solicitud());

        ApplicationError error = result.fold(value -> null, failure -> failure);
        assertThat(error).isSameAs(CONFLICTO);
    }

    @Test
    void requiresItsCollaboratorAndTheRequest() {
        assertThatNullPointerException().isThrownBy(() -> new IngresoInventarioFacade(null));
        var facade = new IngresoInventarioFacade(command -> Result.failure(CONFLICTO));
        assertThatNullPointerException().isThrownBy(() -> facade.registrarIngresoCompra(null));
    }

    @Test
    void exposesTheConcurrencyCodeOfTheInventoryErrors() {
        assertThat(IngresoInventarioApi.CODIGO_CONCURRENCIA).isEqualTo(InventarioErrors.CONCURRENCIA);
    }
}
