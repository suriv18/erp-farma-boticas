package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.RubroComercialJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RubroComercialJpaRepository extends JpaRepository<RubroComercialJpaEntity, Long> {
    Optional<RubroComercialJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);
}
