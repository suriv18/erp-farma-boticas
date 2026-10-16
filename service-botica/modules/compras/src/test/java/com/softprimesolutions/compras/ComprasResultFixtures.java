package com.softprimesolutions.compras;

import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.HOY;
import static com.softprimesolutions.compras.ComprasFixtures.LINEA_RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.LOTE;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.VENCIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.dec;

import com.softprimesolutions.compras.application.dto.result.LineaRecepcionResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResumenResult;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import java.util.List;

public final class ComprasResultFixtures {

    private ComprasResultFixtures() {
    }

    public static RecepcionResult recepcionResult() {
        return new RecepcionResult(
                RECEPCION, "REC-2026-000001", ORDEN, PROVEEDOR, ESTABLECIMIENTO, ALMACEN, "01", "F001", "123",
                "T001-45", null, AHORA, dec("22.5"), null, "CONFIRMADA", "Recepcion",
                List.of(new LineaRecepcionResult(
                        LINEA_RECEPCION, 1, 1, SKU, "LOTE-1", null, VENCIMIENTO, dec("6"), dec("5"), dec("1"),
                        dec("5.5"), "ACEPTADO_PARCIAL", "Envase danado", null, LOTE)));
    }

    public static OrdenCompraResumenResult ordenResumen() {
        return new OrdenCompraResumenResult(
                ORDEN, "OC-2026-000001", PROVEEDOR, "Laboratorios SAC", ESTABLECIMIENTO, HOY, null, "PEN",
                dec("64.90"), "EMITIDA");
    }
}
