package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.PrincipioActivoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrincipioActivoJpaRepository extends JpaRepository<PrincipioActivoJpaEntity, Long> {
    Optional<PrincipioActivoJpaEntity> findByUuidPublico(UUID uuidPublico);
}
