package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.ProductoPrincipioActivoJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoPrincipioActivoJpaRepository
        extends JpaRepository<ProductoPrincipioActivoJpaEntity, ProductoPrincipioActivoJpaEntity.Key> {
    List<ProductoPrincipioActivoJpaEntity> findByProductoReguladoId(Long productoReguladoId);
    void deleteByProductoReguladoId(Long productoReguladoId);
}
