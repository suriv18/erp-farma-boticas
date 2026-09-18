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

    @Column(name = "identidad_id", nullable = false)
    private Long identidadId;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(length = 500)
    private String issuer;

    @Column(name = "email_claim", columnDefinition = "citext")
    private String emailClaim;

    @Column(name = "ultimo_login_at")
    private Instant ultimoLoginAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentidadExternaJpaEntity() {
    }

    public IdentidadExternaJpaEntity(
            Long identidadId, String provider, String subject, String issuer,
            String emailClaim, Instant createdAt) {
        this.identidadId = identidadId;
        this.provider = provider;
        this.subject = subject;
        this.issuer = issuer;
        this.emailClaim = emailClaim;
        this.createdAt = createdAt;
    }
}
