package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.PermisoJpaEntity;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermisoJpaRepository extends JpaRepository<PermisoJpaEntity, Long> {
    long countByCodigoIn(Collection<String> codes);
}
