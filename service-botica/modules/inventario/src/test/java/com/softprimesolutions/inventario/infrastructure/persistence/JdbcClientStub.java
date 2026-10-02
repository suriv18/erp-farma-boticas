package com.softprimesolutions.inventario.infrastructure.persistence;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class JdbcClientStub {

    public record Statement(String sql, Map<String, Object> params) {
    }

    private record Rule(
            String fragment, List<Map<String, Object>> rows, List<Object> scalars, int updateCount,
            RuntimeException failure) {
    }

    private final JdbcClient client = mock(JdbcClient.class);
    private final List<Rule> rules = new ArrayList<>();
    private final List<Statement> statements = new ArrayList<>();

    public JdbcClientStub() {
        when(client.sql(anyString())).thenAnswer(invocation -> statement(invocation.getArgument(0)));
    }

    public JdbcClient client() {
        return client;
    }

    public List<Statement> statements() {
        return statements;
    }

    public Statement statementContaining(String fragment) {
        return statements.stream().filter(statement -> statement.sql().contains(fragment)).findFirst()
                .orElseThrow(() -> new AssertionError("No se ejecutó SQL con: " + fragment));
    }

    public JdbcClientStub rows(String fragment, Map<String, Object> row) {
        rules.add(new Rule(fragment, List.of(row), List.of(), 1, null));
        return this;
    }

    public JdbcClientStub scalar(String fragment, Object value) {
        rules.add(new Rule(fragment, List.of(), List.of(value), 1, null));
        return this;
    }

    public JdbcClientStub updates(String fragment, int count) {
        rules.add(new Rule(fragment, List.of(), List.of(), count, null));
        return this;
    }

    public JdbcClientStub failsWith(String fragment, RuntimeException failure) {
        rules.add(new Rule(fragment, List.of(), List.of(), 1, failure));
        return this;
    }

    public static ResultSet resultSet(Map<String, Object> values) {
        return mock(ResultSet.class, invocation -> {
            var value = values.get((String) invocation.getArgument(0));
            var type = invocation.getMethod().getReturnType();
            if (type == boolean.class) return Boolean.TRUE.equals(value);
            if (type == long.class) return value == null ? 0L : ((Number) value).longValue();
            return value;
        });
    }

    private JdbcClient.StatementSpec statement(String sql) {
        var params = new LinkedHashMap<String, Object>();
        statements.add(new Statement(sql, params));
        var rule = rules.stream().filter(candidate -> sql.contains(candidate.fragment())).findFirst()
                .orElse(new Rule("", List.of(), List.of(), 1, null));
        return mock(JdbcClient.StatementSpec.class, invocation -> {
            var method = invocation.getMethod();
            if (method.getReturnType().equals(JdbcClient.StatementSpec.class)) {
                params.put(invocation.getArgument(0), invocation.getArgument(1));
                return invocation.getMock();
            }
            if (method.getName().equals("update")) {
                if (rule.failure() != null) throw rule.failure();
                return rule.updateCount();
            }
            return mappedQuery(rule, invocation.getArgument(0));
        });
    }

    private static JdbcClient.MappedQuerySpec<?> mappedQuery(Rule rule, Object queryArgument) {
        var mapped = queryArgument instanceof RowMapper<?> mapper ? mapRows(mapper, rule.rows()) : rule.scalars();
        return mock(JdbcClient.MappedQuerySpec.class, invocation -> switch (invocation.getMethod().getName()) {
            case "list" -> mapped;
            case "optional" -> mapped.stream().findFirst();
            case "single" -> mapped.getFirst();
            default -> throw new UnsupportedOperationException(invocation.getMethod().getName());
        });
    }

    private static List<Object> mapRows(RowMapper<?> mapper, List<Map<String, Object>> rows) {
        var mapped = new ArrayList<Object>();
        try {
            for (var index = 0; index < rows.size(); index++) {
                mapped.add(mapper.mapRow(resultSet(rows.get(index)), index));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
        return mapped;
    }
}
