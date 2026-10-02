package com.softprimesolutions.inventario.domain.model;

import com.softprimesolutions.inventario.domain.exception.InventarioErrorCodes;
import com.softprimesolutions.inventario.domain.valueobject.Actor;
import com.softprimesolutions.inventario.domain.valueobject.LoteId;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Lote extends AggregateRoot {

    private static final int NUMERO_LOTE_MAX = 120;
    private static final int MOTIVO_MAX = 1000;

    private final LoteId id;
    private final TenantId tenantId;
    private final UUID skuId;
    private final String numeroLote;
    private final LocalDate fechaVencimiento;
    private final EstadoLote estado;
    private final String motivoEstado;
    private final Instant bloqueadoAt;
    private final String bloqueadoPor;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final String updatedBy;

    private Lote(
            LoteId id, TenantId tenantId, UUID skuId, String numeroLote, LocalDate fechaVencimiento,
            EstadoLote estado, String motivoEstado, Instant bloqueadoAt, String bloqueadoPor,
            Instant createdAt, Instant updatedAt, String updatedBy) {
        this.id = Objects.requireNonNull(id, "id es obligatorio");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId es obligatorio");
        this.skuId = Objects.requireNonNull(skuId, "skuId es obligatorio");
        this.numeroLote = numeroLote;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = Objects.requireNonNull(estado, "estado es obligatorio");
        this.motivoEstado = motivoEstado;
        this.bloqueadoAt = bloqueadoAt;
        this.bloqueadoPor = bloqueadoPor;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public static Result<Lote, ErrorDetail> create(
            LoteId id, TenantId tenantId, UUID skuId, String numeroLote, LocalDate fechaVencimiento,
            LocalDate hoy, Instant createdAt) {
        Objects.requireNonNull(hoy, "hoy es obligatorio");
        Objects.requireNonNull(createdAt, "createdAt es obligatorio");
        var numero = normalize(numeroLote);
        if (numero == null || numero.length() > NUMERO_LOTE_MAX) {
            return failure(InventarioErrorCodes.LOTE_INVALIDO,
                    "El número de lote debe tener entre 1 y 120 caracteres.");
        }
        if (fechaVencimiento == null) {
            return failure(InventarioErrorCodes.LOTE_INVALIDO, "La fecha de vencimiento del lote es obligatoria.");
        }
        if (fechaVencimiento.isBefore(hoy)) {
            return failure(InventarioErrorCodes.LOTE_VENCIDO, "No se puede ingresar un lote vencido.");
        }
        return Result.success(new Lote(
                id, tenantId, skuId, numero, fechaVencimiento, EstadoLote.HABILITADO, null, null, null,
                createdAt, null, null));
    }

    public static Lote restore(
            LoteId id, TenantId tenantId, UUID skuId, String numeroLote, LocalDate fechaVencimiento,
            EstadoLote estado, String motivoEstado, Instant bloqueadoAt, String bloqueadoPor,
            Instant createdAt, Instant updatedAt, String updatedBy) {
        return new Lote(
                id, tenantId, skuId, numeroLote, fechaVencimiento, estado, motivoEstado, bloqueadoAt,
                bloqueadoPor, createdAt, updatedAt, updatedBy);
    }

    public Result<Lote, ErrorDetail> bloquear(String motivo, Actor actor, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        if (!estado.admiteBloqueo()) {
            return failure(InventarioErrorCodes.LOTE_ESTADO_INVALIDO,
                    "Solo un lote habilitado o en cuarentena puede bloquearse.");
        }
        var razon = normalize(motivo);
        if (razon == null || razon.length() > MOTIVO_MAX) {
            return failure(InventarioErrorCodes.LOTE_MOTIVO_INVALIDO,
                    "El motivo del bloqueo debe tener entre 1 y 1000 caracteres.");
        }
        return Result.success(new Lote(
                id, tenantId, skuId, numeroLote, fechaVencimiento, EstadoLote.BLOQUEADO, razon, at,
                actor.codigo(), createdAt, at, actor.codigo()));
    }

    public Result<Lote, ErrorDetail> desbloquear(Actor actor, LocalDate hoy, Instant at) {
        Objects.requireNonNull(actor, "actor es obligatorio");
        Objects.requireNonNull(hoy, "hoy es obligatorio");
        Objects.requireNonNull(at, "at es obligatorio");
        if (estado != EstadoLote.BLOQUEADO) {
            return failure(InventarioErrorCodes.LOTE_ESTADO_INVALIDO, "Solo un lote bloqueado puede desbloquearse.");
        }
        if (fechaVencimiento.isBefore(hoy)) {
            return failure(InventarioErrorCodes.LOTE_NO_HABILITABLE, "Un lote vencido no puede habilitarse.");
        }
        return Result.success(new Lote(
                id, tenantId, skuId, numeroLote, fechaVencimiento, EstadoLote.HABILITADO, null, null, null,
                createdAt, at, actor.codigo()));
    }

    public boolean vendible(LocalDate hoy) {
        return estado.vendible() && !fechaVencimiento.isBefore(hoy);
    }

    private static <T> Result<T, ErrorDetail> failure(String code, String message) {
        return Result.failure(new ErrorDetail(code, message, Map.of()));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public LoteId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public UUID skuId() { return skuId; }
    public String numeroLote() { return numeroLote; }
    public LocalDate fechaVencimiento() { return fechaVencimiento; }
    public EstadoLote estado() { return estado; }
    public String motivoEstado() { return motivoEstado; }
    public Instant bloqueadoAt() { return bloqueadoAt; }
    public String bloqueadoPor() { return bloqueadoPor; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public String updatedBy() { return updatedBy; }
}
