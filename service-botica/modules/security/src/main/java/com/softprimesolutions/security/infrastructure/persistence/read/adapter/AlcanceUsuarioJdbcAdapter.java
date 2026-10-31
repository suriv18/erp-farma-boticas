package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import static java.util.stream.Collectors.toSet;

import com.softprimesolutions.security.api.AlcanceResultado;
import com.softprimesolutions.security.api.AlcanceUsuario;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AlcanceUsuarioJdbcAdapter implements AlcanceUsuario {

    private static final String ASIGNACIONES = """
            SELECT a.tipo_ambito, e.uuid_publico AS empresa_uuid, s.uuid_publico AS establecimiento_uuid,
                   w.uuid_publico AS almacen_uuid, p.uuid_publico AS terminal_uuid
              FROM sch_seguridad.usuario_rol_ambito a
              JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
              JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
              JOIN sch_admin.tenant t ON t.id = a.tenant_id
         LEFT JOIN sch_organizacion.empresa_operadora e ON e.id = a.empresa_id
         LEFT JOIN sch_organizacion.establecimiento_farmaceutico s ON s.id = a.establecimiento_id
         LEFT JOIN sch_organizacion.almacen w ON w.id = a.almacen_id
         LEFT JOIN sch_organizacion.terminal_pos p ON p.id = a.terminal_id
             WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :usuarioId
               AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO'
               AND %s
            """.formatted(AutorizacionAmbitoJdbcAdapter.ASIGNACION_VIGENTE);

    record Fila(String tipo, UUID empresa, UUID establecimiento, UUID almacen, UUID terminal) {
    }

    private final JdbcClient jdbc;

    public AlcanceUsuarioJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public AlcanceResultado alcanceDe(UUID tenantId, UUID usuarioId) {
        var filas = jdbc.sql(ASIGNACIONES)
                .param("tenantId", tenantId)
                .param("usuarioId", usuarioId)
                .query(AlcanceUsuarioJdbcAdapter::fila)
                .list();
        return new AlcanceResultado(
                filas.stream().anyMatch(fila -> "GLOBAL".equals(fila.tipo())),
                idsDe(filas, "EMPRESA", Fila::empresa),
                idsDe(filas, "ESTABLECIMIENTO", Fila::establecimiento),
                idsDe(filas, "ALMACEN", Fila::almacen),
                idsDe(filas, "TERMINAL", Fila::terminal));
    }

    static Fila fila(ResultSet rs, int rowNumber) throws SQLException {
        return new Fila(
                rs.getString("tipo_ambito"),
                rs.getObject("empresa_uuid", UUID.class),
                rs.getObject("establecimiento_uuid", UUID.class),
                rs.getObject("almacen_uuid", UUID.class),
                rs.getObject("terminal_uuid", UUID.class));
    }

    private static Set<UUID> idsDe(List<Fila> filas, String tipo, Function<Fila, UUID> id) {
        return filas.stream().filter(fila -> tipo.equals(fila.tipo())).map(id).collect(toSet());
    }
}
