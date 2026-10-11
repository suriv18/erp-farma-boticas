package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import com.softprimesolutions.security.api.AutorizacionAmbito;
import com.softprimesolutions.security.api.RecursoAmbito;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AutorizacionAmbitoJdbcAdapter implements AutorizacionAmbito {

    private static final String PERMITE = """
            SELECT EXISTS (
                SELECT 1
                  FROM sch_seguridad.usuario_rol_ambito a
                  JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
                  JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
                  JOIN sch_seguridad.rol_permiso rp ON rp.rol_id = r.id AND rp.tenant_id = r.tenant_id
                  JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                  JOIN sch_admin.tenant t ON t.id = a.tenant_id
                  LEFT JOIN sch_organizacion.establecimiento_farmaceutico e
                    ON e.tenant_id = a.tenant_id AND e.uuid_publico = :establecimientoId
                 WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :usuarioId AND p.codigo = :permiso
                   AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO' AND a.estado = 'ACTIVO'
                   AND rp.estado = 'ACTIVO' AND p.estado = 'ACTIVO'
                   AND a.vigente_desde <= CURRENT_TIMESTAMP
                   AND (a.vigente_hasta IS NULL OR a.vigente_hasta >= CURRENT_TIMESTAMP)
                   AND (a.tipo_ambito = 'GLOBAL'
                        OR (a.tipo_ambito = 'EMPRESA' AND a.empresa_id = e.empresa_id)
                        OR (a.tipo_ambito = 'ESTABLECIMIENTO' AND a.establecimiento_id = e.id)
                        OR (a.tipo_ambito = 'ALMACEN' AND a.almacen_id = (
                                SELECT al.id FROM sch_organizacion.almacen al
                                 WHERE al.tenant_id = a.tenant_id AND al.establecimiento_id = e.id
                                   AND al.uuid_publico = :almacenId))
                        OR (a.tipo_ambito = 'TERMINAL' AND a.terminal_id = (
                                SELECT tp.id FROM sch_organizacion.terminal_pos tp
                                 WHERE tp.tenant_id = a.tenant_id AND tp.establecimiento_id = e.id
                                   AND tp.uuid_publico = :terminalId))))
            """;

    private final JdbcClient jdbc;

    public AutorizacionAmbitoJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean permite(UUID tenantId, UUID usuarioId, String permiso, RecursoAmbito recurso) {
        return jdbc.sql(PERMITE)
                .param("tenantId", tenantId)
                .param("usuarioId", usuarioId)
                .param("permiso", permiso)
                .param("establecimientoId", recurso.establecimientoId())
                .param("almacenId", recurso.almacenId())
                .param("terminalId", recurso.terminalId())
                .query(Boolean.class)
                .single();
    }
}
