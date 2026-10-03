package com.softprimesolutions.inventario.api.facade;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SalidaInventarioFacadeTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID LINEA = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");

    private static SalidaVentaSolicitud solicitud() {
        return new SalidaVentaSolicitud(
                TENANT, ALMACEN, SKU, new BigDecimal("6"), VENTA, LINEA, ACTOR_ID, "venta:1");
    }

    @Test
    void translatesTheRequestIntoACommandAndTheMovementsIntoConsumedLotes() {
        var received = new AtomicReference<RegistrarSalidaVentaCommand>();
        var movimientoId = UUID.randomUUID();
        var facade = new SalidaInventarioFacade(command -> {
            received.set(command);
            return Result.success(List.of(new MovimientoResult(
                    movimientoId, POSICION, LOTE, "SALIDA_VENTA", "S", new BigDecimal("6"),
                    new BigDecimal("10"), new BigDecimal("4"), AHORA)));
        });

        var registrada = facade.registrarSalidaVenta(solicitud())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        var command = received.get();
        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.skuId()).isEqualTo(SKU);
        assertThat(command.cantidad()).isEqualByComparingTo("6");
        assertThat(command.ventaId()).isEqualTo(VENTA);
        assertThat(command.ventaLineaId()).isEqualTo(LINEA);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.idempotencyKey()).isEqualTo("venta:1");
        assertThat(registrada.lotes()).hasSize(1);
        var lote = registrada.lotes().getFirst();
        assertThat(lote.movimientoId()).isEqualTo(movimientoId);
        assertThat(lote.loteId()).isEqualTo(LOTE);
        assertThat(lote.cantidad()).isEqualByComparingTo("6");
        assertThat(lote.stockPosterior()).isEqualByComparingTo("4");
    }

    @Test
    void passesTheUseCaseErrorThrough() {
        var facade = new SalidaInventarioFacade(command -> Result.failure(CONFLICTO));

        ApplicationError error = facade.registrarSalidaVenta(solicitud()).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }

    @Test
    void requiresItsCollaboratorAndTheRequest() {
        assertThatNullPointerException().isThrownBy(() -> new SalidaInventarioFacade(null));
        var facade = new SalidaInventarioFacade(command -> Result.failure(CONFLICTO));
        assertThatNullPointerException().isThrownBy(() -> facade.registrarSalidaVenta(null));
    }

    @Test
    void exposesTheErrorCodesOfTheInventoryErrors() {
        assertThat(SalidaInventarioApi.CODIGO_CONCURRENCIA).isEqualTo(InventarioErrors.CONCURRENCIA);
        assertThat(SalidaInventarioApi.CODIGO_STOCK_INSUFICIENTE)
                .isEqualTo(InventarioErrors.stockInsuficiente().code());
    }
}
