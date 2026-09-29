package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.organizacion.domain.valueobject.TerminalPosId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;

/** Terminal de punto de venta (caja) asociado a un establecimiento. */
public final class TerminalPos extends AggregateRoot {

    private final TerminalPosId id;
    private final TenantId tenantId;
    private final EstablecimientoId establecimientoId;
    private final String codigo;
    private final String nombre;
    private final String serieBoletaDefecto;
    private final String serieFacturaDefecto;
    private final String numeroSerieEquipo;
    private final String hostname;
    private final String ipEquipo;
    private final String impresoraCodigo;
    private final boolean storeEdgeHabilitado;
    private final EstadoTerminalPos estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private TerminalPos(
            TerminalPosId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, EstadoTerminalPos estado, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.serieBoletaDefecto = serieBoletaDefecto;
        this.serieFacturaDefecto = serieFacturaDefecto;
        this.numeroSerieEquipo = numeroSerieEquipo;
        this.hostname = hostname;
        this.ipEquipo = ipEquipo;
        this.impresoraCodigo = impresoraCodigo;
        this.storeEdgeHabilitado = storeEdgeHabilitado;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<TerminalPos, ErrorDetail> create(
            TerminalPosId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del terminal es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (establecimientoId == null) return invalid("establecimientoId", "El establecimiento es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedCodigo = normalize(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > 40) {
            return invalid("codigo", "El código debe tener entre 1 y 40 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        return Result.success(new TerminalPos(
                id, tenantId, establecimientoId, normalizedCodigo, normalizedNombre,
                normalize(serieBoletaDefecto), normalize(serieFacturaDefecto), normalize(numeroSerieEquipo),
                normalize(hostname), normalize(ipEquipo), normalize(impresoraCodigo), storeEdgeHabilitado,
                EstadoTerminalPos.ACTIVO, createdAt, null));
    }

    public static TerminalPos restore(
            TerminalPosId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, EstadoTerminalPos estado, Instant createdAt, Instant updatedAt) {
        return new TerminalPos(
                id, tenantId, establecimientoId, codigo, nombre, serieBoletaDefecto, serieFacturaDefecto,
                numeroSerieEquipo, hostname, ipEquipo, impresoraCodigo, storeEdgeHabilitado, estado,
                createdAt, updatedAt);
    }

    public Result<TerminalPos, ErrorDetail> updateDetails(
            String nombre, String serieBoletaDefecto, String serieFacturaDefecto,
            String numeroSerieEquipo, String hostname, String ipEquipo, String impresoraCodigo,
            boolean storeEdgeHabilitado, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        return Result.success(new TerminalPos(
                id, tenantId, establecimientoId, codigo, normalizedNombre, normalize(serieBoletaDefecto),
                normalize(serieFacturaDefecto), normalize(numeroSerieEquipo), normalize(hostname),
                normalize(ipEquipo), normalize(impresoraCodigo), storeEdgeHabilitado, estado,
                createdAt, updatedAt));
    }

    public Result<TerminalPos, ErrorDetail> cambiarEstado(EstadoTerminalPos nuevoEstado, Instant updatedAt) {
        if (nuevoEstado == null) return invalid("estado", "El nuevo estado es obligatorio.");
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new TerminalPos(
                id, tenantId, establecimientoId, codigo, nombre, serieBoletaDefecto, serieFacturaDefecto,
                numeroSerieEquipo, hostname, ipEquipo, impresoraCodigo, storeEdgeHabilitado, nuevoEstado,
                createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_TERMINAL_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public TerminalPosId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public EstablecimientoId establecimientoId() { return establecimientoId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String serieBoletaDefecto() { return serieBoletaDefecto; }
    public String serieFacturaDefecto() { return serieFacturaDefecto; }
    public String numeroSerieEquipo() { return numeroSerieEquipo; }
    public String hostname() { return hostname; }
    public String ipEquipo() { return ipEquipo; }
    public String impresoraCodigo() { return impresoraCodigo; }
    public boolean storeEdgeHabilitado() { return storeEdgeHabilitado; }
    public EstadoTerminalPos estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
