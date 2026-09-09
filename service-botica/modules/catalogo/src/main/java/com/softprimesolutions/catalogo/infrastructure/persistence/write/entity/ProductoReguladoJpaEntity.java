package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "producto_regulado", schema = "sch_farmacia")
public class ProductoReguladoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tipo_producto", nullable = false, length = 40)
    private String tipoProducto;

    @Column(name = "rubro_codigo", length = 50)
    private String rubroCodigo;

    @Column(name = "tipo_registro", length = 40)
    private String tipoRegistro;

    @Column(name = "numero_registro", length = 100)
    private String numeroRegistro;

    @Column(nullable = false, length = 500)
    private String denominacion;

    @Column(name = "concentracion_texto", length = 300)
    private String concentracionTexto;

    @Column(name = "presentacion_regulatoria", length = 500)
    private String presentacionRegulatoria;

    @Column(name = "forma_farmaceutica_codigo", length = 30)
    private String formaFarmaceuticaCodigo;

    @Column(name = "via_administracion_codigo", length = 30)
    private String viaAdministracionCodigo;

    @Column(name = "unidad_medida_codigo", length = 30)
    private String unidadMedidaCodigo;

    @Column(name = "condicion_venta_codigo", length = 30)
    private String condicionVentaCodigo;

    @Column(name = "clasificacion_atc", length = 30)
    private String clasificacionAtc;

    @Column(name = "clasificacion_controlada_codigo", length = 40)
    private String clasificacionControladaCodigo;

    @Column(name = "tipo_liberacion", length = 40)
    private String tipoLiberacion;

    @Column(name = "origen_fabricacion", length = 40)
    private String origenFabricacion;

    @Column(name = "pais_origen", length = 100)
    private String paisOrigen;

    @Column(name = "subpartida_nacional", length = 30)
    private String subpartidaNacional;

    @Column(name = "titular_registro", length = 300)
    private String titularRegistro;

    @Column(length = 300)
    private String fabricante;

    @Column(length = 300)
    private String importador;

    @Column(name = "establecimiento_expendio", length = 200)
    private String establecimientoExpendio;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "estado_regulatorio", nullable = false, length = 30)
    private String estadoRegulatorio;

    @Column(length = 300)
    private String fuente;

    @Column(name = "version_fuente", length = 100)
    private String versionFuente;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ProductoReguladoJpaEntity() {
    }

    public ProductoReguladoJpaEntity(
            UUID uuidPublico, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String estadoRegulatorio, String fuente, String versionFuente, Instant createdAt,
            Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tipoProducto = tipoProducto;
        this.rubroCodigo = rubroCodigo;
        this.tipoRegistro = tipoRegistro;
        this.numeroRegistro = numeroRegistro;
        this.denominacion = denominacion;
        this.concentracionTexto = concentracionTexto;
        this.presentacionRegulatoria = presentacionRegulatoria;
        this.formaFarmaceuticaCodigo = formaFarmaceuticaCodigo;
        this.viaAdministracionCodigo = viaAdministracionCodigo;
        this.unidadMedidaCodigo = unidadMedidaCodigo;
        this.condicionVentaCodigo = condicionVentaCodigo;
        this.clasificacionAtc = clasificacionAtc;
        this.clasificacionControladaCodigo = clasificacionControladaCodigo;
        this.tipoLiberacion = tipoLiberacion;
        this.origenFabricacion = origenFabricacion;
        this.paisOrigen = paisOrigen;
        this.subpartidaNacional = subpartidaNacional;
        this.titularRegistro = titularRegistro;
        this.fabricante = fabricante;
        this.importador = importador;
        this.establecimientoExpendio = establecimientoExpendio;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estadoRegulatorio = estadoRegulatorio;
        this.fuente = fuente;
        this.versionFuente = versionFuente;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public String getTipoProducto() { return tipoProducto; }
    public String getRubroCodigo() { return rubroCodigo; }
    public String getTipoRegistro() { return tipoRegistro; }
    public String getNumeroRegistro() { return numeroRegistro; }
    public String getDenominacion() { return denominacion; }
    public String getConcentracionTexto() { return concentracionTexto; }
    public String getPresentacionRegulatoria() { return presentacionRegulatoria; }
    public String getFormaFarmaceuticaCodigo() { return formaFarmaceuticaCodigo; }
    public String getViaAdministracionCodigo() { return viaAdministracionCodigo; }
    public String getUnidadMedidaCodigo() { return unidadMedidaCodigo; }
    public String getCondicionVentaCodigo() { return condicionVentaCodigo; }
    public String getClasificacionAtc() { return clasificacionAtc; }
    public String getClasificacionControladaCodigo() { return clasificacionControladaCodigo; }
    public String getTipoLiberacion() { return tipoLiberacion; }
    public String getOrigenFabricacion() { return origenFabricacion; }
    public String getPaisOrigen() { return paisOrigen; }
    public String getSubpartidaNacional() { return subpartidaNacional; }
    public String getTitularRegistro() { return titularRegistro; }
    public String getFabricante() { return fabricante; }
    public String getImportador() { return importador; }
    public String getEstablecimientoExpendio() { return establecimientoExpendio; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public String getEstadoRegulatorio() { return estadoRegulatorio; }
    public String getFuente() { return fuente; }
    public String getVersionFuente() { return versionFuente; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
