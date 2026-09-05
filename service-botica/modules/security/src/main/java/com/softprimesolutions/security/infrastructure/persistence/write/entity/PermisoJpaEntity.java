package com.softprimesolutions.security.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "permiso", schema = "sch_seguridad")
public class PermisoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modulo_id", nullable = false)
    private Long moduloId;

    @Column(nullable = false, unique = true, length = 150)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String recurso;

    @Column(nullable = false, length = 50)
    private String accion;

    @Column(nullable = false, length = 180)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "es_critico", nullable = false)
    private boolean esCritico;

    @Column(nullable = false, length = 20)
    private String estado;

    protected PermisoJpaEntity() {
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
}
