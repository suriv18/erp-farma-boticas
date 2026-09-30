package com.softprimesolutions.organizacion.infrastructure.persistence.write.adapter;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.mapper.OrganizacionWriteMapper;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.AlmacenJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.EmpresaOperadoraJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.EstablecimientoJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.TerminalPosJpaRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrganizacionJpaWriteAdapter implements OrganizacionWritePort {

    private static final String SERIE_BOLETA_EN_USO = """
            SELECT EXISTS (
                SELECT 1 FROM sch_organizacion.terminal_pos
                 WHERE tenant_id = :tenantId AND empresa_id = :empresaId AND es_activo = '1'
                   AND serie_boleta_defecto = :serie AND uuid_publico <> :terminalId)
            """;

    private static final String SERIE_FACTURA_EN_USO = """
            SELECT EXISTS (
                SELECT 1 FROM sch_organizacion.terminal_pos
                 WHERE tenant_id = :tenantId AND empresa_id = :empresaId AND es_activo = '1'
                   AND serie_factura_defecto = :serie AND uuid_publico <> :terminalId)
            """;

    private final EmpresaOperadoraJpaRepository empresaRepository;
    private final EstablecimientoJpaRepository establecimientoRepository;
    private final AlmacenJpaRepository almacenRepository;
    private final TerminalPosJpaRepository terminalRepository;
    private final JdbcClient jdbcClient;

    public OrganizacionJpaWriteAdapter(
            EmpresaOperadoraJpaRepository empresaRepository,
            EstablecimientoJpaRepository establecimientoRepository,
            AlmacenJpaRepository almacenRepository,
            TerminalPosJpaRepository terminalRepository,
            JdbcClient jdbcClient) {
        this.empresaRepository = empresaRepository;
        this.establecimientoRepository = establecimientoRepository;
        this.almacenRepository = almacenRepository;
        this.terminalRepository = terminalRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveEmpresaOutcome save(EmpresaOperadora empresa) {
        var tenantId = findTenantId(empresa.tenantId().value());
        if (tenantId.isEmpty()) return SaveEmpresaOutcome.TENANT_NOT_FOUND;
        var existing = empresaRepository.findByUuidPublico(empresa.id().value());
        if (existing.isEmpty() && empresaRepository.existsByTenantIdAndRuc(tenantId.get(), empresa.ruc())) {
            return SaveEmpresaOutcome.DUPLICATE_RUC;
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.empresa_operadora
                                   SET razon_social = :razonSocial, nombre_comercial = :nombreComercial,
                                       direccion_fiscal = :direccionFiscal, ubigeo_fiscal = :ubigeoFiscal,
                                       telefono = :telefono, email = :email, sitio_web = :sitioWeb,
                                       moneda_funcional = :monedaFuncional, zona_horaria = :zonaHoraria,
                                       permite_venta_online = :permiteVentaOnline, estado = :estado,
                                       updated_at = :updatedAt
                                 WHERE uuid_publico = :empresaId
                                """)
                        .param("razonSocial", empresa.razonSocial())
                        .param("nombreComercial", empresa.nombreComercial())
                        .param("direccionFiscal", empresa.direccionFiscal())
                        .param("ubigeoFiscal", empresa.ubigeoFiscal())
                        .param("telefono", empresa.telefono())
                        .param("email", empresa.email())
                        .param("sitioWeb", empresa.sitioWeb())
                        .param("monedaFuncional", empresa.monedaFuncional())
                        .param("zonaHoraria", empresa.zonaHoraria())
                        .param("permiteVentaOnline", empresa.permiteVentaOnline())
                        .param("estado", empresa.estado().name())
                        .param("updatedAt", toOffsetDateTime(empresa.updatedAt()))
                        .param("empresaId", empresa.id().value())
                        .update();
                return SaveEmpresaOutcome.UPDATED;
            }
            empresaRepository.saveAndFlush(OrganizacionWriteMapper.toEntity(empresa, tenantId.get()));
            return SaveEmpresaOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveEmpresaOutcome.DUPLICATE_RUC;
        }
    }

    @Override
    @Transactional
    public SaveEstablecimientoOutcome save(Establecimiento establecimiento) {
        var tenantId = findTenantId(establecimiento.tenantId().value());
        var empresaId = findEmpresaId(establecimiento.tenantId().value(), establecimiento.empresaId().value());
        if (tenantId.isEmpty() || empresaId.isEmpty()) return SaveEstablecimientoOutcome.EMPRESA_NOT_FOUND;

        var existing = establecimientoRepository.findByUuidPublico(establecimiento.id().value());
        if (existing.isEmpty()) {
            if (!findEmpresaEstado(establecimiento.tenantId().value(), establecimiento.empresaId().value())
                    .admiteAltasDeHijos()) {
                return SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA;
            }
            if (establecimientoRepository.existsByTenantIdAndCodigo(tenantId.get(), establecimiento.codigo())) {
                return SaveEstablecimientoOutcome.DUPLICATE_CODIGO;
            }
            if (establecimiento.codigoDigemid() != null && establecimientoRepository
                    .existsByTenantIdAndCodigoDigemid(tenantId.get(), establecimiento.codigoDigemid())) {
                return SaveEstablecimientoOutcome.DUPLICATE_DIGEMID;
            }
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.establecimiento_farmaceutico
                                   SET nombre = :nombre, tipo_establecimiento = :tipoEstablecimiento,
                                       categoria_regulatoria_codigo = :categoriaRegulatoriaCodigo,
                                       codigo_anexo_sunat = :codigoAnexoSunat, codigo_digemid = :codigoDigemid,
                                       direccion = :direccion, ubigeo = :ubigeo, referencia = :referencia,
                                       latitud = :latitud, longitud = :longitud, telefono = :telefono,
                                       email = :email, es_principal = :esPrincipal,
                                       permite_venta_online = :permiteVentaOnline,
                                       permite_delivery = :permiteDelivery, perfil_operacion = :perfilOperacion,
                                       zona_horaria = :zonaHoraria, estado_operativo = :estadoOperativo,
                                       updated_at = :updatedAt
                                 WHERE uuid_publico = :establecimientoId
                                """)
                        .param("nombre", establecimiento.nombre())
                        .param("tipoEstablecimiento", establecimiento.tipoEstablecimiento().name())
                        .param("categoriaRegulatoriaCodigo", establecimiento.categoriaRegulatoriaCodigo())
                        .param("codigoAnexoSunat", establecimiento.codigoAnexoSunat())
                        .param("codigoDigemid", establecimiento.codigoDigemid())
                        .param("direccion", establecimiento.direccion())
                        .param("ubigeo", establecimiento.ubigeo())
                        .param("referencia", establecimiento.referencia())
                        .param("latitud", establecimiento.latitud())
                        .param("longitud", establecimiento.longitud())
                        .param("telefono", establecimiento.telefono())
                        .param("email", establecimiento.email())
                        .param("esPrincipal", establecimiento.esPrincipal())
                        .param("permiteVentaOnline", establecimiento.permiteVentaOnline())
                        .param("permiteDelivery", establecimiento.permiteDelivery())
                        .param("perfilOperacion", establecimiento.perfilOperacion().name())
                        .param("zonaHoraria", establecimiento.zonaHoraria())
                        .param("estadoOperativo", establecimiento.estadoOperativo().name())
                        .param("updatedAt", toOffsetDateTime(establecimiento.updatedAt()))
                        .param("establecimientoId", establecimiento.id().value())
                        .update();
                return SaveEstablecimientoOutcome.UPDATED;
            }
            establecimientoRepository.saveAndFlush(
                    OrganizacionWriteMapper.toEntity(establecimiento, tenantId.get(), empresaId.get()));
            return SaveEstablecimientoOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveEstablecimientoOutcome.DUPLICATE_CODIGO;
        }
    }

    @Override
    @Transactional
    public SaveAlmacenOutcome save(Almacen almacen) {
        var tenantId = findTenantId(almacen.tenantId().value());
        var parent = findEstablecimientoConEmpresa(almacen.tenantId().value(), almacen.establecimientoId().value());
        if (tenantId.isEmpty() || parent.isEmpty()) return SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND;

        var existing = almacenRepository.findByUuidPublico(almacen.id().value());
        if (existing.isEmpty()) {
            if (!findEstablecimientoEstado(almacen.tenantId().value(), almacen.establecimientoId().value())
                    .admiteAltasDeHijos()) {
                return SaveAlmacenOutcome.ESTABLECIMIENTO_NO_OPERATIVO;
            }
            if (almacenRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                    tenantId.get(), parent.get().establecimientoId(), almacen.codigo())) {
                return SaveAlmacenOutcome.DUPLICATE_CODIGO;
            }
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.almacen
                                   SET nombre = :nombre, tipo = :tipo, permite_lotes = :permiteLotes,
                                       permite_vencimiento = :permiteVencimiento, permite_venta = :permiteVenta,
                                       permite_despacho = :permiteDespacho, control_temperatura = :controlTemperatura,
                                       temperatura_min_c = :temperaturaMinC, temperatura_max_c = :temperaturaMaxC,
                                       es_activo = :esActivo, updated_at = :updatedAt
                                 WHERE uuid_publico = :almacenId
                                """)
                        .param("nombre", almacen.nombre())
                        .param("tipo", almacen.tipo().name())
                        .param("permiteLotes", almacen.permiteLotes())
                        .param("permiteVencimiento", almacen.permiteVencimiento())
                        .param("permiteVenta", almacen.permiteVenta())
                        .param("permiteDespacho", almacen.permiteDespacho())
                        .param("controlTemperatura", almacen.controlTemperatura())
                        .param("temperaturaMinC", almacen.temperaturaMinC())
                        .param("temperaturaMaxC", almacen.temperaturaMaxC())
                        .param("esActivo", OrganizacionWriteMapper.activoFlag(almacen.activo()))
                        .param("updatedAt", toOffsetDateTime(almacen.updatedAt()))
                        .param("almacenId", almacen.id().value())
                        .update();
                return SaveAlmacenOutcome.UPDATED;
            }
            almacenRepository.saveAndFlush(OrganizacionWriteMapper.toEntity(
                    almacen, tenantId.get(), parent.get().empresaId(), parent.get().establecimientoId()));
            return SaveAlmacenOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveAlmacenOutcome.DUPLICATE_CODIGO;
        }
    }

    @Override
    @Transactional
    public SaveTerminalOutcome save(TerminalPos terminal) {
        var tenantId = findTenantId(terminal.tenantId().value());
        var parent = findEstablecimientoConEmpresa(terminal.tenantId().value(), terminal.establecimientoId().value());
        if (tenantId.isEmpty() || parent.isEmpty()) return SaveTerminalOutcome.ESTABLECIMIENTO_NOT_FOUND;

        var existing = terminalRepository.findByUuidPublico(terminal.id().value());
        if (existing.isEmpty()) {
            if (!findEstablecimientoEstado(terminal.tenantId().value(), terminal.establecimientoId().value())
                    .admiteAltasDeHijos()) {
                return SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO;
            }
            if (terminalRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                    tenantId.get(), parent.get().establecimientoId(), terminal.codigo())) {
                return SaveTerminalOutcome.DUPLICATE_CODIGO;
            }
        }
        if (serieEnUso(SERIE_BOLETA_EN_USO, tenantId.get(), parent.get().empresaId(),
                terminal.serieBoletaDefecto(), terminal.id().value())) {
            return SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA;
        }
        if (serieEnUso(SERIE_FACTURA_EN_USO, tenantId.get(), parent.get().empresaId(),
                terminal.serieFacturaDefecto(), terminal.id().value())) {
            return SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA;
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_organizacion.terminal_pos
                                   SET nombre = :nombre, serie_boleta_defecto = :serieBoletaDefecto,
                                       serie_factura_defecto = :serieFacturaDefecto,
                                       numero_serie_equipo = :numeroSerieEquipo, hostname = :hostname,
                                       ip_equipo = CAST(:ipEquipo AS inet), impresora_codigo = :impresoraCodigo,
                                       store_edge_habilitado = :storeEdgeHabilitado, estado = :estado,
                                       updated_at = :updatedAt
                                 WHERE uuid_publico = :terminalId
                                """)
                        .param("nombre", terminal.nombre())
                        .param("serieBoletaDefecto", terminal.serieBoletaDefecto())
                        .param("serieFacturaDefecto", terminal.serieFacturaDefecto())
                        .param("numeroSerieEquipo", terminal.numeroSerieEquipo())
                        .param("hostname", terminal.hostname())
                        .param("ipEquipo", terminal.ipEquipo())
                        .param("impresoraCodigo", terminal.impresoraCodigo())
                        .param("storeEdgeHabilitado", terminal.storeEdgeHabilitado())
                        .param("estado", terminal.estado().name())
                        .param("updatedAt", toOffsetDateTime(terminal.updatedAt()))
                        .param("terminalId", terminal.id().value())
                        .update();
                return SaveTerminalOutcome.UPDATED;
            }
            terminalRepository.saveAndFlush(OrganizacionWriteMapper.toEntity(
                    terminal, tenantId.get(), parent.get().empresaId(), parent.get().establecimientoId()));
            return SaveTerminalOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return terminalViolation(exception);
        }
    }

    private boolean serieEnUso(String sql, Long tenantId, Long empresaId, String serie, UUID terminalId) {
        if (serie == null) return false;
        return jdbcClient.sql(sql)
                .param("tenantId", tenantId)
                .param("empresaId", empresaId)
                .param("serie", serie)
                .param("terminalId", terminalId)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    private static SaveTerminalOutcome terminalViolation(DataIntegrityViolationException exception) {
        var message = String.valueOf(exception.getMessage());
        if (message.contains("uk_terminal_pos_serie_boleta")) return SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA;
        if (message.contains("uk_terminal_pos_serie_factura")) return SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA;
        return SaveTerminalOutcome.DUPLICATE_CODIGO;
    }

    @Override
    public boolean tenantExists(UUID tenantId) {
        return findTenantId(tenantId).isPresent();
    }

    @Override
    public boolean empresaBelongsToTenant(UUID empresaId, UUID tenantId) {
        return findEmpresaId(tenantId, empresaId).isPresent();
    }

    @Override
    public boolean establecimientoBelongsToTenant(UUID establecimientoId, UUID tenantId) {
        return establecimientoRepository.findByUuidPublico(establecimientoId)
                .map(establecimiento -> findTenantId(tenantId)
                        .filter(establecimiento.getTenantId()::equals).isPresent())
                .orElse(false);
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid)
                .query(Long.class)
                .optional();
    }

    private Optional<Long> findEmpresaId(UUID tenantUuid, UUID empresaUuid) {
        if (tenantUuid == null || empresaUuid == null) return Optional.empty();
        return jdbcClient.sql("""
                        SELECT e.id
                          FROM sch_organizacion.empresa_operadora e
                          JOIN sch_admin.tenant t ON t.id = e.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND e.uuid_publico = :empresaUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("empresaUuid", empresaUuid)
                .query(Long.class)
                .optional();
    }

    private Optional<EstablecimientoConEmpresa> findEstablecimientoConEmpresa(
            UUID tenantUuid, UUID establecimientoUuid) {
        return jdbcClient.sql("""
                        SELECT s.id AS establecimiento_id, s.empresa_id AS empresa_id
                          FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND s.uuid_publico = :establecimientoUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("establecimientoUuid", establecimientoUuid)
                .query((rs, rowNumber) -> new EstablecimientoConEmpresa(
                        rs.getLong("establecimiento_id"), rs.getLong("empresa_id")))
                .optional();
    }

    private EstadoEmpresaOperadora findEmpresaEstado(UUID tenantUuid, UUID empresaUuid) {
        return jdbcClient.sql("""
                        SELECT e.estado
                          FROM sch_organizacion.empresa_operadora e
                          JOIN sch_admin.tenant t ON t.id = e.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND e.uuid_publico = :empresaUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("empresaUuid", empresaUuid)
                .query(String.class)
                .optional()
                .map(EstadoEmpresaOperadora::valueOf)
                .orElseThrow();
    }

    private EstadoEstablecimiento findEstablecimientoEstado(UUID tenantUuid, UUID establecimientoUuid) {
        return jdbcClient.sql("""
                        SELECT s.estado_operativo
                          FROM sch_organizacion.establecimiento_farmaceutico s
                          JOIN sch_admin.tenant t ON t.id = s.tenant_id
                         WHERE t.uuid_publico = :tenantUuid AND s.uuid_publico = :establecimientoUuid
                        """)
                .param("tenantUuid", tenantUuid)
                .param("establecimientoUuid", establecimientoUuid)
                .query(String.class)
                .optional()
                .map(EstadoEstablecimiento::valueOf)
                .orElseThrow();
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private record EstablecimientoConEmpresa(Long establecimientoId, Long empresaId) {
    }
}
