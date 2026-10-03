package com.softprimesolutions.inventario.api.facade;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.CONFLICTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AnulacionInventarioFacadeTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    private static ReintegroVentaSolicitud solicitud() {
        return new ReintegroVentaSolicitud(TENANT, VENTA, ACTOR_ID);
    }

    @Test
    void translatesTheRequestIntoACommandAndTheMovementsIntoReintegratedLotes() {
        var received = new AtomicReference<ReintegrarSalidasDeVentaCommand>();
        var movimientoId = UUID.randomUUID();
        var facade = new AnulacionInventarioFacade(command -> {
            received.set(command);
            return Result.success(List.of(new MovimientoResult(
                    movimientoId, POSICION, LOTE, "ANULACION_VENTA", "E", new BigDecimal("3"),
                    new BigDecimal("7"), new BigDecimal("10"), AHORA)));
        });

        var registrado = facade.reintegrarSalidasDeVenta(solicitud())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().ventaId()).isEqualTo(VENTA);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(registrado.movimientos()).hasSize(1);
        var movimiento = registrado.movimientos().getFirst();
        assertThat(movimiento.movimientoId()).isEqualTo(movimientoId);
        assertThat(movimiento.loteId()).isEqualTo(LOTE);
        assertThat(movimiento.cantidad()).isEqualByComparingTo("3");
        assertThat(movimiento.stockPosterior()).isEqualByComparingTo("10");
    }

    @Test
    void passesTheUseCaseErrorThrough() {
        var facade = new AnulacionInventarioFacade(command -> Result.failure(CONFLICTO));

        ApplicationError error = facade.reintegrarSalidasDeVenta(solicitud()).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }

    @Test
    void requiresItsCollaboratorAndTheRequest() {
        assertThatNullPointerException().isThrownBy(() -> new AnulacionInventarioFacade(null));
        var facade = new AnulacionInventarioFacade(command -> Result.failure(CONFLICTO));
        assertThatNullPointerException().isThrownBy(() -> facade.reintegrarSalidasDeVenta(null));
    }

    @Test
    void exposesTheErrorCodesOfTheInventoryErrors() {
        assertThat(AnulacionInventarioApi.CODIGO_CONCURRENCIA).isEqualTo(InventarioErrors.CONCURRENCIA);
        assertThat(AnulacionInventarioApi.CODIGO_SALIDAS_NO_ENCONTRADAS)
                .isEqualTo(InventarioErrors.salidasNoEncontradas().code());
    }
}
