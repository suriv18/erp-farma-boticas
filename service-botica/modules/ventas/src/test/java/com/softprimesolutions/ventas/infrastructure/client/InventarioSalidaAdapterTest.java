package com.softprimesolutions.ventas.infrastructure.client;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.LINEA;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.api.LoteConsumido;
import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaRegistrada;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort.SalidaSolicitada;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InventarioSalidaAdapterTest {

    private static final SalidaSolicitada SOLICITUD = new SalidaSolicitada(
            TENANT, ALMACEN, SKU, dec("5"), VENTA, LINEA, ACTOR_ID, "clave-1:1");

    @Test
    void translatesTheRequestAndMapsTheConsumedLotes() {
        var recibida = new AtomicReference<SalidaVentaSolicitud>();
        var adapter = new InventarioSalidaAdapter(solicitud -> {
            recibida.set(solicitud);
            return Result.success(new SalidaVentaRegistrada(List.of(
                    new LoteConsumido(UUID.randomUUID(), LOTE, dec("3"), dec("7")),
                    new LoteConsumido(UUID.randomUUID(), UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"),
                            dec("2"), dec("8")))));
        });

        var lotes = adapter.descontar(SOLICITUD).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(lotes).containsExactly(
                new LoteConsumo(LOTE, dec("3")),
                new LoteConsumo(UUID.fromString("dddddddd-dddd-4ddd-8ddd-dddddddddddd"), dec("2")));
        var solicitud = recibida.get();
        assertThat(solicitud.tenantId()).isEqualTo(TENANT);
        assertThat(solicitud.almacenId()).isEqualTo(ALMACEN);
        assertThat(solicitud.skuId()).isEqualTo(SKU);
        assertThat(solicitud.cantidad()).isEqualTo(dec("5"));
        assertThat(solicitud.ventaId()).isEqualTo(VENTA);
        assertThat(solicitud.ventaLineaId()).isEqualTo(LINEA);
        assertThat(solicitud.actorId()).isEqualTo(ACTOR_ID);
        assertThat(solicitud.idempotencyKey()).isEqualTo("clave-1:1");
    }

    @Test
    void anInventoryConcurrencyErrorBecomesTheSalesConcurrencyError() {
        var concurrencia = new StandardApplicationError(
                SalidaInventarioApi.CODIGO_CONCURRENCIA, "Concurrencia.", ErrorCategory.CONFLICT);
        var adapter = new InventarioSalidaAdapter(solicitud -> Result.failure(concurrencia));

        ApplicationError error = adapter.descontar(SOLICITUD).fold(value -> null, failure -> failure);

        assertThat(error.code()).isEqualTo(VentasErrors.CONCURRENCIA);
    }

    @Test
    void anyOtherInventoryErrorPassesThroughUntouched() {
        var adapter = new InventarioSalidaAdapter(solicitud -> Result.failure(CONFLICTO));

        ApplicationError error = adapter.descontar(SOLICITUD).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }
}
