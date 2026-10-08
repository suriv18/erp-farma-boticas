package com.softprimesolutions.organizacion.infrastructure.persistence.write.repository;

import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.TerminalPosJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalPosJpaRepository extends JpaRepository<TerminalPosJpaEntity, Long> {

    Optional<TerminalPosJpaEntity> findByUuidPublico(UUID uuidPublico);

    boolean existsByTenantIdAndEstablecimientoIdAndCodigo(Long tenantId, Long establecimientoId, String codigo);
}
