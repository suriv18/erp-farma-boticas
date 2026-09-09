package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoReguladoJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoReguladoJpaRepository extends JpaRepository<ProductoReguladoJpaEntity, Long> {
    Optional<ProductoReguladoJpaEntity> findByUuidPublico(UUID uuidPublico);
}
