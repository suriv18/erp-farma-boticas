package com.softprimesolutions.clientes.infrastructure.persistence.write.repository;

import com.softprimesolutions.clientes.infrastructure.persistence.write.entity.ClienteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteJpaEntity, Long> {

    boolean existsByTenantIdAndTipoDocumentoAndNumeroDocumento(
            Long tenantId, String tipoDocumento, String numeroDocumento);
}
