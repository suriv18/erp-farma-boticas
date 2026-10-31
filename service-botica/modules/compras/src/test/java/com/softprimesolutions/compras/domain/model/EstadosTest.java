package com.softprimesolutions.compras.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstadosTest {

    @Test
    void onlyAnActiveProveedorAdmitsPurchases() {
        assertThat(EstadoProveedor.ACTIVO.admiteCompras()).isTrue();
        assertThat(EstadoProveedor.BLOQUEADO.admiteCompras()).isFalse();
        assertThat(EstadoProveedor.SUSPENDIDO.admiteCompras()).isFalse();
        assertThat(EstadoProveedor.desde("BLOQUEADO")).contains(EstadoProveedor.BLOQUEADO);
        assertThat(EstadoProveedor.desde("otro")).isEmpty();
        assertThat(EstadoProveedor.desde(null)).isEmpty();
    }

    @Test
    void theOrderStatesDefineWhichTransitionsTheyAdmit() {
        assertThat(EstadoOrdenCompra.BORRADOR.aprobable()).isTrue();
        assertThat(EstadoOrdenCompra.EN_APROBACION.aprobable()).isTrue();
        assertThat(EstadoOrdenCompra.APROBADA.aprobable()).isFalse();
        assertThat(EstadoOrdenCompra.APROBADA.emitible()).isTrue();
        assertThat(EstadoOrdenCompra.EMITIDA.emitible()).isFalse();
        assertThat(EstadoOrdenCompra.BORRADOR.anulable()).isTrue();
        assertThat(EstadoOrdenCompra.EMITIDA.anulable()).isTrue();
        assertThat(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA.anulable()).isFalse();
        assertThat(EstadoOrdenCompra.EMITIDA.recepcionable()).isTrue();
        assertThat(EstadoOrdenCompra.PARCIALMENTE_RECIBIDA.recepcionable()).isTrue();
        assertThat(EstadoOrdenCompra.RECIBIDA.recepcionable()).isFalse();
        assertThat(EstadoOrdenCompra.CANCELADA.recepcionable()).isFalse();
        assertThat(EstadoOrdenCompra.CERRADA.anulable()).isFalse();
        assertThat(EstadoOrdenCompra.desde("EMITIDA")).contains(EstadoOrdenCompra.EMITIDA);
        assertThat(EstadoOrdenCompra.desde("otro")).isEmpty();
        assertThat(EstadoOrdenCompra.desde(null)).isEmpty();
    }
}
