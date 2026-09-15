package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categoria_producto", schema = "sch_catalogo")
public class CategoriaProductoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "categoria_padre_id")
    private Long categoriaPadreId;

    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int nivel;

    @Column(nullable = false)
    private int orden;

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

    protected CategoriaProductoJpaEntity() {
    }

    public CategoriaProductoJpaEntity(
            UUID uuidPublico, Long tenantId, Long categoriaPadreId, String codigo, String nombre,
            String descripcion, int nivel, int orden, String estado, String createdBy, Instant createdAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.categoriaPadreId = categoriaPadreId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.orden = orden;
        this.estado = estado;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getCategoriaPadreId() { return categoriaPadreId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getNivel() { return nivel; }
    public int getOrden() { return orden; }
    public String getEstado() { return estado; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getUpdatedBy() { return updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
}
