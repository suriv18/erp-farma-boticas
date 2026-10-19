package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "principio_activo", schema = "sch_catalogo")
public class PrincipioActivoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "codigo_fuente", length = 80)
    private String codigoFuente;

    @Column(nullable = false, length = 300)
    private String denominacion;

    @Column(name = "nombre_normalizado", length = 300)
    private String nombreNormalizado;

    @Column(length = 300)
    private String fuente;

    @Column(nullable = false, length = 20)
    private String estado;

    protected PrincipioActivoJpaEntity() {
    }

    public PrincipioActivoJpaEntity(
            UUID uuidPublico, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente, String estado) {
        this.uuidPublico = uuidPublico;
        this.codigoFuente = codigoFuente;
        this.denominacion = denominacion;
        this.nombreNormalizado = nombreNormalizado;
        this.fuente = fuente;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public String getCodigoFuente() { return codigoFuente; }
    public String getDenominacion() { return denominacion; }
    public String getNombreNormalizado() { return nombreNormalizado; }
    public String getFuente() { return fuente; }
    public String getEstado() { return estado; }
}
