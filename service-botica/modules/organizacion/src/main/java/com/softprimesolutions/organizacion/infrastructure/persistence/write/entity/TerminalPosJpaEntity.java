package com.softprimesolutions.organizacion.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "terminal_pos", schema = "sch_organizacion")
public class TerminalPosJpaEntity {

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

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "serie_boleta_defecto", length = 4)
    private String serieBoletaDefecto;

    @Column(name = "serie_factura_defecto", length = 4)
    private String serieFacturaDefecto;

    @Column(name = "numero_serie_equipo", length = 120)
    private String numeroSerieEquipo;

    @Column(length = 150)
    private String hostname;

    @ColumnTransformer(read = "host(ip_equipo)", write = "CAST(? AS inet)")
    @Column(name = "ip_equipo", columnDefinition = "inet")
    private String ipEquipo;

    @Column(name = "impresora_codigo", length = 100)
    private String impresoraCodigo;

    @Column(name = "store_edge_habilitado", nullable = false)
    private boolean storeEdgeHabilitado;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected TerminalPosJpaEntity() {
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getEmpresaId() { return empresaId; }
    public Long getEstablecimientoId() { return establecimientoId; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getSerieBoletaDefecto() { return serieBoletaDefecto; }
    public String getSerieFacturaDefecto() { return serieFacturaDefecto; }
    public String getNumeroSerieEquipo() { return numeroSerieEquipo; }
    public String getHostname() { return hostname; }
    public String getIpEquipo() { return ipEquipo; }
    public String getImpresoraCodigo() { return impresoraCodigo; }
    public boolean isStoreEdgeHabilitado() { return storeEdgeHabilitado; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
