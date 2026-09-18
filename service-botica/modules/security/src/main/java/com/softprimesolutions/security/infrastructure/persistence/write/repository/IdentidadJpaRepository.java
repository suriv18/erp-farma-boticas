package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.IdentidadJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadJpaRepository extends JpaRepository<IdentidadJpaEntity, Long> {
    Optional<IdentidadJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByTipoDocumentoAndNumeroDocumento(String tipoDocumento, String numeroDocumento);
}
