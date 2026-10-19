package com.softprimesolutions.compras.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JdbcColumnsTest {

    private static final UUID ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final Instant MOMENTO = Instant.parse("2026-06-15T12:00:00Z");

    @Test
    void readsTypedColumnsFromTheResultSet() throws SQLException {
        var values = new HashMap<String, Object>();
        values.put("id", ID);
        values.put("fecha", LocalDate.of(2026, 6, 15));
        values.put("momento", MOMENTO.atOffset(ZoneOffset.UTC));
        var rs = JdbcClientStub.resultSet(values);

        assertThat(JdbcColumns.uuid(rs, "id")).isEqualTo(ID);
        assertThat(JdbcColumns.date(rs, "fecha")).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(JdbcColumns.instant(rs, "momento")).isEqualTo(MOMENTO);
    }

    @Test
    void nullColumnsStayNull() throws SQLException {
        var rs = JdbcClientStub.resultSet(new HashMap<>());

        assertThat(JdbcColumns.uuid(rs, "id")).isNull();
        assertThat(JdbcColumns.date(rs, "fecha")).isNull();
        assertThat(JdbcColumns.instant(rs, "momento")).isNull();
    }

    @Test
    void convertsInstantsToUtcOffsets() {
        assertThat(JdbcColumns.offset(MOMENTO)).isEqualTo(OffsetDateTime.parse("2026-06-15T12:00:00Z"));
        assertThat(JdbcColumns.offset(null)).isNull();
    }
}
