package com.softprimesolutions.organizacion.infrastructure.persistence.read.adapter;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.dto.result.NodoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.mapper.OrganizacionReadMapper;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.repository.OrganizacionJdbcReadRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OrganizacionJdbcReadAdapter implements OrganizacionReadPort {

    private final OrganizacionJdbcReadRepository repository;

    public OrganizacionJdbcReadAdapter(OrganizacionJdbcReadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<EmpresaOperadoraResult> findEmpresas(UUID tenantId, String search, int page, int size) {
        var items = repository.findEmpresas(tenantId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countEmpresas(tenantId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmpresaOperadoraResult> findEmpresaById(UUID tenantId, UUID empresaId) {
        return repository.findEmpresaById(tenantId, empresaId).map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<EstablecimientoResult> findEstablecimientos(
            UUID tenantId, UUID empresaId, String search, int page, int size) {
        var items = repository.findEstablecimientos(tenantId, empresaId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(
                items, page, size, repository.countEstablecimientos(tenantId, empresaId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstablecimientoResult> findEstablecimientoById(UUID tenantId, UUID establecimientoId) {
        return repository.findEstablecimientoById(tenantId, establecimientoId)
                .map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<AlmacenResult> findAlmacenes(
            UUID tenantId, UUID establecimientoId, String search, int page, int size) {
        var items = repository.findAlmacenes(tenantId, establecimientoId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countAlmacenes(tenantId, establecimientoId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlmacenResult> findAlmacenById(UUID tenantId, UUID almacenId) {
        return repository.findAlmacenById(tenantId, almacenId).map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<TerminalPosResult> findTerminales(
            UUID tenantId, UUID establecimientoId, String search, int page, int size) {
        var items = repository.findTerminales(tenantId, establecimientoId, search, page * size, size).stream()
                .map(OrganizacionReadMapper::toResult)
                .toList();
        return new PaginaResult<>(items, page, size, repository.countTerminales(tenantId, establecimientoId, search));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TerminalPosResult> findTerminalById(UUID tenantId, UUID terminalId) {
        return repository.findTerminalById(tenantId, terminalId).map(OrganizacionReadMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public EstructuraCorporativaResult findEstructuraCorporativa(UUID tenantId) {
        var almacenes = groupedBy(
                repository.findAllAlmacenes(tenantId), AlmacenProjection::establecimientoUuid,
                almacen -> new NodoResult(
                        almacen.uuidPublico(), almacen.codigo(), almacen.nombre(),
                        almacen.activo() ? "ACTIVE" : "INACTIVE"));
        var terminales = groupedBy(
                repository.findAllTerminalesActivos(tenantId), TerminalProjection::establecimientoUuid,
                terminal -> new NodoResult(
                        terminal.uuidPublico(), terminal.codigo(), terminal.nombre(), terminal.estado()));
        var establecimientos = groupedBy(
                repository.findAllEstablecimientosActivos(tenantId), EstablecimientoProjection::empresaUuid,
                establecimiento -> new EstablecimientoNodoResult(
                        establecimiento.uuidPublico(), establecimiento.codigo(), establecimiento.nombre(),
                        establecimiento.estadoOperativo(), establecimiento.zonaHoraria(),
                        almacenes.getOrDefault(establecimiento.uuidPublico(), List.of()),
                        terminales.getOrDefault(establecimiento.uuidPublico(), List.of())));

        var companies = repository.findAllEmpresasActivas(tenantId).stream()
                .map(empresa -> new EmpresaNodoResult(
                        empresa.uuidPublico(), empresa.razonSocial(), empresa.nombreComercial(), empresa.estado(),
                        establecimientos.getOrDefault(empresa.uuidPublico(), List.of())))
                .toList();
        return new EstructuraCorporativaResult(Instant.now(), companies);
    }

    private static <P, N> Map<UUID, List<N>> groupedBy(
            List<P> projections, Function<P, UUID> parent, Function<P, N> node) {
        return projections.stream().collect(groupingBy(parent, mapping(node, toList())));
    }
}
