package com.softprimesolutions.compras.infrastructure.client;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.CONFLICTO;
import static com.softprimesolutions.compras.ComprasFixtures.LINEA_RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.LOTE;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.VENCIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.error;
import static com.softprimesolutions.compras.ComprasFixtures.value;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort.IngresoSolicitado;
import com.softprimesolutions.inventario.api.IngresoCompraRegistrado;
import com.softprimesolutions.inventario.api.IngresoCompraSolicitud;
import com.softprimesolutions.inventario.api.IngresoInventarioApi;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InventarioIngresoAdapterTest {

    private static final UUID MOVIMIENTO = UUID.fromString("abababab-abab-4bab-8bab-abababababab");

    private static IngresoSolicitado solicitud() {
        return new IngresoSolicitado(
                TENANT, ALMACEN, SKU, "LOTE-1", VENCIMIENTO, dec("5"), RECEPCION, LINEA_RECEPCION, PROVEEDOR,
                ACTOR_ID, "recepcion:1");
    }

    @Test
    void translatesTheRequestToTheInventoryApiAndItsAnswerBack() {
        var received = new AtomicReference<IngresoCompraSolicitud>();
        IngresoInventarioApi api = solicitudInventario -> {
            received.set(solicitudInventario);
            return Result.success(new IngresoCompraRegistrado(MOVIMIENTO, LOTE, UUID.randomUUID(), dec("1"), dec("6")));
        };

        var result = value(new InventarioIngresoAdapter(api).ingresar(solicitud()));

        assertThat(received.get()).isEqualTo(new IngresoCompraSolicitud(
                TENANT, ALMACEN, SKU, "LOTE-1", VENCIMIENTO, dec("5"), RECEPCION, LINEA_RECEPCION, PROVEEDOR,
                ACTOR_ID, "recepcion:1"));
        assertThat(result.movimientoId()).isEqualTo(MOVIMIENTO);
        assertThat(result.loteId()).isEqualTo(LOTE);
        assertThat(result.stockPosterior()).isEqualByComparingTo("6");
    }

    @Test
    void passesTheInventoryErrorsThroughUnchanged() {
        IngresoInventarioApi api = solicitudInventario -> Result.failure(CONFLICTO);

        assertThat(error(new InventarioIngresoAdapter(api).ingresar(solicitud()))).isSameAs(CONFLICTO);
    }

    @Test
    void translatesTheInventoryConcurrencyErrorToTheOneOfThePurchasesModule() {
        IngresoInventarioApi api = solicitudInventario -> Result.failure(new StandardApplicationError(
                IngresoInventarioApi.CODIGO_CONCURRENCIA, "Stock modificado", ErrorCategory.CONFLICT));

        var error = error(new InventarioIngresoAdapter(api).ingresar(solicitud()));

        assertThat(error.code()).isEqualTo(ComprasErrors.CONCURRENCIA);
    }
}
