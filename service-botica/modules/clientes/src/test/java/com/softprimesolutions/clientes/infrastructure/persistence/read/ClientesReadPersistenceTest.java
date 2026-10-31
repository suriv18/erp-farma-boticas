package com.softprimesolutions.clientes.infrastructure.persistence.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.infrastructure.JdbcClientStub;
import com.softprimesolutions.clientes.infrastructure.persistence.read.adapter.ClientesJdbcReadAdapter;
import com.softprimesolutions.clientes.infrastructure.persistence.read.mapper.ClienteRowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientesReadPersistenceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID CLIENTE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ClientesJdbcReadAdapter adapter = new ClientesJdbcReadAdapter(jdbc.client());

    private static ClienteResult cliente() {
        return new ClienteResult(CLIENTE, TENANT, "NATURAL", "1", "12345678", "Ana", "Pérez", null, null, null,
                null, "ACTIVO", null, NOW, null);
    }

    @Test
    void mapsEveryColumnOfARow() throws SQLException {
        var rs = mock(ResultSet.class);
        when(rs.getObject("uuid_publico", UUID.class)).thenReturn(CLIENTE);
        when(rs.getObject("tenant_uuid", UUID.class)).thenReturn(TENANT);
        when(rs.getString("tipo_cliente")).thenReturn("NATURAL");
        when(rs.getString("tipo_documento")).thenReturn("1");
        when(rs.getString("numero_documento")).thenReturn("12345678");
        when(rs.getString("nombres")).thenReturn("Ana");
        when(rs.getString("apellidos")).thenReturn("Pérez");
        when(rs.getString("razon_social")).thenReturn(null);
        when(rs.getString("email")).thenReturn("ana@correo.pe");
        when(rs.getString("telefono")).thenReturn("999");
        when(rs.getObject("fecha_nacimiento", LocalDate.class)).thenReturn(LocalDate.parse("1990-05-10"));
        when(rs.getString("estado")).thenReturn("ACTIVO");
        when(rs.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));

        var result = new ClienteRowMapper().mapRow(rs, 0);

        assertThat(result).isEqualTo(new ClienteResult(CLIENTE, TENANT, "NATURAL", "1", "12345678", "Ana", "Pérez",
                null, "ana@correo.pe", "999", LocalDate.parse("1990-05-10"), "ACTIVO", null, NOW, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsAPageWithTheNormalizedSearchPatternAndOffset() {
        jdbc.answering(sql -> sql.startsWith("SELECT COUNT(*)") ? (Object) 41L : List.of(cliente()));

        var page = adapter.findClientes(TENANT, "  ANA ", 2, 20);

        assertThat(page.items()).containsExactly(cliente());
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(41L);
        assertThat(jdbc.boundParams())
                .containsEntry("tenantId", TENANT)
                .containsEntry("search", "ana")
                .containsEntry("pattern", "%ana%")
                .containsEntry("limit", 20)
                .containsEntry("offset", 40);
        assertThat(jdbc.executedSql()).hasSize(2);
    }

    @Test
    void treatsAMissingSearchAsEmpty() {
        jdbc.answering(sql -> sql.startsWith("SELECT COUNT(*)") ? (Object) 0L : List.of());

        var page = adapter.findClientes(TENANT, null, 0, 20);

        assertThat(page.items()).isEmpty();
        assertThat(jdbc.boundParams()).containsEntry("search", "").containsEntry("pattern", "%%");
    }

    @Test
    void findsAClientByItsIdentifier() {
        jdbc.answering(sql -> Optional.of(cliente()));

        assertThat(adapter.findClienteById(TENANT, CLIENTE)).contains(cliente());
        assertThat(jdbc.boundParams()).containsEntry("tenantId", TENANT).containsEntry("clienteId", CLIENTE);
    }

    @Test
    void findsAClientByItsDocument() {
        jdbc.answering(sql -> Optional.empty());

        assertThat(adapter.findClienteByDocumento(TENANT, "1", "12345678")).isEmpty();
        assertThat(jdbc.boundParams())
                .containsEntry("tipoDocumento", "1")
                .containsEntry("numeroDocumento", "12345678");
    }

    @Test
    void mapsTheBlockReasonAndTheUpdateInstant() throws SQLException {
        var rs = mock(ResultSet.class);
        when(rs.getString("motivo_bloqueo")).thenReturn("Fraude");
        when(rs.getString("estado")).thenReturn("BLOQUEADO");
        when(rs.getObject("created_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC));
        var updated = NOW.plusSeconds(3600);
        when(rs.getObject("updated_at", OffsetDateTime.class)).thenReturn(OffsetDateTime.ofInstant(updated, ZoneOffset.UTC));

        var result = new ClienteRowMapper().mapRow(rs, 0);

        assertThat(result.estado()).isEqualTo("BLOQUEADO");
        assertThat(result.motivoBloqueo()).isEqualTo("Fraude");
        assertThat(result.updatedAt()).isEqualTo(updated);
    }
}
