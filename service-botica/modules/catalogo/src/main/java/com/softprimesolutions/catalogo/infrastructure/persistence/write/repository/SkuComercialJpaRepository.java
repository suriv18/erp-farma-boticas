package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuComercialJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuComercialJpaRepository extends JpaRepository<SkuComercialJpaEntity, Long> {
    Optional<SkuComercialJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndCodigoInterno(Long tenantId, String codigoInterno);
}
