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
@Table(name = "membership", schema = "sch_seguridad")
public class MembershipJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "identidad_id", nullable = false)
    private Long identidadId;

    @Column(name = "nombre_mostrar", length = 250)
    private String nombreMostrar;

    @Column(name = "requiere_cambio_credencial", nullable = false)
    private boolean requiereCambioCredencial;

    @Column(name = "mfa_requerido", nullable = false)
    private boolean mfaRequerido;

    @Column(name = "ultimo_login_at")
    private Instant ultimoLoginAt;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected MembershipJpaEntity() {
    }

    public MembershipJpaEntity(
            UUID uuidPublico, Long tenantId, Long identidadId, String nombreMostrar,
            boolean requiereCambioCredencial, boolean mfaRequerido, String estado,
            Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.identidadId = identidadId;
        this.nombreMostrar = nombreMostrar;
        this.requiereCambioCredencial = requiereCambioCredencial;
        this.mfaRequerido = mfaRequerido;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public Long getIdentidadId() { return identidadId; }
    public String getNombreMostrar() { return nombreMostrar; }
    public boolean isRequiereCambioCredencial() { return requiereCambioCredencial; }
    public boolean isMfaRequerido() { return mfaRequerido; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
