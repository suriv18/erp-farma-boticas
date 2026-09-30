package com.softprimesolutions.organizacion.infrastructure.persistence.write.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveAlmacenOutcome;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveEmpresaOutcome;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveEstablecimientoOutcome;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort.SaveTerminalOutcome;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.EstadoTerminalPos;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.domain.model.PerfilOperacion;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.organizacion.domain.model.TipoAlmacen;
import com.softprimesolutions.organizacion.domain.model.TipoEstablecimiento;
import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.AlmacenJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EmpresaOperadoraJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EstablecimientoJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.TerminalPosJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.AlmacenJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.EmpresaOperadoraJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.EstablecimientoJpaRepository;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.repository.TerminalPosJpaRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;

class OrganizacionJpaWriteAdapterTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final Long TENANT_ID = 1L;
    private static final UUID EMPRESA_UUID = UUID.randomUUID();
    private static final Long EMPRESA_ID = 2L;
    private static final UUID ESTABLECIMIENTO_UUID = UUID.randomUUID();
    private static final Long ESTABLECIMIENTO_ID = 3L;
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final EmpresaOperadoraJpaRepository empresaRepository = mock(EmpresaOperadoraJpaRepository.class);
    private final EstablecimientoJpaRepository establecimientoRepository =
            mock(EstablecimientoJpaRepository.class);
    private final AlmacenJpaRepository almacenRepository = mock(AlmacenJpaRepository.class);
    private final TerminalPosJpaRepository terminalRepository = mock(TerminalPosJpaRepository.class);
    private final JdbcClient jdbcClient = mock(JdbcClient.class);

    private final OrganizacionJpaWriteAdapter adapter = new OrganizacionJpaWriteAdapter(
            empresaRepository, establecimientoRepository, almacenRepository, terminalRepository, jdbcClient);

    private Long resolvedTenantId = TENANT_ID;
    private Long resolvedEmpresaId = EMPRESA_ID;
    private Long resolvedEstablecimientoId = ESTABLECIMIENTO_ID;
    private Long resolvedParentEmpresaId = EMPRESA_ID;
    private String resolvedEmpresaEstado = "ACTIVO";
    private String resolvedEstablecimientoEstado = "ACTIVO";
    private boolean serieBoletaEnUso = false;
    private boolean serieFacturaEnUso = false;

    OrganizacionJpaWriteAdapterTest() {
        when(jdbcClient.sql(anyString())).thenAnswer(invocation -> statement(invocation.getArgument(0)));
    }

    private JdbcClient.StatementSpec statement(String sql) {
        return mock(JdbcClient.StatementSpec.class, invocation -> {
            var method = invocation.getMethod();
            if (method.getReturnType().equals(JdbcClient.StatementSpec.class)) return invocation.getMock();
            if (method.getName().equals("query")) return mappedQuery(lookup(sql, invocation.getArgument(0)));
            return method.getReturnType().equals(int.class) ? 1 : null;
        });
    }

    private JdbcClient.MappedQuerySpec<?> mappedQuery(Optional<?> result) {
        return mock(JdbcClient.MappedQuerySpec.class, invocation -> result);
    }

    private Optional<?> lookup(String sql, Object queryArgument) throws SQLException {
        if (sql.contains("serie_boleta_defecto = :serie")) return Optional.of(serieBoletaEnUso);
        if (sql.contains("serie_factura_defecto = :serie")) return Optional.of(serieFacturaEnUso);
        if (sql.contains("SELECT e.estado")) return Optional.of(resolvedEmpresaEstado);
        if (sql.contains("SELECT s.estado_operativo")) return Optional.of(resolvedEstablecimientoEstado);
        if (sql.contains("sch_admin.tenant WHERE")) return Optional.ofNullable(resolvedTenantId);
        if (sql.contains("empresa_operadora e")) return Optional.ofNullable(resolvedEmpresaId);
        if (resolvedEstablecimientoId == null) return Optional.empty();
        var resultSet = mock(ResultSet.class);
        when(resultSet.getLong("establecimiento_id")).thenReturn(resolvedEstablecimientoId);
        when(resultSet.getLong("empresa_id")).thenReturn(resolvedParentEmpresaId);
        return Optional.ofNullable(((RowMapper<?>) queryArgument).mapRow(resultSet, 0));
    }

    private EmpresaOperadora empresa(UUID id) {
        return EmpresaOperadora.create(
                        new EmpresaOperadoraId(id), new TenantId(TENANT_UUID), "20123456786", "Boticas SAC",
                        "Boticas", "Av. Siempre Viva 123", "150101", "014445566", "contacto@boticas.pe",
                        "https://boticas.pe", "PEN", "America/Lima", false, NOW)
                .fold(value -> value, error -> { throw new AssertionError(error.message()); });
    }

    private Establecimiento establecimiento(UUID id, String codigoDigemid) {
        return Establecimiento.create(
                        new EstablecimientoId(id), new TenantId(TENANT_UUID), new EmpresaOperadoraId(EMPRESA_UUID),
                        "EST001", "Botica Central", TipoEstablecimiento.BOTICA, null, "0001", codigoDigemid,
                        "Av. Principal 100", "150101", null, null, null, "014445566", "botica@boticas.pe",
                        true, false, false, PerfilOperacion.ONLINE, "America/Lima", NOW)
                .fold(value -> value, error -> { throw new AssertionError(error.message()); });
    }

    private Almacen almacen(UUID id) {
        return Almacen.create(
                        new AlmacenId(id), new TenantId(TENANT_UUID), new EstablecimientoId(ESTABLECIMIENTO_UUID),
                        "ALM001", "Almacén Central", TipoAlmacen.GENERAL, true, true, true, true, false,
                        null, null, NOW)
                .fold(value -> value, error -> { throw new AssertionError(error.message()); });
    }

    private TerminalPos terminal(UUID id) {
        return TerminalPos.create(
                        new TerminalPosId(id), new TenantId(TENANT_UUID), new EstablecimientoId(ESTABLECIMIENTO_UUID),
                        "POS001", "Caja 1", "B001", "F001", "SN-001", "host-1", "192.168.0.10", "IMP01", true, NOW)
                .fold(value -> value, error -> { throw new AssertionError(error.message()); });
    }

    private TerminalPos terminalSinSeries(UUID id) {
        return TerminalPos.create(
                        new TerminalPosId(id), new TenantId(TENANT_UUID), new EstablecimientoId(ESTABLECIMIENTO_UUID),
                        "POS002", "Caja 2", null, null, "SN-002", "host-2", "192.168.0.11", "IMP02", false, NOW)
                .fold(value -> value, error -> { throw new AssertionError(error.message()); });
    }

    private static <T> T unwrap(com.softprimesolutions.shared.kernel.result.Result<T, ?> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error.toString()); });
    }

    @Test
    void createsEmpresaWhenTenantExistsAndRucIsNotDuplicated() {
        var empresa = empresa(UUID.randomUUID());
        when(empresaRepository.findByUuidPublico(empresa.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(empresa)).isEqualTo(SaveEmpresaOutcome.CREATED);
        verify(empresaRepository).saveAndFlush(any(EmpresaOperadoraJpaEntity.class));
    }

    @Test
    void returnsTenantNotFoundWhenTenantDoesNotExist() {
        resolvedTenantId = null;

        assertThat(adapter.save(empresa(UUID.randomUUID()))).isEqualTo(SaveEmpresaOutcome.TENANT_NOT_FOUND);
        verify(empresaRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateRucWhenAnotherEmpresaAlreadyHasIt() {
        var empresa = empresa(UUID.randomUUID());
        when(empresaRepository.findByUuidPublico(empresa.id().value())).thenReturn(Optional.empty());
        when(empresaRepository.existsByTenantIdAndRuc(TENANT_ID, empresa.ruc())).thenReturn(true);

        assertThat(adapter.save(empresa)).isEqualTo(SaveEmpresaOutcome.DUPLICATE_RUC);
        verify(empresaRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateRucWhenInsertViolatesUniqueConstraint() {
        var empresa = empresa(UUID.randomUUID());
        when(empresaRepository.findByUuidPublico(empresa.id().value())).thenReturn(Optional.empty());
        when(empresaRepository.saveAndFlush(any(EmpresaOperadoraJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(adapter.save(empresa)).isEqualTo(SaveEmpresaOutcome.DUPLICATE_RUC);
    }

    @Test
    void updatesEmpresaWithoutUpdatedAtWhenIdAlreadyExists() {
        var empresa = empresa(UUID.randomUUID());
        when(empresaRepository.findByUuidPublico(empresa.id().value()))
                .thenReturn(Optional.of(mock(EmpresaOperadoraJpaEntity.class)));

        assertThat(adapter.save(empresa)).isEqualTo(SaveEmpresaOutcome.UPDATED);
        verify(empresaRepository, never()).saveAndFlush(any());
        verify(empresaRepository, never()).existsByTenantIdAndRuc(any(), any());
    }

    @Test
    void updatesEmpresaWithUpdatedAtWhenIdAlreadyExists() {
        var empresa = unwrap(empresa(UUID.randomUUID()).cambiarEstado(EstadoEmpresaOperadora.SUSPENDIDO, NOW));
        when(empresaRepository.findByUuidPublico(empresa.id().value()))
                .thenReturn(Optional.of(mock(EmpresaOperadoraJpaEntity.class)));

        assertThat(adapter.save(empresa)).isEqualTo(SaveEmpresaOutcome.UPDATED);
    }

    @Test
    void createsEstablecimientoWithDigemidWhenEmpresaExists() {
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.CREATED);
        verify(establecimientoRepository).saveAndFlush(any(EstablecimientoJpaEntity.class));
    }

    @Test
    void createsEstablecimientoWithoutDigemidWhenEmpresaExists() {
        var establecimiento = establecimiento(UUID.randomUUID(), null);
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.CREATED);
        verify(establecimientoRepository, never()).existsByTenantIdAndCodigoDigemid(any(), any());
    }

    @Test
    void returnsEmpresaNotFoundWhenEmpresaDoesNotBelongToTenant() {
        resolvedEmpresaId = null;

        assertThat(adapter.save(establecimiento(UUID.randomUUID(), "DIG001")))
                .isEqualTo(SaveEstablecimientoOutcome.EMPRESA_NOT_FOUND);
        verify(establecimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsEmpresaNotFoundForEstablecimientoWhenTenantDoesNotExist() {
        resolvedTenantId = null;

        assertThat(adapter.save(establecimiento(UUID.randomUUID(), "DIG001")))
                .isEqualTo(SaveEstablecimientoOutcome.EMPRESA_NOT_FOUND);
    }

    @Test
    void returnsDuplicateCodigoWhenEstablecimientoCodigoAlreadyExists() {
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.empty());
        when(establecimientoRepository.existsByTenantIdAndCodigo(TENANT_ID, establecimiento.codigo()))
                .thenReturn(true);

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.DUPLICATE_CODIGO);
        verify(establecimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateDigemidWhenCodigoDigemidAlreadyExists() {
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.empty());
        when(establecimientoRepository.existsByTenantIdAndCodigoDigemid(TENANT_ID, "DIG001")).thenReturn(true);

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.DUPLICATE_DIGEMID);
        verify(establecimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateCodigoWhenEstablecimientoInsertViolatesUniqueConstraint() {
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.empty());
        when(establecimientoRepository.saveAndFlush(any(EstablecimientoJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.DUPLICATE_CODIGO);
    }

    @Test
    void updatesEstablecimientoWhenIdAlreadyExists() {
        var establecimiento = unwrap(establecimiento(UUID.randomUUID(), "DIG001")
                .cambiarEstadoOperativo(EstadoEstablecimiento.SUSPENDIDO, NOW));
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.of(mock(EstablecimientoJpaEntity.class)));

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.UPDATED);
        verify(establecimientoRepository, never()).saveAndFlush(any());
        verify(establecimientoRepository, never()).existsByTenantIdAndCodigo(any(), any());
    }

    @Test
    void createsActiveAlmacenWhenEstablecimientoExists() {
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.CREATED);
        verify(almacenRepository).saveAndFlush(any(AlmacenJpaEntity.class));
    }

    @Test
    void createsInactiveAlmacenWhenEstablecimientoExists() {
        var almacen = unwrap(almacen(UUID.randomUUID()).desactivar(NOW));
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.CREATED);
    }

    @Test
    void returnsEstablecimientoNotFoundWhenParentDoesNotExistForAlmacen() {
        resolvedEstablecimientoId = null;

        assertThat(adapter.save(almacen(UUID.randomUUID()))).isEqualTo(SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND);
        verify(almacenRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsEstablecimientoNotFoundForAlmacenWhenTenantDoesNotExist() {
        resolvedTenantId = null;

        assertThat(adapter.save(almacen(UUID.randomUUID()))).isEqualTo(SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND);
    }

    @Test
    void returnsDuplicateCodigoWhenAlmacenCodigoAlreadyExistsInEstablecimiento() {
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());
        when(almacenRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                TENANT_ID, ESTABLECIMIENTO_ID, almacen.codigo())).thenReturn(true);

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.DUPLICATE_CODIGO);
        verify(almacenRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateCodigoWhenAlmacenInsertViolatesUniqueConstraint() {
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());
        when(almacenRepository.saveAndFlush(any(AlmacenJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.DUPLICATE_CODIGO);
    }

    @Test
    void updatesAlmacenWhenIdAlreadyExists() {
        var almacen = unwrap(almacen(UUID.randomUUID()).desactivar(NOW));
        when(almacenRepository.findByUuidPublico(almacen.id().value()))
                .thenReturn(Optional.of(mock(AlmacenJpaEntity.class)));

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.UPDATED);
        verify(almacenRepository, never()).saveAndFlush(any());
    }

    @Test
    void createsTerminalWhenEstablecimientoExists() {
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.CREATED);
        verify(terminalRepository).saveAndFlush(any(TerminalPosJpaEntity.class));
    }

    @Test
    void returnsEstablecimientoNotFoundWhenParentDoesNotExistForTerminal() {
        resolvedEstablecimientoId = null;

        assertThat(adapter.save(terminal(UUID.randomUUID()))).isEqualTo(SaveTerminalOutcome.ESTABLECIMIENTO_NOT_FOUND);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsEstablecimientoNotFoundForTerminalWhenTenantDoesNotExist() {
        resolvedTenantId = null;

        assertThat(adapter.save(terminal(UUID.randomUUID()))).isEqualTo(SaveTerminalOutcome.ESTABLECIMIENTO_NOT_FOUND);
    }

    @Test
    void returnsDuplicateCodigoWhenTerminalCodigoAlreadyExistsInEstablecimiento() {
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());
        when(terminalRepository.existsByTenantIdAndEstablecimientoIdAndCodigo(
                TENANT_ID, ESTABLECIMIENTO_ID, terminal.codigo())).thenReturn(true);

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_CODIGO);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateCodigoWhenTerminalInsertViolatesUniqueConstraint() {
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());
        when(terminalRepository.saveAndFlush(any(TerminalPosJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_CODIGO);
    }

    @Test
    void updatesTerminalWhenIdAlreadyExists() {
        var terminal = unwrap(terminal(UUID.randomUUID()).cambiarEstado(EstadoTerminalPos.BLOQUEADO, NOW));
        when(terminalRepository.findByUuidPublico(terminal.id().value()))
                .thenReturn(Optional.of(mock(TerminalPosJpaEntity.class)));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.UPDATED);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsEmpresaNoOperativaWhenEmpresaIsSuspended() {
        resolvedEmpresaEstado = "SUSPENDIDO";
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.empty());

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.EMPRESA_NO_OPERATIVA);
        verify(establecimientoRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesEstablecimientoEvenWhenEmpresaIsBlocked() {
        resolvedEmpresaEstado = "BLOQUEADO";
        var establecimiento = establecimiento(UUID.randomUUID(), "DIG001");
        when(establecimientoRepository.findByUuidPublico(establecimiento.id().value()))
                .thenReturn(Optional.of(mock(EstablecimientoJpaEntity.class)));

        assertThat(adapter.save(establecimiento)).isEqualTo(SaveEstablecimientoOutcome.UPDATED);
    }

    @Test
    void returnsEstablecimientoNoOperativoForAlmacenWhenEstablecimientoIsSuspended() {
        resolvedEstablecimientoEstado = "SUSPENDIDO";
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.ESTABLECIMIENTO_NO_OPERATIVO);
        verify(almacenRepository, never()).saveAndFlush(any());
    }

    @Test
    void createsAlmacenWhenEstablecimientoIsInRemodelacion() {
        resolvedEstablecimientoEstado = "REMODELACION";
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.CREATED);
    }

    @Test
    void updatesAlmacenEvenWhenEstablecimientoIsClosed() {
        resolvedEstablecimientoEstado = "CLAUSURADO";
        var almacen = almacen(UUID.randomUUID());
        when(almacenRepository.findByUuidPublico(almacen.id().value()))
                .thenReturn(Optional.of(mock(AlmacenJpaEntity.class)));

        assertThat(adapter.save(almacen)).isEqualTo(SaveAlmacenOutcome.UPDATED);
    }

    @Test
    void returnsEstablecimientoNoOperativoForTerminalWhenEstablecimientoIsClosed() {
        resolvedEstablecimientoEstado = "CLAUSURADO";
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatesTerminalEvenWhenEstablecimientoIsSuspended() {
        resolvedEstablecimientoEstado = "SUSPENDIDO";
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value()))
                .thenReturn(Optional.of(mock(TerminalPosJpaEntity.class)));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.UPDATED);
    }

    @Test
    void returnsDuplicateSerieBoletaWhenAnotherTerminalOfTheEmpresaUsesIt() {
        serieBoletaEnUso = true;
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateSerieFacturaWhenAnotherTerminalOfTheEmpresaUsesIt() {
        serieFacturaEnUso = true;
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA);
        verify(terminalRepository, never()).saveAndFlush(any());
    }

    @Test
    void returnsDuplicateSerieWhenUpdatingATerminalToAUsedSerie() {
        serieBoletaEnUso = true;
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value()))
                .thenReturn(Optional.of(mock(TerminalPosJpaEntity.class)));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);
    }

    @Test
    void skipsTheSerieCheckWhenTheTerminalHasNoSeries() {
        serieBoletaEnUso = true;
        serieFacturaEnUso = true;
        var terminal = terminalSinSeries(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.CREATED);
    }

    @Test
    void returnsDuplicateSerieBoletaWhenInsertViolatesTheBoletaIndex() {
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());
        when(terminalRepository.saveAndFlush(any(TerminalPosJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("violates unique constraint uk_terminal_pos_serie_boleta"));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);
    }

    @Test
    void returnsDuplicateSerieFacturaWhenInsertViolatesTheFacturaIndex() {
        var terminal = terminal(UUID.randomUUID());
        when(terminalRepository.findByUuidPublico(terminal.id().value())).thenReturn(Optional.empty());
        when(terminalRepository.saveAndFlush(any(TerminalPosJpaEntity.class)))
                .thenThrow(new DataIntegrityViolationException("violates unique constraint uk_terminal_pos_serie_factura"));

        assertThat(adapter.save(terminal)).isEqualTo(SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA);
    }

    @Test
    void tenantExistsReflectsLookupResult() {
        assertThat(adapter.tenantExists(TENANT_UUID)).isTrue();
        resolvedTenantId = null;
        assertThat(adapter.tenantExists(TENANT_UUID)).isFalse();
    }

    @Test
    void tenantExistsReturnsFalseWhenTenantIsNull() {
        assertThat(adapter.tenantExists(null)).isFalse();
    }

    @Test
    void empresaBelongsToTenantReflectsLookupResult() {
        assertThat(adapter.empresaBelongsToTenant(EMPRESA_UUID, TENANT_UUID)).isTrue();
        resolvedEmpresaId = null;
        assertThat(adapter.empresaBelongsToTenant(EMPRESA_UUID, TENANT_UUID)).isFalse();
    }

    @Test
    void empresaBelongsToTenantReturnsFalseWhenAnyIdIsNull() {
        assertThat(adapter.empresaBelongsToTenant(EMPRESA_UUID, null)).isFalse();
        assertThat(adapter.empresaBelongsToTenant(null, TENANT_UUID)).isFalse();
    }

    @Test
    void establecimientoBelongsToTenantReturnsTrueWhenTenantMatches() {
        var entity = mock(EstablecimientoJpaEntity.class);
        when(entity.getTenantId()).thenReturn(TENANT_ID);
        when(establecimientoRepository.findByUuidPublico(ESTABLECIMIENTO_UUID)).thenReturn(Optional.of(entity));

        assertThat(adapter.establecimientoBelongsToTenant(ESTABLECIMIENTO_UUID, TENANT_UUID)).isTrue();
    }

    @Test
    void establecimientoBelongsToTenantReturnsFalseWhenEstablecimientoNotFound() {
        when(establecimientoRepository.findByUuidPublico(ESTABLECIMIENTO_UUID)).thenReturn(Optional.empty());

        assertThat(adapter.establecimientoBelongsToTenant(ESTABLECIMIENTO_UUID, TENANT_UUID)).isFalse();
    }

    @Test
    void establecimientoBelongsToTenantReturnsFalseWhenTenantDoesNotMatch() {
        var entity = mock(EstablecimientoJpaEntity.class);
        when(entity.getTenantId()).thenReturn(99L);
        when(establecimientoRepository.findByUuidPublico(ESTABLECIMIENTO_UUID)).thenReturn(Optional.of(entity));

        assertThat(adapter.establecimientoBelongsToTenant(ESTABLECIMIENTO_UUID, TENANT_UUID)).isFalse();
    }
}
