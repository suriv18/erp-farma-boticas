package com.softprimesolutions.inventario.domain.model;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.HOY;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.lote;
import static com.softprimesolutions.inventario.InventarioFixtures.loteHabilitado;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.domain.exception.InventarioErrorCodes;
import com.softprimesolutions.inventario.domain.valueobject.LoteId;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LoteTest {

    private static Result<Lote, ErrorDetail> create(String numero, LocalDate vencimiento) {
        return Lote.create(new LoteId(LOTE), new TenantId(TENANT), SKU, numero, vencimiento, HOY, AHORA);
    }

    private static Lote created(String numero, LocalDate vencimiento) {
        return create(numero, vencimiento).fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static String code(Result<Lote, ErrorDetail> result) {
        return result.fold(value -> "OK", ErrorDetail::code);
    }

    private static Lote unwrap(Result<Lote, ErrorDetail> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    @Test
    void createsAnEnabledLoteWithTheNormalizedNumber() {
        var lote = created("  L-001  ", VENCIMIENTO);

        assertThat(lote.id()).isEqualTo(new LoteId(LOTE));
        assertThat(lote.tenantId()).isEqualTo(new TenantId(TENANT));
        assertThat(lote.skuId()).isEqualTo(SKU);
        assertThat(lote.numeroLote()).isEqualTo("L-001");
        assertThat(lote.fechaVencimiento()).isEqualTo(VENCIMIENTO);
        assertThat(lote.estado()).isEqualTo(EstadoLote.HABILITADO);
        assertThat(lote.motivoEstado()).isNull();
        assertThat(lote.bloqueadoAt()).isNull();
        assertThat(lote.bloqueadoPor()).isNull();
        assertThat(lote.createdAt()).isEqualTo(AHORA);
        assertThat(lote.updatedAt()).isNull();
        assertThat(lote.updatedBy()).isNull();
    }

    @Test
    void acceptsALoteThatExpiresToday() {
        assertThat(code(create("L-001", HOY))).isEqualTo("OK");
    }

    @Test
    void rejectsAMissingOrBlankOrOversizedNumber() {
        assertThat(code(create(null, VENCIMIENTO))).isEqualTo(InventarioErrorCodes.LOTE_INVALIDO);
        assertThat(code(create("   ", VENCIMIENTO))).isEqualTo(InventarioErrorCodes.LOTE_INVALIDO);
        assertThat(code(create("x".repeat(121), VENCIMIENTO))).isEqualTo(InventarioErrorCodes.LOTE_INVALIDO);
        assertThat(code(create("x".repeat(120), VENCIMIENTO))).isEqualTo("OK");
    }

    @Test
    void rejectsAMissingOrPastExpirationDate() {
        assertThat(code(create("L-001", null))).isEqualTo(InventarioErrorCodes.LOTE_INVALIDO);
        assertThat(code(create("L-001", HOY.minusDays(1)))).isEqualTo(InventarioErrorCodes.LOTE_VENCIDO);
    }

    @Test
    void blocksAnEnabledOrQuarantinedLoteRecordingWhoAndWhy() {
        var bloqueado = unwrap(loteHabilitado().bloquear("  Reclamo de calidad  ", ACTOR, AHORA));

        assertThat(bloqueado.estado()).isEqualTo(EstadoLote.BLOQUEADO);
        assertThat(bloqueado.motivoEstado()).isEqualTo("Reclamo de calidad");
        assertThat(bloqueado.bloqueadoAt()).isEqualTo(AHORA);
        assertThat(bloqueado.bloqueadoPor()).isEqualTo(ACTOR.codigo());
        assertThat(bloqueado.updatedAt()).isEqualTo(AHORA);
        assertThat(bloqueado.updatedBy()).isEqualTo(ACTOR.codigo());
        assertThat(code(lote(EstadoLote.CUARENTENA, VENCIMIENTO).bloquear("x", ACTOR, AHORA))).isEqualTo("OK");
    }

    @Test
    void refusesToBlockALoteInAnyOtherState() {
        for (var estado : new EstadoLote[] {
                EstadoLote.BLOQUEADO, EstadoLote.INMOVILIZADO_RECALL, EstadoLote.VENCIDO,
                EstadoLote.BAJA_DESTRUIDO}) {
            assertThat(code(lote(estado, VENCIMIENTO).bloquear("x", ACTOR, AHORA)))
                    .isEqualTo(InventarioErrorCodes.LOTE_ESTADO_INVALIDO);
        }
    }

    @Test
    void refusesToBlockWithoutAValidReason() {
        assertThat(code(loteHabilitado().bloquear(null, ACTOR, AHORA)))
                .isEqualTo(InventarioErrorCodes.LOTE_MOTIVO_INVALIDO);
        assertThat(code(loteHabilitado().bloquear("   ", ACTOR, AHORA)))
                .isEqualTo(InventarioErrorCodes.LOTE_MOTIVO_INVALIDO);
        assertThat(code(loteHabilitado().bloquear("x".repeat(1001), ACTOR, AHORA)))
                .isEqualTo(InventarioErrorCodes.LOTE_MOTIVO_INVALIDO);
    }

    @Test
    void unblocksABlockedLoteAndClearsTheBlockData() {
        var bloqueado = unwrap(loteHabilitado().bloquear("motivo", ACTOR, AHORA));

        var habilitado = unwrap(bloqueado.desbloquear(ACTOR, HOY, AHORA.plusSeconds(60)));

        assertThat(habilitado.estado()).isEqualTo(EstadoLote.HABILITADO);
        assertThat(habilitado.motivoEstado()).isNull();
        assertThat(habilitado.bloqueadoAt()).isNull();
        assertThat(habilitado.bloqueadoPor()).isNull();
        assertThat(habilitado.updatedAt()).isEqualTo(AHORA.plusSeconds(60));
        assertThat(habilitado.updatedBy()).isEqualTo(ACTOR.codigo());
    }

    @Test
    void refusesToUnblockALoteThatIsNotBlocked() {
        assertThat(code(loteHabilitado().desbloquear(ACTOR, HOY, AHORA)))
                .isEqualTo(InventarioErrorCodes.LOTE_ESTADO_INVALIDO);
    }

    @Test
    void refusesToEnableAnExpiredLote() {
        var vencido = lote(EstadoLote.BLOQUEADO, HOY.minusDays(1));

        assertThat(code(vencido.desbloquear(ACTOR, HOY, AHORA))).isEqualTo(InventarioErrorCodes.LOTE_NO_HABILITABLE);
    }

    @Test
    void isSellableOnlyWhenEnabledAndNotExpired() {
        assertThat(loteHabilitado().vendible(HOY)).isTrue();
        assertThat(lote(EstadoLote.HABILITADO, HOY).vendible(HOY)).isTrue();
        assertThat(lote(EstadoLote.HABILITADO, HOY.minusDays(1)).vendible(HOY)).isFalse();
        assertThat(lote(EstadoLote.BLOQUEADO, VENCIMIENTO).vendible(HOY)).isFalse();
    }
}
