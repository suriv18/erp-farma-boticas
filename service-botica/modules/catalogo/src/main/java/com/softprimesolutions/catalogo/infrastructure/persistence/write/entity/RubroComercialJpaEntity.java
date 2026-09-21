package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rubro_comercial", schema = "sch_catalogo")
public class RubroComercialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "es_farmaceutico", nullable = false)
    private boolean esFarmaceutico;

    @Column(nullable = false)
    private int orden;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "es_activo", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String esActivo;

    @Column(name = "created_by", nullable = false, length = 15)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by", length = 15)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected RubroComercialJpaEntity() {
    }

    public RubroComercialJpaEntity(
            UUID uuidPublico, Long tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden, String esActivo, String createdBy, Instant createdAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.esFarmaceutico = esFarmaceutico;
        this.orden = orden;
        this.esActivo = esActivo;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public boolean isEsFarmaceutico() { return esFarmaceutico; }
    public int getOrden() { return orden; }
    public String getEsActivo() { return esActivo; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
