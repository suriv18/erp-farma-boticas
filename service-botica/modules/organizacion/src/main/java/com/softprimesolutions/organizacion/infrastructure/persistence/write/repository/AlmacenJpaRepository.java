package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.AlmacenJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlmacenJpaRepository extends JpaRepository<AlmacenJpaEntity, Long> {

    Optional<AlmacenJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndEstablecimientoIdAndCodigo(Long tenantId, Long establecimientoId, String codigo);
}
