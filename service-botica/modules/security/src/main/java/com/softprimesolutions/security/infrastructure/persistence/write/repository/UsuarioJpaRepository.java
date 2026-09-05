package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.UsuarioJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {
    Optional<UsuarioJpaEntity> findByUuidPublico(UUID uuidPublico);
    boolean existsByTenantIdAndUsername(Long tenantId, String username);
    boolean existsByTenantIdAndEmail(Long tenantId, String email);
    boolean existsByTenantIdAndTipoDocumentoAndNumeroDocumento(
            Long tenantId, String tipoDocumento, String numeroDocumento);
}
