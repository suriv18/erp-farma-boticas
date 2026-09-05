package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.RolJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolJpaRepository extends JpaRepository<RolJpaEntity, Long> {
    Optional<RolJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);
}
