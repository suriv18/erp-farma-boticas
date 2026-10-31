package com.softprimesolutions.catalogo.infrastructure.persistence.write.adapter;

import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.mapper.CatalogoComercialWriteMapper;
import com.softprimesolutions.catalogo.infrastructure.persistence.write.repository.RubroComercialJpaRepository;
import jakarta.persistence.EntityManager;
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
public class RubroComercialJpaWriteAdapter implements RubroComercialPort {

    private final RubroComercialJpaRepository repository;
    private final JdbcClient jdbcClient;
    private final EntityManager entityManager;

    public RubroComercialJpaWriteAdapter(
            RubroComercialJpaRepository repository, JdbcClient jdbcClient, EntityManager entityManager) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public SaveOutcome save(RubroComercial rubroComercial) {
        var tenantId = findTenantId(
                rubroComercial.tenantId() == null ? null : rubroComercial.tenantId().value());
        if (tenantId.isEmpty()) return SaveOutcome.TENANT_NOT_FOUND;

        var existing = repository.findByUuidPublico(rubroComercial.id().value());
        if (existing.isEmpty()) {
            if (repository.existsByTenantIdAndCodigo(tenantId.get(), rubroComercial.codigo())) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
            try {
                repository.saveAndFlush(CatalogoComercialWriteMapper.toEntity(rubroComercial, tenantId.get()));
                return SaveOutcome.CREATED;
            } catch (DataIntegrityViolationException exception) {
                return SaveOutcome.DUPLICATE_CODIGO;
            }
        }

        var entity = existing.get();
        if (!entity.getCodigo().equals(rubroComercial.codigo())
                && repository.existsByTenantIdAndCodigo(tenantId.get(), rubroComercial.codigo())) {
            return SaveOutcome.DUPLICATE_CODIGO;
        }
        jdbcClient.sql("""
                        UPDATE sch_catalogo.rubro_comercial
                           SET codigo = :codigo, nombre = :nombre, descripcion = :descripcion,
                               es_farmaceutico = :esFarmaceutico, orden = :orden,
                               updated_by = :updatedBy, updated_at = :updatedAt
                         WHERE uuid_publico = :rubroComercialId
                        """)
                .param("codigo", rubroComercial.codigo())
                .param("nombre", rubroComercial.nombre())
                .param("descripcion", rubroComercial.descripcion())
                .param("esFarmaceutico", rubroComercial.esFarmaceutico())
                .param("orden", rubroComercial.orden())
                .param("updatedBy", "SYSTEM")
                .param("updatedAt", toOffsetDateTime(Instant.now()))
                .param("rubroComercialId", rubroComercial.id().value())
                .update();
        return SaveOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return Optional.empty();
        var entity = repository.findByUuidPublico(rubroComercialId);
        if (entity.isEmpty() || !entity.get().getTenantId().equals(tenantInternalId.get())) {
            return Optional.empty();
        }
        var rubro = RubroComercial.restore(
                new RubroComercialId(entity.get().getUuidPublico()), new TenantId(tenantId),
                entity.get().getCodigo(), entity.get().getNombre(), entity.get().getDescripcion(),
                entity.get().isEsFarmaceutico(), entity.get().getOrden(),
                CatalogoComercialWriteMapper.toEstadoRubroComercial(entity.get().getEsActivo()));
        return Optional.of(rubro);
    }

    @Override
    @Transactional
    public boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt) {
        var tenantInternalId = findTenantId(tenantId);
        if (tenantInternalId.isEmpty()) return false;
        var esActivo = "ACTIVO".equals(status) ? "1" : "0";
        var updated = jdbcClient.sql("""
                        UPDATE sch_catalogo.rubro_comercial SET es_activo = :esActivo
                         WHERE tenant_id = :tenantId AND uuid_publico = :rubroComercialId
                        """)
                .param("esActivo", esActivo).param("tenantId", tenantInternalId.get())
                .param("rubroComercialId", rubroComercialId)
                .update() == 1;
        if (updated) entityManager.clear();
        return updated;
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid).query(Long.class).optional();
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
