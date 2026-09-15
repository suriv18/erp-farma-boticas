package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    protected CategoriaProductoJpaEntity() {
    }

    public CategoriaProductoJpaEntity(
            UUID uuidPublico, Long tenantId, Long categoriaPadreId, String codigo, String nombre,
            String descripcion, int nivel, int orden, String estado) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.categoriaPadreId = categoriaPadreId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.orden = orden;
        this.estado = estado;
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
}
