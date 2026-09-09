package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "condicion_venta", schema = "sch_farmacia")
public class CondicionVentaJpaEntity {

    @Id
    @Column(length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String denominacion;

    @Column(name = "requiere_receta", nullable = false)
    private boolean requiereReceta;

    @Column(name = "requiere_retencion", nullable = false)
    private boolean requiereRetencion;

    @Column(length = 300)
    private String fuente;

    @Column(name = "version_fuente", length = 100)
    private String versionFuente;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false, length = 20)
    private String estado;

    protected CondicionVentaJpaEntity() {
    }

    public CondicionVentaJpaEntity(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.requiereReceta = requiereReceta;
        this.requiereRetencion = requiereRetencion;
        this.fuente = fuente;
        this.versionFuente = versionFuente;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estado = estado;
    }

    public String getCodigo() { return codigo; }
    public String getDenominacion() { return denominacion; }
    public boolean isRequiereReceta() { return requiereReceta; }
    public boolean isRequiereRetencion() { return requiereRetencion; }
    public String getFuente() { return fuente; }
    public String getVersionFuente() { return versionFuente; }
    public LocalDate getVigenteDesde() { return vigenteDesde; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public String getEstado() { return estado; }
}
