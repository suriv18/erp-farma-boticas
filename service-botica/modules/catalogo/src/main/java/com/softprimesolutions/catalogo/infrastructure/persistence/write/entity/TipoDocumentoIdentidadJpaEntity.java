package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_documento_identidad", schema = "sch_catalogo")
public class TipoDocumentoIdentidadJpaEntity {

    @Id
    @Column(length = 2)
    private String codigo;

    @Column(nullable = false, length = 30)
    private String sigla;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(name = "max")
    private Short max;

    @Column(name = "min")
    private Short min;

    @Column(nullable = false, length = 20)
    private String estado;

    protected TipoDocumentoIdentidadJpaEntity() {
    }

    public TipoDocumentoIdentidadJpaEntity(
            String codigo, String sigla, String denominacion, Short max, Short min, String estado) {
        this.codigo = codigo;
        this.sigla = sigla;
        this.denominacion = denominacion;
        this.max = max;
        this.min = min;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getSigla() { return sigla; }
    public String getDenominacion() { return denominacion; }
    public Short getMax() { return max; }
    public Short getMin() { return min; }
    public String getEstado() { return estado; }
}
