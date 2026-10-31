package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.security.api.RecursoAmbito;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

class AutorizacionAmbitoJdbcAdapterTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO = UUID.randomUUID();
    private static final UUID TERMINAL = UUID.randomUUID();

    private final JdbcClient jdbc = mock(JdbcClient.class);
    private final JdbcClient.StatementSpec statement = mock(JdbcClient.StatementSpec.class, RETURNS_SELF);
    private final AutorizacionAmbitoJdbcAdapter adapter = new AutorizacionAmbitoJdbcAdapter(jdbc);

    @SuppressWarnings("unchecked")
    private void respondWith(boolean permitido) {
        var query = (JdbcClient.MappedQuerySpec<Boolean>) mock(JdbcClient.MappedQuerySpec.class);
        when(query.single()).thenReturn(permitido);
        when(jdbc.sql(anyString())).thenReturn(statement);
        when(statement.query(Boolean.class)).thenReturn(query);
    }

    @Test
    void asksTheDatabaseWithTheActorThePermissionAndTheResourceScope() {
        respondWith(true);

        var permitido = adapter.permite(
                TENANT, USUARIO, "ventas.ventas.registrar", new RecursoAmbito(ESTABLECIMIENTO, null, TERMINAL));

        assertThat(permitido).isTrue();
        verify(statement).param("tenantId", TENANT);
        verify(statement).param("usuarioId", USUARIO);
        verify(statement).param("permiso", "ventas.ventas.registrar");
        verify(statement).param("establecimientoId", ESTABLECIMIENTO);
        verify(statement).param("almacenId", (Object) null);
        verify(statement).param("terminalId", TERMINAL);
    }

    @Test
    void reportsWhenTheActorHasNoAssignmentCoveringTheResource() {
        respondWith(false);

        assertThat(adapter.permite(TENANT, USUARIO, "ventas.ventas.anular", new RecursoAmbito(null, null, null)))
                .isFalse();
    }
}
