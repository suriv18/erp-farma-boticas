package com.softprimesolutions.ventas.infrastructure.persistence;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.LINEA;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public final class Rows {

    public static final OffsetDateTime MOMENTO = AHORA.atOffset(ZoneOffset.UTC);

    private Rows() {
    }

    public static Map<String, Object> turno() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", TURNO);
        row.put("terminal_uuid", TERMINAL);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("cajero_uuid", ACTOR_ID);
        row.put("apertura_at", MOMENTO);
        row.put("fondo_inicial", dec("50.00"));
        row.put("estado", "ABIERTO");
        row.put("cierre_at", null);
        row.put("total_ventas_sistema", dec("0.00"));
        row.put("total_sistema", dec("50.00"));
        row.put("total_declarado", null);
        row.put("diferencia", null);
        row.put("observacion_cierre", null);
        return row;
    }

    public static Map<String, Object> ventaCabecera() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("numero_operacion", "EST001-POS01-000001");
        row.put("terminal_uuid", TERMINAL);
        row.put("turno_uuid", TURNO);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("vendedor_uuid", ACTOR_ID);
        row.put("fecha_venta", MOMENTO);
        row.put("moneda", "PEN");
        row.put("subtotal", dec("12.50"));
        row.put("descuento_total", dec("0.00"));
        row.put("impuesto_total", dec("0.00"));
        row.put("total", dec("12.50"));
        row.put("estado", "CONFIRMADA");
        return row;
    }

    public static Map<String, Object> ventaLinea() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", LINEA);
        row.put("numero_linea", 1);
        row.put("sku_uuid", SKU);
        row.put("descripcion_snapshot", "Paracetamol 500 mg");
        row.put("unidad_venta_codigo", "UND");
        row.put("cantidad", dec("5.0000"));
        row.put("precio_unitario", dec("2.5000"));
        row.put("total_linea", dec("12.50"));
        return row;
    }

    public static Map<String, Object> ventaLote() {
        var row = new HashMap<String, Object>();
        row.put("linea_uuid", LINEA);
        row.put("lote_uuid", LOTE);
        row.put("cantidad", dec("5.0000"));
        return row;
    }

    public static Map<String, Object> ventaPago() {
        var row = new HashMap<String, Object>();
        row.put("medio_codigo", "EFECTIVO");
        row.put("monto", dec("12.50"));
        row.put("monto_recibido", dec("20.00"));
        row.put("vuelto", dec("7.50"));
        return row;
    }

    public static Map<String, Object> ventaResumen() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("numero_operacion", "EST001-POS01-000001");
        row.put("terminal_uuid", TERMINAL);
        row.put("fecha_venta", MOMENTO);
        row.put("total", dec("12.50"));
        row.put("estado", "CONFIRMADA");
        return row;
    }
}
