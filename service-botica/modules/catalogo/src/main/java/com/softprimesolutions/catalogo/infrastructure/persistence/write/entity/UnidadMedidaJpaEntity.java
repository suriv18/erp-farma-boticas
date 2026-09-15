package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "unidad_medida", schema = "sch_catalogo")
public class UnidadMedidaJpaEntity {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String denominacion;

    @Column(length = 30)
    private String simbolo;

    @Column(name = "permite_decimal", nullable = false)
    private boolean permiteDecimal;

    @Column(length = 300)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    protected UnidadMedidaJpaEntity() {
    }

    public UnidadMedidaJpaEntity(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente,
            String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.simbolo = simbolo;
        this.permiteDecimal = permiteDecimal;
        this.fuente = fuente;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public String getSimbolo() { return simbolo; }
    public boolean isPermiteDecimal() { return permiteDecimal; }
    public String getFuente() { return fuente; }
    public String getEstado() { return estado; }
}
