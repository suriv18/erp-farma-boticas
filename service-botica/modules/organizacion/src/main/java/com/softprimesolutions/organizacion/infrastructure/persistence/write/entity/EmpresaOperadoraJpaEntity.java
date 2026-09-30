package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "empresa_operadora", schema = "sch_organizacion")
public class EmpresaOperadoraJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 300)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 300)
    private String nombreComercial;

    @Column(name = "direccion_fiscal", length = 500)
    private String direccionFiscal;

    @Column(name = "ubigeo_fiscal", length = 6)
    private String ubigeoFiscal;

    @Column(length = 40)
    private String telefono;

    @Column
    private String email;

    @Column(name = "sitio_web", length = 300)
    private String sitioWeb;

    @Column(name = "moneda_funcional", nullable = false, length = 3)
    private String monedaFuncional;

    @Column(name = "zona_horaria", nullable = false, length = 80)
    private String zonaHoraria;

    @Column(name = "permite_venta_online", nullable = false)
    private boolean permiteVentaOnline;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected EmpresaOperadoraJpaEntity() {
    }

    public EmpresaOperadoraJpaEntity(
            UUID uuidPublico, Long tenantId, String ruc, String razonSocial, String nombreComercial,
            String direccionFiscal, String ubigeoFiscal, String telefono, String email, String sitioWeb,
            String monedaFuncional, String zonaHoraria, boolean permiteVentaOnline, String estado,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.direccionFiscal = direccionFiscal;
        this.ubigeoFiscal = ubigeoFiscal;
        this.telefono = telefono;
        this.email = email;
        this.sitioWeb = sitioWeb;
        this.monedaFuncional = monedaFuncional;
        this.zonaHoraria = zonaHoraria;
        this.permiteVentaOnline = permiteVentaOnline;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getRuc() { return ruc; }
    public String getRazonSocial() { return razonSocial; }
    public String getNombreComercial() { return nombreComercial; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public String getUbigeoFiscal() { return ubigeoFiscal; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getSitioWeb() { return sitioWeb; }
    public String getMonedaFuncional() { return monedaFuncional; }
    public String getZonaHoraria() { return zonaHoraria; }
    public boolean isPermiteVentaOnline() { return permiteVentaOnline; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
