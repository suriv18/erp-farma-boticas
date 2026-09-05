package com.softprimesolutions.security.infrastructure.persistence.write.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "identidad_externa", schema = "sch_seguridad")
public class IdentidadExternaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(length = 500)
    private String issuer;

    @Column(name = "email_claim", length = 254)
    private String emailClaim;

    @Column(name = "ultimo_login_at")
    private Instant ultimoLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentidadExternaJpaEntity() {
    }

    public IdentidadExternaJpaEntity(
            Long usuarioId, String provider, String subject, String issuer,
            String emailClaim, Instant createdAt) {
        this.usuarioId = usuarioId;
        this.provider = provider;
        this.subject = subject;
        this.issuer = issuer;
        this.emailClaim = emailClaim;
        this.createdAt = createdAt;
    }
}
