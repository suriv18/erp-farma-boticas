package com.softprimesolutions.ventas.infrastructure.persistence.read.adapter;

import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.mapper.VentasApplicationMapper;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.infrastructure.persistence.TurnoRows;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VentasJdbcReadAdapter implements VentasReadPort {

    private final JdbcClient jdbcClient;

    public VentasJdbcReadAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(TurnoRows.POR_ID)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(TurnoRows.ABIERTO_POR_TERMINAL)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }
}
