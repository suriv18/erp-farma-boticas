package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EstablecimientoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoJpaRepository extends JpaRepository<EstablecimientoJpaEntity, Long> {

    Optional<EstablecimientoJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndCodigo(Long tenantId, String codigo);

    boolean existsByTenantIdAndCodigoDigemid(Long tenantId, String codigoDigemid);
}
