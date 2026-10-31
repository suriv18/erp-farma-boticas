package com.softprimesolutions.catalogo.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "producto_principio_activo", schema = "sch_catalogo")
@IdClass(ProductoPrincipioActivoJpaEntity.Key.class)
public class ProductoPrincipioActivoJpaEntity {

    @Id
    @Column(name = "producto_regulado_id")
    private Long productoReguladoId;

    @Id
    @Column(name = "principio_activo_id")
    private Long principioActivoId;

    @Column(name = "concentracion_texto", length = 200)
    private String concentracionTexto;

    private BigDecimal cantidad;

    @Column(name = "unidad_medida_codigo", length = 30)
    private String unidadMedidaCodigo;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    private short orden;

    protected ProductoPrincipioActivoJpaEntity() {
    }

    public ProductoPrincipioActivoJpaEntity(
            Long productoReguladoId, Long principioActivoId, String concentracionTexto, BigDecimal cantidad,
            String unidadMedidaCodigo, boolean esPrincipal, short orden) {
        this.productoReguladoId = productoReguladoId;
        this.principioActivoId = principioActivoId;
        this.concentracionTexto = concentracionTexto;
        this.cantidad = cantidad;
        this.unidadMedidaCodigo = unidadMedidaCodigo;
        this.esPrincipal = esPrincipal;
        this.orden = orden;
    }

    public Long getProductoReguladoId() { return productoReguladoId; }
    public Long getPrincipioActivoId() { return principioActivoId; }
    public String getConcentracionTexto() { return concentracionTexto; }
    public BigDecimal getCantidad() { return cantidad; }
    public String getUnidadMedidaCodigo() { return unidadMedidaCodigo; }
    public boolean isEsPrincipal() { return esPrincipal; }
    public short getOrden() { return orden; }

    public static final class Key implements Serializable {
        private Long productoReguladoId;
        private Long principioActivoId;

        public Key() {
        }

        public Key(Long productoReguladoId, Long principioActivoId) {
            this.productoReguladoId = productoReguladoId;
            this.principioActivoId = principioActivoId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return Objects.equals(productoReguladoId, key.productoReguladoId)
                    && Objects.equals(principioActivoId, key.principioActivoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productoReguladoId, principioActivoId);
        }
    }
}
