package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "sku_codigo_barra", schema = "sch_catalogo")
public class SkuCodigoBarraJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "sku_id", nullable = false)
    private Long skuId;

    @Column(name = "tipo_codigo", nullable = false, length = 30)
    private String tipoCodigo;

    @Column(name = "codigo_barra", nullable = false, length = 80)
    private String codigoBarra;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_by", nullable = false, length = 15)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 15)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected SkuCodigoBarraJpaEntity() {
    }

    public SkuCodigoBarraJpaEntity(
            Long tenantId, Long skuId, String tipoCodigo, String codigoBarra, boolean esPrincipal,
            LocalDate vigenteDesde, LocalDate vigenteHasta, String estado, String createdBy, Instant createdAt) {
        this.tenantId = tenantId;
        this.skuId = skuId;
        this.tipoCodigo = tipoCodigo;
        this.codigoBarra = codigoBarra;
        this.esPrincipal = esPrincipal;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estado = estado;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getSkuId() { return skuId; }
    public String getTipoCodigo() { return tipoCodigo; }
    public String getCodigoBarra() { return codigoBarra; }
    public boolean isEsPrincipal() { return esPrincipal; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public String getEstado() { return estado; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
