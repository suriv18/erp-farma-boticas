package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sku_comercial", schema = "sch_catalogo")
public class SkuComercialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "producto_regulado_id")
    private Long productoReguladoId;

    @Column(name = "categoria_id")
    private Long categoriaId;

    @Column(name = "marca_id")
    private Long marcaId;

    @Column(name = "tipo_sku", nullable = false, length = 30)
    private String tipoSku;

    @Column(name = "codigo_interno", nullable = false, length = 60)
    private String codigoInterno;

    @Column(name = "descripcion_comercial", nullable = false, length = 500)
    private String descripcionComercial;

    @Column(name = "nombre_corto", length = 200)
    private String nombreCorto;

    @Column(name = "presentacion_comercial", length = 300)
    private String presentacionComercial;

    @Column(name = "unidad_venta_codigo", length = 30)
    private String unidadVentaCodigo;

    private BigDecimal contenido;

    @Column(name = "unidad_contenido_codigo", length = 30)
    private String unidadContenidoCodigo;

    @Column(name = "peso_gramos")
    private BigDecimal pesoGramos;

    @Column(name = "alto_cm")
    private BigDecimal altoCm;

    @Column(name = "ancho_cm")
    private BigDecimal anchoCm;

    @Column(name = "largo_cm")
    private BigDecimal largoCm;

    @Column(name = "permite_venta_fraccion", nullable = false)
    private boolean permiteVentaFraccion;

    @Column(name = "factor_fraccion")
    private BigDecimal factorFraccion;

    @Column(name = "unidad_fraccion_codigo", length = 30)
    private String unidadFraccionCodigo;

    @Column(name = "requiere_lote", nullable = false)
    private boolean requiereLote;

    @Column(name = "requiere_vencimiento", nullable = false)
    private boolean requiereVencimiento;

    @Column(name = "afecto_igv", nullable = false)
    private boolean afectoIgv;

    @Column(name = "stock_minimo_default", nullable = false)
    private BigDecimal stockMinimoDefault;

    @Column(name = "stock_maximo_default")
    private BigDecimal stockMaximoDefault;

    @Column(name = "imagen_uri")
    private String imagenUri;

    @Column(name = "estado_comercial", nullable = false, length = 20)
    private String estadoComercial;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected SkuComercialJpaEntity() {
    }

    public SkuComercialJpaEntity(
            UUID uuidPublico, Long tenantId, Long productoReguladoId, Long categoriaId, Long marcaId,
            String tipoSku, String codigoInterno, String descripcionComercial, String nombreCorto,
            String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, String unidadFraccionCodigo,
            boolean requiereLote, boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, String estadoComercial, String createdBy,
            Instant createdAt, String updatedBy, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.productoReguladoId = productoReguladoId;
        this.categoriaId = categoriaId;
        this.marcaId = marcaId;
        this.tipoSku = tipoSku;
        this.codigoInterno = codigoInterno;
        this.descripcionComercial = descripcionComercial;
        this.nombreCorto = nombreCorto;
        this.presentacionComercial = presentacionComercial;
        this.unidadVentaCodigo = unidadVentaCodigo;
        this.contenido = contenido;
        this.unidadContenidoCodigo = unidadContenidoCodigo;
        this.pesoGramos = pesoGramos;
        this.altoCm = altoCm;
        this.anchoCm = anchoCm;
        this.largoCm = largoCm;
        this.permiteVentaFraccion = permiteVentaFraccion;
        this.factorFraccion = factorFraccion;
        this.unidadFraccionCodigo = unidadFraccionCodigo;
        this.requiereLote = requiereLote;
        this.requiereVencimiento = requiereVencimiento;
        this.afectoIgv = afectoIgv;
        this.stockMinimoDefault = stockMinimoDefault;
        this.stockMaximoDefault = stockMaximoDefault;
        this.imagenUri = imagenUri;
        this.estadoComercial = estadoComercial;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getProductoReguladoId() { return productoReguladoId; }
    public Long getCategoriaId() { return categoriaId; }
    public Long getMarcaId() { return marcaId; }
    public String getTipoSku() { return tipoSku; }
    public String getCodigoInterno() { return codigoInterno; }
    public String getDescripcionComercial() { return descripcionComercial; }
    public String getNombreCorto() { return nombreCorto; }
    public String getPresentacionComercial() { return presentacionComercial; }
    public String getUnidadVentaCodigo() { return unidadVentaCodigo; }
    public BigDecimal getContenido() { return contenido; }
    public String getUnidadContenidoCodigo() { return unidadContenidoCodigo; }
    public BigDecimal getPesoGramos() { return pesoGramos; }
    public BigDecimal getAltoCm() { return altoCm; }
    public BigDecimal getAnchoCm() { return anchoCm; }
    public BigDecimal getLargoCm() { return largoCm; }
    public boolean isPermiteVentaFraccion() { return permiteVentaFraccion; }
    public BigDecimal getFactorFraccion() { return factorFraccion; }
    public String getUnidadFraccionCodigo() { return unidadFraccionCodigo; }
    public boolean isRequiereLote() { return requiereLote; }
    public boolean isRequiereVencimiento() { return requiereVencimiento; }
    public boolean isAfectoIgv() { return afectoIgv; }
    public BigDecimal getStockMinimoDefault() { return stockMinimoDefault; }
    public BigDecimal getStockMaximoDefault() { return stockMaximoDefault; }
    public String getImagenUri() { return imagenUri; }
    public String getEstadoComercial() { return estadoComercial; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
