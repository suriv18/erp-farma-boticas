package com.softprimesolutions.inventario.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.softprimesolutions.inventario.domain.valueobject.Actor;
import com.softprimesolutions.inventario.domain.valueobject.LoteId;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnumsAndValueObjectsTest {

    @Test
    void onlyAnEnabledLoteIsSellable() {
        assertThat(EstadoLote.values()).filteredOn(EstadoLote::vendible).containsExactly(EstadoLote.HABILITADO);
    }

    @Test
    void onlyEnabledAndQuarantinedLotesCanBeBlocked() {
        assertThat(EstadoLote.values()).filteredOn(EstadoLote::admiteBloqueo)
                .containsExactly(EstadoLote.HABILITADO, EstadoLote.CUARENTENA);
    }

    @Test
    void stockCanBeAddedToEnabledQuarantinedAndBlockedLotes() {
        assertThat(EstadoLote.values()).filteredOn(EstadoLote::admiteIngreso)
                .containsExactly(EstadoLote.HABILITADO, EstadoLote.CUARENTENA, EstadoLote.BLOQUEADO);
    }

    @Test
    void parsesMovementTypesAndExposesTheirNature() {
        assertThat(TipoMovimiento.desde("AJUSTE_INGRESO")).contains(TipoMovimiento.AJUSTE_INGRESO);
        assertThat(TipoMovimiento.desde("AJUSTE_SALIDA")).contains(TipoMovimiento.AJUSTE_SALIDA);
        assertThat(TipoMovimiento.desde("OTRO")).isEmpty();
        assertThat(TipoMovimiento.desde(null)).isEmpty();
        assertThat(TipoMovimiento.AJUSTE_INGRESO.ingreso()).isTrue();
        assertThat(TipoMovimiento.AJUSTE_INGRESO.naturaleza()).isEqualTo("E");
        assertThat(TipoMovimiento.AJUSTE_SALIDA.ingreso()).isFalse();
        assertThat(TipoMovimiento.AJUSTE_SALIDA.naturaleza()).isEqualTo("S");
    }

    @Test
    void salidaVentaIsANonManualSalidaThatRequiresASellableLote() {
        assertThat(TipoMovimiento.desde("SALIDA_VENTA")).contains(TipoMovimiento.SALIDA_VENTA);
        assertThat(TipoMovimiento.SALIDA_VENTA.ingreso()).isFalse();
        assertThat(TipoMovimiento.SALIDA_VENTA.naturaleza()).isEqualTo("S");
        assertThat(TipoMovimiento.SALIDA_VENTA.manual()).isFalse();
        assertThat(TipoMovimiento.SALIDA_VENTA.tipoOperacionSunat()).isEqualTo("01");
        assertThat(TipoMovimiento.SALIDA_VENTA.exigeLoteVendible()).isTrue();
        assertThat(TipoMovimiento.AJUSTE_SALIDA.exigeLoteVendible()).isFalse();
    }

    @Test
    void keepsTheFullUserIdAsTheActorCode() {
        var id = UUID.fromString("12345678-9abc-4def-8123-456789abcdef");

        assertThat(new Actor(id).codigo()).isEqualTo("12345678-9abc-4def-8123-456789abcdef").hasSize(36);
        assertThat(new Actor(id).id()).isEqualTo(id);
    }

    @Test
    void identifiersRequireAValue() {
        var id = UUID.randomUUID();

        assertThat(new LoteId(id).value()).isEqualTo(id);
        assertThat(new PosicionId(id).value()).isEqualTo(id);
        assertThat(new TenantId(id).value()).isEqualTo(id);
        assertThatNullPointerException().isThrownBy(() -> new LoteId(null));
        assertThatNullPointerException().isThrownBy(() -> new PosicionId(null));
        assertThatNullPointerException().isThrownBy(() -> new TenantId(null));
        assertThatNullPointerException().isThrownBy(() -> new Actor(null));
    }

    @Test
    void anulacionVentaIsANonManualIngresoThatIgnoresTheLoteStateOnIngreso() {
        assertThat(TipoMovimiento.desde("ANULACION_VENTA")).contains(TipoMovimiento.ANULACION_VENTA);
        assertThat(TipoMovimiento.ANULACION_VENTA.ingreso()).isTrue();
        assertThat(TipoMovimiento.ANULACION_VENTA.naturaleza()).isEqualTo("E");
        assertThat(TipoMovimiento.ANULACION_VENTA.manual()).isFalse();
        assertThat(TipoMovimiento.ANULACION_VENTA.tipoOperacionSunat()).isEqualTo("05");
        assertThat(TipoMovimiento.ANULACION_VENTA.exigeLoteVendible()).isFalse();
        assertThat(TipoMovimiento.ANULACION_VENTA.ignoraEstadoLoteEnIngreso()).isTrue();
        assertThat(TipoMovimiento.INGRESO_COMPRA.ignoraEstadoLoteEnIngreso()).isFalse();
        assertThat(TipoMovimiento.SALIDA_VENTA.ignoraEstadoLoteEnIngreso()).isFalse();
    }
}
