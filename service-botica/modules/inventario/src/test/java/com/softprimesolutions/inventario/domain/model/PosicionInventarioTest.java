package com.softprimesolutions.inventario.domain.model;

import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.posicion;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.domain.exception.InventarioErrorCodes;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PosicionInventarioTest {

    private static String code(Result<AplicacionMovimiento, ErrorDetail> result) {
        return result.fold(value -> "OK", ErrorDetail::code);
    }

    private static AplicacionMovimiento applied(Result<AplicacionMovimiento, ErrorDetail> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    @Test
    void startsEmptyAtVersionZero() {
        var nueva = PosicionInventario.nueva(new PosicionId(POSICION), LOTE, ALMACEN, SKU);

        assertThat(nueva.id()).isEqualTo(new PosicionId(POSICION));
        assertThat(nueva.loteId()).isEqualTo(LOTE);
        assertThat(nueva.almacenId()).isEqualTo(ALMACEN);
        assertThat(nueva.skuId()).isEqualTo(SKU);
        assertThat(nueva.cantidadFisica()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nueva.cantidadReservada()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(nueva.version()).isZero();
    }

    @Test
    void anIngresoIncreasesThePhysicalStockKeepingTheObservedVersion() {
        var aplicacion = applied(posicion("10", "2", 7).aplicar(TipoMovimiento.AJUSTE_INGRESO, new BigDecimal("5.5")));

        assertThat(aplicacion.stockAnterior()).isEqualByComparingTo("10");
        assertThat(aplicacion.stockPosterior()).isEqualByComparingTo("15.5");
        assertThat(aplicacion.posicion().cantidadFisica()).isEqualByComparingTo("15.5");
        assertThat(aplicacion.posicion().cantidadReservada()).isEqualByComparingTo("2");
        assertThat(aplicacion.posicion().version()).isEqualTo(7);
    }

    @Test
    void aSalidaDecreasesTheStockDownToTheReservedQuantity() {
        var aplicacion = applied(posicion("10", "2", 1).aplicar(TipoMovimiento.AJUSTE_SALIDA, new BigDecimal("8")));

        assertThat(aplicacion.stockPosterior()).isEqualByComparingTo("2");
    }

    @Test
    void aSalidaCannotLeaveLessStockThanWhatIsReserved() {
        assertThat(code(posicion("10", "2", 1).aplicar(TipoMovimiento.AJUSTE_SALIDA, new BigDecimal("8.0001"))))
                .isEqualTo(InventarioErrorCodes.STOCK_INSUFICIENTE);
        assertThat(code(posicion("5", "0", 1).aplicar(TipoMovimiento.AJUSTE_SALIDA, new BigDecimal("6"))))
                .isEqualTo(InventarioErrorCodes.STOCK_INSUFICIENTE);
    }

    @Test
    void rejectsMissingNonPositiveOrOversizedQuantities() {
        var posicion = posicion("10", "0", 1);

        assertThat(code(posicion.aplicar(TipoMovimiento.AJUSTE_INGRESO, null)))
                .isEqualTo(InventarioErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(posicion.aplicar(TipoMovimiento.AJUSTE_INGRESO, BigDecimal.ZERO)))
                .isEqualTo(InventarioErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(posicion.aplicar(TipoMovimiento.AJUSTE_INGRESO, new BigDecimal("-1"))))
                .isEqualTo(InventarioErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(posicion.aplicar(TipoMovimiento.AJUSTE_INGRESO, new BigDecimal("1000000000.0001"))))
                .isEqualTo(InventarioErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(posicion.aplicar(TipoMovimiento.AJUSTE_INGRESO, new BigDecimal("1000000000"))))
                .isEqualTo("OK");
    }

    @Test
    void rejectsMoreThanFourSignificantDecimals() {
        var posicion = posicion("10", "0", 1);

        assertThat(code(posicion.aplicar(TipoMovimiento.AJUSTE_INGRESO, new BigDecimal("0.00001"))))
                .isEqualTo(InventarioErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(posicion.aplicar(TipoMovimiento.AJUSTE_INGRESO, new BigDecimal("1.5000000"))))
                .isEqualTo("OK");
    }
}
