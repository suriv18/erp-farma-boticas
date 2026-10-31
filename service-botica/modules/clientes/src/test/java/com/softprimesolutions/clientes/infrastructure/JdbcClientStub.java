package com.softprimesolutions.clientes.infrastructure;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcClientStub {

    private final JdbcClient client = mock(JdbcClient.class);
    private final List<String> executedSql = new ArrayList<>();
    private final Map<String, Object> boundParams = new HashMap<>();
    private Function<String, Object> results = sql -> null;

    public JdbcClientStub() {
        when(client.sql(anyString())).thenAnswer(invocation -> statement(invocation.getArgument(0)));
    }

    public JdbcClient client() {
        return client;
    }

    public List<String> executedSql() {
        return executedSql;
    }

    public Map<String, Object> boundParams() {
        return boundParams;
    }

    public void answering(Function<String, Object> results) {
        this.results = results;
    }

    private JdbcClient.StatementSpec statement(String sql) {
        return mock(JdbcClient.StatementSpec.class, invocation -> {
            var method = invocation.getMethod().getName();
            if (method.equals("query")) return mappedQuery(sql);
            if (method.equals("update")) {
                executedSql.add(sql);
                return results.apply(sql);
            }
            boundParams.put(invocation.getArgument(0), invocation.getArgument(1));
            return invocation.getMock();
        });
    }

    private JdbcClient.MappedQuerySpec<?> mappedQuery(String sql) {
        return mock(JdbcClient.MappedQuerySpec.class, invocation -> {
            executedSql.add(sql);
            return results.apply(sql);
        });
    }
}
