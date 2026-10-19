package com.softprimesolutions.compras.application.usecase.command;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.error;
import static com.softprimesolutions.compras.ComprasFixtures.lineaOrden;
import static com.softprimesolutions.compras.ComprasFixtures.orden;
import static com.softprimesolutions.compras.ComprasFixtures.ordenNueva;
import static com.softprimesolutions.compras.ComprasFixtures.value;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.compras.application.dto.command.TransicionOrden;
import com.softprimesolutions.compras.application.dto.command.TransicionarOrdenCompraCommand;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TransicionarOrdenCompraHandlerTest {

    private final OrdenCompraWritePort ordenes = mock(OrdenCompraWritePort.class);
    private final TransicionarOrdenCompraHandler handler = new TransicionarOrdenCompraHandler(ordenes, () -> AHORA);

    private static TransicionarOrdenCompraCommand command(TransicionOrden transicion, String motivo) {
        return new TransicionarOrdenCompraCommand(TENANT, ORDEN, transicion, motivo, ACTOR_ID);
    }

    private void given(OrdenCompra orden) {
        when(ordenes.findById(TENANT, ORDEN)).thenReturn(Optional.of(orden));
        when(ordenes.actualizarEstado(any(), any())).thenReturn(true);
    }

    private OrdenCompra saved(EstadoOrdenCompra previo) {
        var captor = ArgumentCaptor.forClass(OrdenCompra.class);
        verify(ordenes).actualizarEstado(captor.capture(), org.mockito.ArgumentMatchers.eq(previo));
        return captor.getValue();
    }

    @Test
    void approvesADraftGuardedByItsPreviousState() {
        given(ordenNueva());

        var result = value(handler.execute(command(TransicionOrden.APROBAR, null)));

        assertThat(result.estado()).isEqualTo("APROBADA");
        assertThat(result.aprobadoAt()).isEqualTo(AHORA);
        assertThat(saved(EstadoOrdenCompra.BORRADOR).aprobadoPor()).isEqualTo(ACTOR_ID.toString());
    }

    @Test
    void issuesAnApprovedOrder() {
        given(orden(EstadoOrdenCompra.APROBADA, lineaOrden("10", "0", "0", "0")));

        var result = value(handler.execute(command(TransicionOrden.EMITIR, null)));

        assertThat(result.estado()).isEqualTo("EMITIDA");
        assertThat(saved(EstadoOrdenCompra.APROBADA).estado()).isEqualTo(EstadoOrdenCompra.EMITIDA);
    }

    @Test
    void cancelsAnOrderRecordingTheReason() {
        given(ordenNueva());

        var result = value(handler.execute(command(TransicionOrden.ANULAR, "Error de digitacion")));

        assertThat(result.estado()).isEqualTo("CANCELADA");
        assertThat(result.observacion()).startsWith("Anulada: Error de digitacion");
    }

    @Test
    void reportsAnUnknownOrder() {
        var error = error(handler.execute(command(TransicionOrden.APROBAR, null)));

        assertThat(error.code()).isEqualTo("COM_ORDEN_NO_ENCONTRADA");
    }

    @Test
    void reportsTheDomainErrorOfAnInvalidTransition() {
        given(ordenNueva());

        var error = error(handler.execute(command(TransicionOrden.EMITIR, null)));

        assertThat(error.code()).isEqualTo("COM_ORDEN_ESTADO_INVALIDO");
        assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
    }

    @Test
    void reportsAConcurrentModificationWhenThePreviousStateNoLongerMatches() {
        given(ordenNueva());
        when(ordenes.actualizarEstado(any(), any())).thenReturn(false);

        var error = error(handler.execute(command(TransicionOrden.APROBAR, null)));

        assertThat(error.code()).isEqualTo("COM_MODIFICACION_CONCURRENTE");
    }
}
