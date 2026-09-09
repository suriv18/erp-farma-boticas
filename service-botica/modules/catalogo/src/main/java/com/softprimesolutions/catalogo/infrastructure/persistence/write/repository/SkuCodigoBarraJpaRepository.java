package com.softprimesolutions.catalogo.infrastructure.persistence.write.repository;

import com.softprimesolutions.catalogo.infrastructure.persistence.write.entity.SkuCodigoBarraJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkuCodigoBarraJpaRepository extends JpaRepository<SkuCodigoBarraJpaEntity, Long> {
    List<SkuCodigoBarraJpaEntity> findBySkuId(Long skuId);
    boolean existsByTenantIdAndCodigoBarra(Long tenantId, String codigoBarra);
    void deleteBySkuId(Long skuId);
}
