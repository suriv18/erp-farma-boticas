package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EmpresaOperadoraJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaOperadoraJpaRepository extends JpaRepository<EmpresaOperadoraJpaEntity, Long> {

    Optional<EmpresaOperadoraJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndRuc(Long tenantId, String ruc);
}
