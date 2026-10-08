package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.CondicionVentaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CondicionVentaJpaRepository extends JpaRepository<CondicionVentaJpaEntity, String> {
}
