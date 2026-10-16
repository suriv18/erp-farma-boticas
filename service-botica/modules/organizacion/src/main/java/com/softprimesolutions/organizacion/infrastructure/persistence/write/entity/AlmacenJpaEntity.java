package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "almacen", schema = "sch_organizacion")
public class AlmacenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "establecimiento_id", nullable = false)
    private Long establecimientoId;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(name = "permite_lotes", nullable = false)
    private boolean permiteLotes;

    @Column(name = "permite_vencimiento", nullable = false)
    private boolean permiteVencimiento;

    @Column(name = "permite_venta", nullable = false)
    private boolean permiteVenta;

    @Column(name = "permite_despacho", nullable = false)
    private boolean permiteDespacho;

    @Column(name = "control_temperatura", nullable = false)
    private boolean controlTemperatura;

    @Column(name = "temperatura_min_c", precision = 6, scale = 2)
    private BigDecimal temperaturaMinC;

    @Column(name = "temperatura_max_c", precision = 6, scale = 2)
    private BigDecimal temperaturaMaxC;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "es_activo", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String esActivo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected AlmacenJpaEntity() {
    }

    public AlmacenJpaEntity(
            UUID uuidPublico, Long tenantId, Long empresaId, Long establecimientoId, String codigo,
            String nombre, String tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, String esActivo,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.empresaId = empresaId;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.permiteLotes = permiteLotes;
        this.permiteVencimiento = permiteVencimiento;
        this.permiteVenta = permiteVenta;
        this.permiteDespacho = permiteDespacho;
        this.controlTemperatura = controlTemperatura;
        this.temperaturaMinC = temperaturaMinC;
        this.temperaturaMaxC = temperaturaMaxC;
        this.esActivo = esActivo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getEmpresaId() { return empresaId; }
    public Long getEstablecimientoId() { return establecimientoId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public boolean isPermiteLotes() { return permiteLotes; }
    public boolean isPermiteVencimiento() { return permiteVencimiento; }
    public boolean isPermiteVenta() { return permiteVenta; }
    public boolean isPermiteDespacho() { return permiteDespacho; }
    public boolean isControlTemperatura() { return controlTemperatura; }
    public BigDecimal getTemperaturaMinC() { return temperaturaMinC; }
    public BigDecimal getTemperaturaMaxC() { return temperaturaMaxC; }
    public String getEsActivo() { return esActivo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
