package com.softprimesolutions.organizacion.infrastructure.persistence.read.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class OrganizacionJdbcReadRepositoryTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID PARENT = UUID.randomUUID();
    private static final UUID ROW_ID = UUID.randomUUID();
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private final JdbcClient jdbcClient = mock(JdbcClient.class);
    private final OrganizacionJdbcReadRepository repository = new OrganizacionJdbcReadRepository(jdbcClient);
    private final List<String> executedSql = new ArrayList<>();
    private final Map<String, Object> boundParams = new HashMap<>();
    private List<Map<String, Object>> rows = List.of();
    private long countValue;

    OrganizacionJdbcReadRepositoryTest() {
        when(jdbcClient.sql(anyString())).thenAnswer(invocation -> statement(invocation.getArgument(0)));
    }

    private JdbcClient.StatementSpec statement(String sql) {
        return mock(JdbcClient.StatementSpec.class, invocation -> {
            var method = invocation.getMethod();
            if (method.getReturnType().equals(JdbcClient.StatementSpec.class)) {
                boundParams.put(invocation.getArgument(0), invocation.getArgument(1));
                return invocation.getMock();
            }
            executedSql.add(sql);
            return mappedQuery(invocation.getArgument(0));
        });
    }

    private JdbcClient.MappedQuerySpec<?> mappedQuery(Object queryArgument) {
        return mock(JdbcClient.MappedQuerySpec.class, invocation -> switch (invocation.getMethod().getName()) {
            case "list" -> mapAll(queryArgument);
            case "optional" -> mapAll(queryArgument).stream().findFirst();
            default -> countValue;
        });
    }

    private List<?> mapAll(Object queryArgument) throws SQLException {
        var mapper = (RowMapper<?>) queryArgument;
        var mapped = new ArrayList<Object>();
        for (var index = 0; index < rows.size(); index++) {
            mapped.add(mapper.mapRow(resultSet(rows.get(index)), index));
        }
        return mapped;
    }

    private static ResultSet resultSet(Map<String, Object> values) {
        return mock(ResultSet.class, invocation -> {
            var value = values.get((String) invocation.getArgument(0));
            return invocation.getMethod().getReturnType().equals(boolean.class) ? Boolean.TRUE.equals(value) : value;
        });
    }

    private static Map<String, Object> row(String esActivo, OffsetDateTime updatedAt) {
        var values = new HashMap<String, Object>();
        values.put("uuid_publico", ROW_ID);
        values.put("tenant_uuid", TENANT);
        values.put("empresa_uuid", PARENT);
        values.put("establecimiento_uuid", PARENT);
        for (var column : List.of(
                "ruc", "razon_social", "nombre_comercial", "direccion_fiscal", "ubigeo_fiscal", "telefono",
                "email", "sitio_web", "moneda_funcional", "zona_horaria", "estado", "codigo", "nombre",
                "tipo_establecimiento", "categoria_regulatoria_codigo", "codigo_anexo_sunat", "codigo_digemid",
                "direccion", "ubigeo", "referencia", "perfil_operacion", "estado_operativo", "tipo",
                "serie_boleta_defecto", "serie_factura_defecto", "numero_serie_equipo", "hostname",
                "ip_equipo", "impresora_codigo")) {
            values.put(column, column);
        }
        for (var column : List.of(
                "permite_venta_online", "es_principal", "permite_delivery", "permite_lotes",
                "permite_vencimiento", "permite_venta", "permite_despacho", "control_temperatura",
                "store_edge_habilitado")) {
            values.put(column, true);
        }
        for (var column : List.of("latitud", "longitud", "temperatura_min_c", "temperatura_max_c")) {
            values.put(column, BigDecimal.ONE);
        }
        values.put("es_activo", esActivo);
        values.put("created_at", CREATED_AT);
        values.put("updated_at", updatedAt);
        return values;
    }

    private void givenRows(String esActivo, OffsetDateTime updatedAt) {
        rows = List.of(row(esActivo, updatedAt));
    }

    @Test
    void mapsEmpresasAndNormalizesTheSearchTerm() {
        givenRows("1", UPDATED_AT);

        var empresas = repository.findEmpresas(TENANT, "  Boti ", 20, 10);

        assertThat(empresas).singleElement().satisfies(empresa -> {
            assertThat(empresa.uuidPublico()).isEqualTo(ROW_ID);
            assertThat(empresa.tenantUuid()).isEqualTo(TENANT);
            assertThat(empresa.ruc()).isEqualTo("ruc");
            assertThat(empresa.permiteVentaOnline()).isTrue();
            assertThat(empresa.createdAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
            assertThat(empresa.updatedAt()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
        });
        assertThat(boundParams).containsEntry("tenantId", TENANT).containsEntry("search", "boti")
                .containsEntry("pattern", "%boti%").containsEntry("limit", 10).containsEntry("offset", 20);
    }

    @Test
    void treatsNullSearchAsEmpty() {
        givenRows("1", null);

        var empresas = repository.findEmpresas(TENANT, null, 0, 5);

        assertThat(empresas).singleElement().satisfies(empresa -> assertThat(empresa.updatedAt()).isNull());
        assertThat(boundParams).containsEntry("search", "").containsEntry("pattern", "%%");
    }

    @Test
    void countsEmpresasWithTheSameFilterAsTheListing() {
        countValue = 7;

        assertThat(repository.countEmpresas(TENANT, "x")).isEqualTo(7);
        assertThat(executedSql).singleElement().asString().startsWith("SELECT COUNT(*)").contains("empresa_operadora");
    }

    @Test
    void findsEmpresaByIdOrEmpty() {
        givenRows("1", UPDATED_AT);
        assertThat(repository.findEmpresaById(TENANT, ROW_ID)).isPresent();
        assertThat(boundParams).containsEntry("empresaId", ROW_ID);

        rows = List.of();
        assertThat(repository.findEmpresaById(TENANT, ROW_ID)).isEqualTo(Optional.empty());
    }

    @Test
    void listsAllActiveEmpresas() {
        givenRows("1", UPDATED_AT);

        assertThat(repository.findAllEmpresasActivas(TENANT)).hasSize(1);
        assertThat(executedSql).singleElement().asString().contains("es_activo = '1'");
    }

    @Test
    void mapsEstablecimientosScopedToAnEmpresa() {
        givenRows("1", UPDATED_AT);

        var establecimientos = repository.findEstablecimientos(TENANT, PARENT, "botica", 0, 10);

        assertThat(establecimientos).singleElement().satisfies(establecimiento -> {
            assertThat(establecimiento.empresaUuid()).isEqualTo(PARENT);
            assertThat(establecimiento.latitud()).isEqualTo(BigDecimal.ONE);
            assertThat(establecimiento.esPrincipal()).isTrue();
            assertThat(establecimiento.estadoOperativo()).isEqualTo("estado_operativo");
        });
        assertThat(boundParams).containsEntry("empresaId", PARENT).containsEntry("search", "botica");
    }

    @Test
    void passesNullEmpresaFilterThroughToListAllEstablecimientos() {
        givenRows("1", UPDATED_AT);
        countValue = 3;

        repository.findEstablecimientos(TENANT, null, "", 0, 10);
        assertThat(repository.countEstablecimientos(TENANT, null, "")).isEqualTo(3);

        assertThat(boundParams).containsEntry("empresaId", null);
    }

    @Test
    void findsEstablecimientoByIdOrEmpty() {
        givenRows("1", UPDATED_AT);
        assertThat(repository.findEstablecimientoById(TENANT, ROW_ID)).isPresent();
        assertThat(boundParams).containsEntry("establecimientoId", ROW_ID);

        rows = List.of();
        assertThat(repository.findEstablecimientoById(TENANT, ROW_ID)).isEmpty();
    }

    @Test
    void listsAllActiveEstablecimientos() {
        givenRows("1", UPDATED_AT);

        assertThat(repository.findAllEstablecimientosActivos(TENANT)).hasSize(1);
    }

    @Test
    void mapsActiveAlmacenesIncludingTheirActiveFlag() {
        givenRows("1", UPDATED_AT);

        var almacenes = repository.findAlmacenes(TENANT, PARENT, "alm", 0, 10);

        assertThat(almacenes).singleElement().satisfies(almacen -> {
            assertThat(almacen.establecimientoUuid()).isEqualTo(PARENT);
            assertThat(almacen.activo()).isTrue();
            assertThat(almacen.temperaturaMaxC()).isEqualTo(BigDecimal.ONE);
        });
        assertThat(boundParams).containsEntry("establecimientoId", PARENT);
    }

    @Test
    void mapsInactiveAlmacenesWithoutFilteringThemOut() {
        givenRows("0", UPDATED_AT);
        countValue = 1;

        assertThat(repository.findAlmacenes(TENANT, null, "", 0, 10))
                .singleElement().satisfies(almacen -> assertThat(almacen.activo()).isFalse());
        assertThat(repository.countAlmacenes(TENANT, null, "")).isEqualTo(1);
        assertThat(executedSql).noneMatch(sql -> sql.contains("es_activo = '1'"));
    }

    @Test
    void findsAlmacenByIdOrEmpty() {
        givenRows("1", UPDATED_AT);
        assertThat(repository.findAlmacenById(TENANT, ROW_ID)).isPresent();
        assertThat(boundParams).containsEntry("almacenId", ROW_ID);

        rows = List.of();
        assertThat(repository.findAlmacenById(TENANT, ROW_ID)).isEmpty();
    }

    @Test
    void listsEveryAlmacenForTheCorporateStructure() {
        givenRows("0", UPDATED_AT);

        assertThat(repository.findAllAlmacenes(TENANT)).hasSize(1);
    }

    @Test
    void mapsTerminales() {
        givenRows("1", UPDATED_AT);
        countValue = 4;

        var terminales = repository.findTerminales(TENANT, PARENT, "pos", 0, 10);

        assertThat(terminales).singleElement().satisfies(terminal -> {
            assertThat(terminal.establecimientoUuid()).isEqualTo(PARENT);
            assertThat(terminal.ipEquipo()).isEqualTo("ip_equipo");
            assertThat(terminal.storeEdgeHabilitado()).isTrue();
        });
        assertThat(repository.countTerminales(TENANT, PARENT, "pos")).isEqualTo(4);
    }

    @Test
    void findsTerminalByIdOrEmpty() {
        givenRows("1", UPDATED_AT);
        assertThat(repository.findTerminalById(TENANT, ROW_ID)).isPresent();
        assertThat(boundParams).containsEntry("terminalId", ROW_ID);

        rows = List.of();
        assertThat(repository.findTerminalById(TENANT, ROW_ID)).isEmpty();
    }

    @Test
    void listsAllActiveTerminales() {
        givenRows("1", UPDATED_AT);

        assertThat(repository.findAllTerminalesActivos(TENANT)).hasSize(1);
    }
}
