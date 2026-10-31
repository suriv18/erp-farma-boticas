package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.MembershipJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipJpaRepository extends JpaRepository<MembershipJpaEntity, Long> {
    Optional<MembershipJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndIdentidadId(Long tenantId, Long identidadId);
}
