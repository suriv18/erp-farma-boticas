package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.FormaFarmaceuticaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormaFarmaceuticaJpaRepository extends JpaRepository<FormaFarmaceuticaJpaEntity, String> {
}
