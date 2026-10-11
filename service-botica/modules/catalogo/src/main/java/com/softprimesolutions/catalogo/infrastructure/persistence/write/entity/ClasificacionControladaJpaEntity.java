package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clasificacion_controlada", schema = "sch_catalogo")
public class ClasificacionControladaJpaEntity {

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(name = "norma_fuente", length = 300)
    private String normaFuente;

    @Column(name = "requiere_receta_especial", nullable = false)
    private boolean requiereRecetaEspecial;

    @Column(name = "retiene_receta", nullable = false)
    private boolean retieneReceta;

    @Column(name = "vigencia_receta_dias")
    private Integer vigenciaRecetaDias;

    @Column(nullable = false, length = 20)
    private String estado;

    protected ClasificacionControladaJpaEntity() {
    }

    public ClasificacionControladaJpaEntity(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias, String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.normaFuente = normaFuente;
        this.requiereRecetaEspecial = requiereRecetaEspecial;
        this.retieneReceta = retieneReceta;
        this.vigenciaRecetaDias = vigenciaRecetaDias;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public String getNormaFuente() { return normaFuente; }
    public boolean isRequiereRecetaEspecial() { return requiereRecetaEspecial; }
    public boolean isRetieneReceta() { return retieneReceta; }
    public Integer getVigenciaRecetaDias() { return vigenciaRecetaDias; }
    public String getEstado() { return estado; }
}
