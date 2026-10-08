package com.softprimesolutions.inventario.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

public final class JdbcColumns {

    private JdbcColumns() {
    }

    public static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    public static Instant instant(ResultSet rs, String column) throws SQLException {
        return Optional.ofNullable(rs.getObject(column, OffsetDateTime.class))
                .map(OffsetDateTime::toInstant)
                .orElse(null);
    }

    public static OffsetDateTime offset(Instant value) {
        return Optional.ofNullable(value).map(instant -> instant.atOffset(ZoneOffset.UTC)).orElse(null);
    }
}
