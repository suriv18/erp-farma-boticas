package com.softprimesolutions.security.infrastructure.persistence.write.repository;

import com.softprimesolutions.security.infrastructure.persistence.write.entity.IdentidadExternaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentidadExternaJpaRepository extends JpaRepository<IdentidadExternaJpaEntity, Long> {
    boolean existsByProviderAndSubject(String provider, String subject);
}
