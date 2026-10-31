package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "forma_farmaceutica", schema = "sch_catalogo")
public class FormaFarmaceuticaJpaEntity {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(length = 300)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    protected FormaFarmaceuticaJpaEntity() {
    }

    public FormaFarmaceuticaJpaEntity(String codigo, String denominacion, String fuente, String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.fuente = fuente;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public String getFuente() { return fuente; }
    public String getEstado() { return estado; }
}
