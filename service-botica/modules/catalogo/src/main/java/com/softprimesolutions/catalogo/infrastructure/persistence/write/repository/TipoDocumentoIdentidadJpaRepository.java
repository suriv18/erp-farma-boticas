package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.TipoDocumentoIdentidadJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoDocumentoIdentidadJpaRepository extends JpaRepository<TipoDocumentoIdentidadJpaEntity, String> {
}
