package com.softprimesolutions.compras.infrastructure.persistence;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.EMPRESA;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public final class Rows {

    public static final OffsetDateTime MOMENTO = AHORA.atOffset(ZoneOffset.UTC);

    private Rows() {
    }

    public static Map<String, Object> proveedor() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", PROVEEDOR);
        row.put("tipo_documento", "6");
        row.put("numero_documento", "20100070970");
        row.put("razon_social", "Laboratorios SAC");
        row.put("nombre_comercial", "Labs");
        row.put("direccion", "Av. Lima 123");
        row.put("ubigeo", "150101");
        row.put("telefono", "999888777");
        row.put("email", "ventas@labs.example");
        row.put("contacto_nombre", "Ana");
        row.put("contacto_telefono", "999000111");
        row.put("contacto_email", "ana@labs.example");
        row.put("condicion_pago_default", "CREDITO 30");
        row.put("dias_credito_default", 30);
        row.put("moneda_default", "PEN");
        row.put("es_laboratorio", Boolean.TRUE);
        row.put("es_importador", Boolean.FALSE);
        row.put("es_distribuidor", Boolean.TRUE);
        row.put("calificacion", "CONFIABLE");
        row.put("estado", "ACTIVO");
        row.put("created_at", MOMENTO);
        row.put("created_by", ACTOR_ID.toString());
        return row;
    }

    public static Map<String, Object> ordenEncabezado(boolean aprobada) {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", ORDEN);
        row.put("empresa_uuid", EMPRESA);
        row.put("proveedor_uuid", PROVEEDOR);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("numero", "OC-2026-000001");
        row.put("fecha_emision", HOY);
        row.put("moneda", "PEN");
        row.put("tipo_cambio", dec("1.000000"));
        row.put("condicion_pago", "CONTADO");
        row.put("dias_credito", 0);
        row.put("subtotal", dec("55.00"));
        row.put("descuento_total", dec("0.00"));
        row.put("impuesto_total", dec("9.90"));
        row.put("total", dec("64.90"));
        row.put("estado", aprobada ? "APROBADA" : "BORRADOR");
        row.put("observacion", "Reposicion");
        if (aprobada) {
            row.put("aprobado_por", ACTOR_ID);
            row.put("aprobado_at", MOMENTO);
        }
        row.put("created_at", MOMENTO);
        row.put("created_by", ACTOR_ID.toString());
        return row;
    }

    public static Map<String, Object> ordenLinea() {
        var row = new HashMap<String, Object>();
        row.put("numero_linea", 1);
        row.put("sku_uuid", SKU);
        row.put("descripcion_snapshot", "Paracetamol 500 mg");
        row.put("cantidad", dec("10.0000"));
        row.put("unidad_medida_codigo", "UND");
        row.put("precio_unitario", dec("5.500000"));
        row.put("descuento", dec("0.00"));
        row.put("impuesto", dec("9.90"));
        row.put("total_linea", dec("64.90"));
        row.put("tolerancia_exceso_pct", dec("10.0000"));
        row.put("tolerancia_defecto_pct", dec("0.0000"));
        row.put("cantidad_recibida", dec("4.0000"));
        return row;
    }

    public static Map<String, Object> recepcion() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", RECEPCION);
        row.put("numero", "REC-2026-000001");
        row.put("orden_uuid", ORDEN);
        row.put("proveedor_uuid", PROVEEDOR);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("almacen_uuid", ALMACEN);
        row.put("documento_proveedor_tipo", "01");
        row.put("documento_proveedor_serie", "F001");
        row.put("documento_proveedor_numero", "123");
        row.put("guia_remision_remitente", "T001-45");
        row.put("fecha_recepcion", MOMENTO);
        row.put("temperatura_recepcion_c", dec("22.50"));
        row.put("estado", "CONFIRMADA");
        row.put("observacion", "Recepcion");
        return row;
    }

    public static Map<String, Object> recepcionLinea() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", LINEA_RECEPCION);
        row.put("numero_linea", 1);
        row.put("numero_linea_orden", 1);
        row.put("sku_uuid", SKU);
        row.put("numero_lote", "LOTE-1");
        row.put("fecha_vencimiento", VENCIMIENTO);
        row.put("cantidad_recibida", dec("6.0000"));
        row.put("cantidad_aceptada", dec("5.0000"));
        row.put("cantidad_rechazada", dec("1.0000"));
        row.put("costo_unitario", dec("5.500000"));
        row.put("decision_calidad", "ACEPTADO_PARCIAL");
        row.put("motivo_decision", "Envase danado");
        row.put("lote_uuid", LOTE);
        return row;
    }
}
