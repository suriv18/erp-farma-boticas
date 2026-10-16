package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ClasificacionControladaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClasificacionControladaJpaRepository extends JpaRepository<ClasificacionControladaJpaEntity, String> {
}
