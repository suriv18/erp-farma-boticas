package com.softprimesolutions.compras.domain.model;

import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Proveedor extends AggregateRoot {

    private final UUID id;
    private final UUID tenantId;
    private final DatosProveedor datos;
    private final EstadoProveedor estado;
    private final Instant createdAt;
    private final String createdBy;
    private final Instant updatedAt;
    private final String updatedBy;

    private Proveedor(
            UUID id, UUID tenantId, DatosProveedor datos, EstadoProveedor estado, Instant createdAt,
            String createdBy, Instant updatedAt, String updatedBy) {
        this.id = Objects.requireNonNull(id, "id es obligatorio");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId es obligatorio");
        this.datos = Objects.requireNonNull(datos, "datos es obligatorio");
        this.estado = Objects.requireNonNull(estado, "estado es obligatorio");
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Proveedor crear(UUID id, UUID tenantId, DatosProveedor datos, Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        return new Proveedor(id, tenantId, datos, EstadoProveedor.ACTIVO, at, actor.codigo(), null, null);
    }

    public static Proveedor restore(
            UUID id, UUID tenantId, DatosProveedor datos, EstadoProveedor estado, Instant createdAt,
            String createdBy, Instant updatedAt, String updatedBy) {
        return new Proveedor(id, tenantId, datos, estado, createdAt, createdBy, updatedAt, updatedBy);
    }

    public Proveedor actualizar(DatosProveedor nuevosDatos, Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        return new Proveedor(id, tenantId, nuevosDatos, estado, createdAt, createdBy, at, actor.codigo());
    }

    public Proveedor cambiarEstado(EstadoProveedor nuevoEstado, Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        return new Proveedor(id, tenantId, datos, nuevoEstado, createdAt, createdBy, at, actor.codigo());
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public DatosProveedor datos() { return datos; }
    public EstadoProveedor estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public String createdBy() { return createdBy; }
    public Instant updatedAt() { return updatedAt; }
    public String updatedBy() { return updatedBy; }
}
