package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ViaAdministracionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViaAdministracionJpaRepository extends JpaRepository<ViaAdministracionJpaEntity, String> {
}
