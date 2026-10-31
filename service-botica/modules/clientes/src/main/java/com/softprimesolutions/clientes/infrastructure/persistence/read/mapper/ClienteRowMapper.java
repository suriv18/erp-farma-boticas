package com.softprimesolutions.clientes.infrastructure.persistence.read.mapper;

import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

public final class ClienteRowMapper implements RowMapper<ClienteResult> {

    @Override
    public ClienteResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ClienteResult(
                rs.getObject("uuid_publico", UUID.class), rs.getObject("tenant_uuid", UUID.class),
                rs.getString("tipo_cliente"), rs.getString("tipo_documento"), rs.getString("numero_documento"),
                rs.getString("nombres"), rs.getString("apellidos"), rs.getString("razon_social"),
                rs.getString("email"), rs.getString("telefono"), rs.getObject("fecha_nacimiento", LocalDate.class),
                rs.getString("estado"), rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
