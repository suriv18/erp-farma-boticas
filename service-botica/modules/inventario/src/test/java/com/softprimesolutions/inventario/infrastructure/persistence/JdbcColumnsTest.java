package com.softprimesolutions.inventario.infrastructure.persistence;

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

    @Test
    void readsTypedColumnsAndConvertsTimestampsToInstants() throws SQLException {
        var id = UUID.randomUUID();
        var values = new HashMap<String, Object>();
        values.put("id", id);
        values.put("fecha", LocalDate.of(2026, 6, 15));
        values.put("momento", OffsetDateTime.of(2026, 6, 15, 7, 0, 0, 0, ZoneOffset.ofHours(-5)));
        values.put("vacio", null);
        var resultSet = JdbcClientStub.resultSet(values);

        assertThat(JdbcColumns.uuid(resultSet, "id")).isEqualTo(id);
        assertThat(JdbcColumns.date(resultSet, "fecha")).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(JdbcColumns.instant(resultSet, "momento")).isEqualTo(Instant.parse("2026-06-15T12:00:00Z"));
        assertThat(JdbcColumns.instant(resultSet, "vacio")).isNull();
    }

    @Test
    void convertsInstantsToUtcOffsetDateTimesKeepingNulls() {
        var instant = Instant.parse("2026-06-15T12:00:00Z");

        assertThat(JdbcColumns.offset(instant)).isEqualTo(OffsetDateTime.of(2026, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC));
        assertThat(JdbcColumns.offset(null)).isNull();
    }
}
