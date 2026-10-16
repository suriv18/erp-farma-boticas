package com.softprimesolutions.ventas.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JdbcColumnsTest {

    private final ResultSet rs = mock(ResultSet.class);

    @Test
    void readsUuidsDatesAndInstants() throws SQLException {
        var id = UUID.randomUUID();
        var fecha = LocalDate.of(2026, 6, 15);
        var momento = OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        when(rs.getObject("id", UUID.class)).thenReturn(id);
        when(rs.getObject("fecha", LocalDate.class)).thenReturn(fecha);
        when(rs.getObject("momento", OffsetDateTime.class)).thenReturn(momento);

        assertThat(JdbcColumns.uuid(rs, "id")).isEqualTo(id);
        assertThat(JdbcColumns.date(rs, "fecha")).isEqualTo(fecha);
        assertThat(JdbcColumns.instant(rs, "momento")).isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
    }

    @Test
    void aMissingTimestampIsNull() throws SQLException {
        assertThat(JdbcColumns.instant(rs, "momento")).isNull();
    }

    @Test
    void convertsInstantsToUtcOffsetsAndKeepsNulls() {
        var instante = Instant.parse("2026-06-15T12:00:00Z");

        assertThat(JdbcColumns.offset(instante)).isEqualTo(OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC));
        assertThat(JdbcColumns.offset(null)).isNull();
    }
}
