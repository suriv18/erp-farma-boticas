package com.softprimesolutions.ventas.infrastructure.client;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.CONFLICTO;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.MovimientoReintegrado;
import com.softprimesolutions.inventario.api.ReintegroVentaRegistrado;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InventarioReintegroAdapterTest {

    @Test
    void translatesTheRequestAndMapsTheReintegratedLotes() {
        var recibida = new AtomicReference<ReintegroVentaSolicitud>();
        var adapter = new InventarioReintegroAdapter(solicitud -> {
            recibida.set(solicitud);
            return Result.success(new ReintegroVentaRegistrado(List.of(
                    new MovimientoReintegrado(UUID.randomUUID(), LOTE, dec("3"), dec("10")))));
        });

        var lotes = adapter.reintegrar(TENANT, VENTA, ACTOR_ID)
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(lotes).containsExactly(new LoteConsumo(LOTE, dec("3")));
        assertThat(recibida.get().tenantId()).isEqualTo(TENANT);
        assertThat(recibida.get().ventaId()).isEqualTo(VENTA);
        assertThat(recibida.get().actorId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void anInventoryConcurrencyErrorBecomesTheSalesConcurrencyError() {
        var concurrencia = new StandardApplicationError(
                AnulacionInventarioApi.CODIGO_CONCURRENCIA, "Concurrencia.", ErrorCategory.CONFLICT);
        var adapter = new InventarioReintegroAdapter(solicitud -> Result.failure(concurrencia));

        ApplicationError error = adapter.reintegrar(TENANT, VENTA, ACTOR_ID).fold(value -> null, failure -> failure);

        assertThat(error.code()).isEqualTo(VentasErrors.CONCURRENCIA);
    }

    @Test
    void anyOtherInventoryErrorPassesThroughUntouched() {
        var adapter = new InventarioReintegroAdapter(solicitud -> Result.failure(CONFLICTO));

        ApplicationError error = adapter.reintegrar(TENANT, VENTA, ACTOR_ID).fold(value -> null, failure -> failure);

        assertThat(error).isSameAs(CONFLICTO);
    }
}
