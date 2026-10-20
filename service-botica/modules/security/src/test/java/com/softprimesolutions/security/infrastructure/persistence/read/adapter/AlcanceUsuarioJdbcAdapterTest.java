package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.security.infrastructure.persistence.read.adapter.AlcanceUsuarioJdbcAdapter.Fila;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class AlcanceUsuarioJdbcAdapterTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID EMPRESA = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO = UUID.randomUUID();
    private static final UUID ALMACEN = UUID.randomUUID();
    private static final UUID TERMINAL = UUID.randomUUID();

    private final JdbcClient jdbc = mock(JdbcClient.class);
    private final JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
    private final AlcanceUsuarioJdbcAdapter adapter = new AlcanceUsuarioJdbcAdapter(jdbc);

    @SuppressWarnings("unchecked")
    private void respondWith(List<Fila> filas) {
        var query = (JdbcClient.MappedQuerySpec<Fila>) mock(JdbcClient.MappedQuerySpec.class);
        when(query.list()).thenReturn(filas);
        when(jdbc.sql(anyString())).thenReturn(statement);
        when(statement.query(ArgumentMatchers.<RowMapper<Fila>>any())).thenReturn(query);
    }

    @Test
    void collectsTheResourcesOfEveryAssignmentByScopeType() {
        respondWith(List.of(
                new Fila("EMPRESA", EMPRESA, null, null, null),
                new Fila("ESTABLECIMIENTO", EMPRESA, ESTABLECIMIENTO, null, null),
                new Fila("ALMACEN", EMPRESA, ESTABLECIMIENTO, ALMACEN, null),
                new Fila("TERMINAL", EMPRESA, ESTABLECIMIENTO, null, TERMINAL)));

        var alcance = adapter.alcanceDe(TENANT, USUARIO);

        assertThat(alcance.global()).isFalse();
        assertThat(alcance.empresas()).containsExactly(EMPRESA);
        assertThat(alcance.establecimientos()).containsExactly(ESTABLECIMIENTO);
        assertThat(alcance.almacenes()).containsExactly(ALMACEN);
        assertThat(alcance.terminales()).containsExactly(TERMINAL);
        verify(statement).param("tenantId", TENANT);
        verify(statement).param("usuarioId", USUARIO);
    }

    @Test
    void reportsAGlobalAssignmentWithoutAnyResource() {
        respondWith(List.of(new Fila("GLOBAL", null, null, null, null)));

        var alcance = adapter.alcanceDe(TENANT, USUARIO);

        assertThat(alcance.global()).isTrue();
        assertThat(alcance.empresas()).isEmpty();
        assertThat(alcance.terminales()).isEmpty();
    }

    @Test
    void returnsAnEmptyScopeWhenThereAreNoCurrentAssignments() {
        respondWith(List.of());

        var alcance = adapter.alcanceDe(TENANT, USUARIO);

        assertThat(alcance.global()).isFalse();
        assertThat(alcance.establecimientos()).isEmpty();
    }

    @Test
    void mapsEveryColumnOfTheAssignmentRow() throws SQLException {
        var rs = mock(ResultSet.class);
        when(rs.getString("tipo_ambito")).thenReturn("TERMINAL");
        when(rs.getObject("empresa_uuid", UUID.class)).thenReturn(EMPRESA);
        when(rs.getObject("establecimiento_uuid", UUID.class)).thenReturn(ESTABLECIMIENTO);
        when(rs.getObject("almacen_uuid", UUID.class)).thenReturn(null);
        when(rs.getObject("terminal_uuid", UUID.class)).thenReturn(TERMINAL);

        assertThat(AlcanceUsuarioJdbcAdapter.fila(rs, 0))
                .isEqualTo(new Fila("TERMINAL", EMPRESA, ESTABLECIMIENTO, null, TERMINAL));
    }
}
