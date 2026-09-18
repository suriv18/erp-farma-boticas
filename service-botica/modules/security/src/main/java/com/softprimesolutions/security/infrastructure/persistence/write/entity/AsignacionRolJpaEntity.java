package com.softprimesolutions.security.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario_rol_ambito", schema = "sch_seguridad")
public class AsignacionRolJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "membership_id", nullable = false)
    private Long usuarioId;

    @Column(name = "rol_id", nullable = false)
    private Long rolId;

    @Column(name = "tipo_ambito", nullable = false, length = 30)
    private String tipoAmbito;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(name = "establecimiento_id")
    private Long establecimientoId;

    @Column(name = "almacen_id")
    private Long almacenId;

    @Column(name = "terminal_id")
    private Long terminalId;

    @Column(name = "vigente_desde", nullable = false)
    private Instant vigenteDesde;

    @Column(name = "vigente_hasta")
    private Instant vigenteHasta;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AsignacionRolJpaEntity() {
    }

    public AsignacionRolJpaEntity(
            UUID uuidPublico, Long tenantId, Long usuarioId, Long rolId, String tipoAmbito,
            Long empresaId, Long establecimientoId, Long almacenId, Long terminalId,
            Instant vigenteDesde, Instant vigenteHasta, String estado, String createdBy, Instant createdAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.usuarioId = usuarioId;
        this.rolId = rolId;
        this.tipoAmbito = tipoAmbito;
        this.empresaId = empresaId;
        this.establecimientoId = establecimientoId;
        this.almacenId = almacenId;
        this.terminalId = terminalId;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estado = estado;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
