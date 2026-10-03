package com.softprimesolutions.ventas.infrastructure.persistence;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
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
}
