package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.UnidadMedidaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadMedidaJpaRepository extends JpaRepository<UnidadMedidaJpaEntity, String> {
}
