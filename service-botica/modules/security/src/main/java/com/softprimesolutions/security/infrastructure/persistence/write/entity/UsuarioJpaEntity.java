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
@Table(name = "usuario", schema = "sch_seguridad")
public class UsuarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 30)
    private String numeroDocumento;

    @Column(length = 150)
    private String nombres;

    @Column(length = 180)
    private String apellidos;

    @Column(length = 150)
    private String username;

    @Column(length = 254)
    private String email;

    @Column(length = 40)
    private String telefono;

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

    protected UsuarioJpaEntity() {
    }

    public UsuarioJpaEntity(
            UUID uuidPublico, Long tenantId, String tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, String username, String email, String telefono,
            String nombreMostrar, boolean requiereCambioCredencial, boolean mfaRequerido,
            String estado, Instant createdAt, Instant updatedAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.username = username;
        this.email = email;
        this.telefono = telefono;
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
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getNombreMostrar() { return nombreMostrar; }
    public boolean isRequiereCambioCredencial() { return requiereCambioCredencial; }
    public boolean isMfaRequerido() { return mfaRequerido; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
