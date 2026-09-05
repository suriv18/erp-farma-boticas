package com.softprimesolutions.security.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rol", schema = "sch_seguridad")
public class RolJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "tipo_rol", nullable = false, length = 30)
    private String tipoRol;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected RolJpaEntity() {
    }

    public RolJpaEntity(
            UUID uuidPublico,
            Long tenantId,
            String codigo,
            String nombre,
            String descripcion,
            String tipoRol,
            boolean esSistema,
            String estado,
            Instant createdAt,
            Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.tipoRol = tipoRol;
        this.esSistema = esSistema;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getTipoRol() { return tipoRol; }
    public boolean isEsSistema() { return esSistema; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
