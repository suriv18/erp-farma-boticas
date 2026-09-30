package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

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
@Table(name = "establecimiento_farmaceutico", schema = "sch_organizacion")
public class EstablecimientoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 250)
    private String nombre;

    @Column(name = "tipo_establecimiento", nullable = false, length = 40)
    private String tipoEstablecimiento;

    @Column(name = "categoria_regulatoria_codigo", length = 50)
    private String categoriaRegulatoriaCodigo;

    @Column(name = "codigo_anexo_sunat", nullable = false, length = 4)
    private String codigoAnexoSunat;

    @Column(name = "codigo_digemid", length = 10)
    private String codigoDigemid;

    @Column(length = 500)
    private String direccion;

    @Column(length = 6)
    private String ubigeo;

    @Column(length = 300)
    private String referencia;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(length = 40)
    private String telefono;

    @Column(columnDefinition = "citext")
    private String email;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @Column(name = "permite_venta_online", nullable = false)
    private boolean permiteVentaOnline;

    @Column(name = "permite_delivery", nullable = false)
    private boolean permiteDelivery;

    @Column(name = "perfil_operacion", nullable = false, length = 30)
    private String perfilOperacion;

    @Column(name = "zona_horaria", nullable = false, length = 80)
    private String zonaHoraria;

    @Column(name = "estado_operativo", nullable = false, length = 20)
    private String estadoOperativo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected EstablecimientoJpaEntity() {
    }

    public EstablecimientoJpaEntity(
            UUID uuidPublico, Long tenantId, Long empresaId, String codigo, String nombre,
            String tipoEstablecimiento, String categoriaRegulatoriaCodigo, String codigoAnexoSunat,
            String codigoDigemid, String direccion, String ubigeo, String referencia, BigDecimal latitud,
            BigDecimal longitud, String telefono, String email, boolean esPrincipal,
            boolean permiteVentaOnline, boolean permiteDelivery, String perfilOperacion,
            String zonaHoraria, String estadoOperativo, Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.empresaId = empresaId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoEstablecimiento = tipoEstablecimiento;
        this.categoriaRegulatoriaCodigo = categoriaRegulatoriaCodigo;
        this.codigoAnexoSunat = codigoAnexoSunat;
        this.codigoDigemid = codigoDigemid;
        this.direccion = direccion;
        this.ubigeo = ubigeo;
        this.referencia = referencia;
        this.latitud = latitud;
        this.longitud = longitud;
        this.telefono = telefono;
        this.email = email;
        this.esPrincipal = esPrincipal;
        this.permiteVentaOnline = permiteVentaOnline;
        this.permiteDelivery = permiteDelivery;
        this.perfilOperacion = perfilOperacion;
        this.zonaHoraria = zonaHoraria;
        this.estadoOperativo = estadoOperativo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getEmpresaId() { return empresaId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getTipoEstablecimiento() { return tipoEstablecimiento; }
    public String getCategoriaRegulatoriaCodigo() { return categoriaRegulatoriaCodigo; }
    public String getCodigoAnexoSunat() { return codigoAnexoSunat; }
    public String getCodigoDigemid() { return codigoDigemid; }
    public String getDireccion() { return direccion; }
    public String getUbigeo() { return ubigeo; }
    public String getReferencia() { return referencia; }
    public BigDecimal getLatitud() { return latitud; }
    public BigDecimal getLongitud() { return longitud; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public boolean isEsPrincipal() { return esPrincipal; }
    public boolean isPermiteVentaOnline() { return permiteVentaOnline; }
    public boolean isPermiteDelivery() { return permiteDelivery; }
    public String getPerfilOperacion() { return perfilOperacion; }
    public String getZonaHoraria() { return zonaHoraria; }
    public String getEstadoOperativo() { return estadoOperativo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
