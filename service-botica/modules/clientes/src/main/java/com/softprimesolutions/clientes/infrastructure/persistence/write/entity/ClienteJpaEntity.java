package com.softprimesolutions.clientes.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cliente", schema = "sch_venta")
public class ClienteJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid_publico", nullable = false, unique = true)
    private UUID uuidPublico;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "tipo_cliente", nullable = false, length = 30)
    private String tipoCliente;

    @Column(name = "tipo_documento", length = 1)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 15)
    private String numeroDocumento;

    @Column(length = 150)
    private String nombres;

    @Column(length = 180)
    private String apellidos;

    @Column(name = "razon_social", length = 300)
    private String razonSocial;

    @Column(columnDefinition = "citext")
    private String email;

    @Column(length = 40)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ClienteJpaEntity() {
    }

    public ClienteJpaEntity(
            UUID uuidPublico, Long tenantId, String tipoCliente, String tipoDocumento, String numeroDocumento,
            String nombres, String apellidos, String razonSocial, String email, String telefono,
            LocalDate fechaNacimiento, String estado, Instant createdAt) {
        this.uuidPublico = uuidPublico;
        this.tenantId = tenantId;
        this.tipoCliente = tipoCliente;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.razonSocial = razonSocial;
        this.email = email;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.estado = estado;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public UUID getUuidPublico() { return uuidPublico; }
    public Long getTenantId() { return tenantId; }
    public String getTipoCliente() { return tipoCliente; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getRazonSocial() { return razonSocial; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public String getEstado() { return estado; }
    public Instant getCreatedAt() { return createdAt; }
}
