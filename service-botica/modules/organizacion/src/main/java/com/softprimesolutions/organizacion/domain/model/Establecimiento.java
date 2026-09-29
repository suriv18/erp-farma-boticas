package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

/** Sede física (botica) donde opera una empresa operadora. */
public final class Establecimiento extends AggregateRoot {

    private static final Pattern ANEXO_SUNAT_PATTERN = Pattern.compile("[0-9]{4}");
    private static final Pattern UBIGEO_PATTERN = Pattern.compile("[0-9]{6}");

    private final EstablecimientoId id;
    private final TenantId tenantId;
    private final EmpresaOperadoraId empresaId;
    private final String codigo;
    private final String nombre;
    private final TipoEstablecimiento tipoEstablecimiento;
    private final String categoriaRegulatoriaCodigo;
    private final String codigoAnexoSunat;
    private final String codigoDigemid;
    private final String direccion;
    private final String ubigeo;
    private final String referencia;
    private final BigDecimal latitud;
    private final BigDecimal longitud;
    private final String telefono;
    private final String email;
    private final boolean esPrincipal;
    private final boolean permiteVentaOnline;
    private final boolean permiteDelivery;
    private final PerfilOperacion perfilOperacion;
    private final String zonaHoraria;
    private final EstadoEstablecimiento estadoOperativo;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Establecimiento(
            EstablecimientoId id, TenantId tenantId, EmpresaOperadoraId empresaId, String codigo,
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, EstadoEstablecimiento estadoOperativo,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.empresaId = empresaId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipoEstablecimiento = tipoEstablecimiento;
        this.categoriaRegulatoriaCodigo = categoriaRegulatoriaCodigo;
        this.codigoAnexoSunat = codigoAnexoSunat;
        this.codigoDigemid = codigoDigemid;
        this.direccion = direccion;
        this.ubigeo = ubigeo;
        this.referencia = referencia;
        this.latitud = latitud;
        this.longitud = longitud;
        this.telefono = telefono;
        this.email = email;
        this.esPrincipal = esPrincipal;
        this.permiteVentaOnline = permiteVentaOnline;
        this.permiteDelivery = permiteDelivery;
        this.perfilOperacion = perfilOperacion;
        this.zonaHoraria = zonaHoraria;
        this.estadoOperativo = estadoOperativo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Establecimiento, ErrorDetail> create(
            EstablecimientoId id, TenantId tenantId, EmpresaOperadoraId empresaId, String codigo,
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del establecimiento es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (empresaId == null) return invalid("empresaId", "La empresa operadora es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");
        if (tipoEstablecimiento == null) {
            return invalid("tipoEstablecimiento", "El tipo de establecimiento es obligatorio.");
        }
        if (perfilOperacion == null) {
            return invalid("perfilOperacion", "El perfil de operación es obligatorio.");
        }

        var normalizedCodigo = normalize(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > 40) {
            return invalid("codigo", "El código debe tener entre 1 y 40 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        var normalizedAnexo = normalize(codigoAnexoSunat);
        if (normalizedAnexo == null || !ANEXO_SUNAT_PATTERN.matcher(normalizedAnexo).matches()) {
            return invalid("codigoAnexoSunat", "El código de anexo SUNAT debe tener 4 dígitos.");
        }

        var normalizedDigemid = normalize(codigoDigemid);
        if (normalizedDigemid != null && normalizedDigemid.length() > 10) {
            return invalid("codigoDigemid", "El código DIGEMID no debe exceder 10 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeo);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeo", "El ubigeo debe tener 6 dígitos.");
        }

        return Result.success(new Establecimiento(
                id, tenantId, empresaId, normalizedCodigo, normalizedNombre, tipoEstablecimiento,
                normalize(categoriaRegulatoriaCodigo), normalizedAnexo, normalizedDigemid,
                normalizeSpaces(direccion), normalizedUbigeo, normalizeSpaces(referencia), latitud,
                longitud, normalize(telefono), normalize(email), esPrincipal, permiteVentaOnline,
                permiteDelivery, perfilOperacion, normalize(zonaHoraria), EstadoEstablecimiento.ACTIVO,
                createdAt, null));
    }

    public static Establecimiento restore(
            EstablecimientoId id, TenantId tenantId, EmpresaOperadoraId empresaId, String codigo,
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, EstadoEstablecimiento estadoOperativo,
            Instant createdAt, Instant updatedAt) {
        return new Establecimiento(
                id, tenantId, empresaId, codigo, nombre, tipoEstablecimiento, categoriaRegulatoriaCodigo,
                codigoAnexoSunat, codigoDigemid, direccion, ubigeo, referencia, latitud, longitud,
                telefono, email, esPrincipal, permiteVentaOnline, permiteDelivery, perfilOperacion,
                zonaHoraria, estadoOperativo, createdAt, updatedAt);
    }

    public Result<Establecimiento, ErrorDetail> updateDetails(
            String nombre, TipoEstablecimiento tipoEstablecimiento, String categoriaRegulatoriaCodigo,
            String codigoAnexoSunat, String codigoDigemid, String direccion, String ubigeo,
            String referencia, BigDecimal latitud, BigDecimal longitud, String telefono, String email,
            boolean esPrincipal, boolean permiteVentaOnline, boolean permiteDelivery,
            PerfilOperacion perfilOperacion, String zonaHoraria, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        if (tipoEstablecimiento == null) {
            return invalid("tipoEstablecimiento", "El tipo de establecimiento es obligatorio.");
        }
        if (perfilOperacion == null) {
            return invalid("perfilOperacion", "El perfil de operación es obligatorio.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        var normalizedAnexo = normalize(codigoAnexoSunat);
        if (normalizedAnexo == null || !ANEXO_SUNAT_PATTERN.matcher(normalizedAnexo).matches()) {
            return invalid("codigoAnexoSunat", "El código de anexo SUNAT debe tener 4 dígitos.");
        }

        var normalizedDigemid = normalize(codigoDigemid);
        if (normalizedDigemid != null && normalizedDigemid.length() > 10) {
            return invalid("codigoDigemid", "El código DIGEMID no debe exceder 10 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeo);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeo", "El ubigeo debe tener 6 dígitos.");
        }

        return Result.success(new Establecimiento(
                id, tenantId, empresaId, codigo, normalizedNombre, tipoEstablecimiento,
                normalize(categoriaRegulatoriaCodigo), normalizedAnexo, normalizedDigemid,
                normalizeSpaces(direccion), normalizedUbigeo, normalizeSpaces(referencia), latitud,
                longitud, normalize(telefono), normalize(email), esPrincipal, permiteVentaOnline,
                permiteDelivery, perfilOperacion, normalize(zonaHoraria), estadoOperativo,
                createdAt, updatedAt));
    }

    public Result<Establecimiento, ErrorDetail> cambiarEstadoOperativo(
            EstadoEstablecimiento nuevoEstado, Instant updatedAt) {
        if (nuevoEstado == null) return invalid("estadoOperativo", "El nuevo estado es obligatorio.");
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new Establecimiento(
                id, tenantId, empresaId, codigo, nombre, tipoEstablecimiento, categoriaRegulatoriaCodigo,
                codigoAnexoSunat, codigoDigemid, direccion, ubigeo, referencia, latitud, longitud,
                telefono, email, esPrincipal, permiteVentaOnline, permiteDelivery, perfilOperacion,
                zonaHoraria, nuevoEstado, createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_ESTABLECIMIENTO_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public EstablecimientoId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public EmpresaOperadoraId empresaId() { return empresaId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public TipoEstablecimiento tipoEstablecimiento() { return tipoEstablecimiento; }
    public String categoriaRegulatoriaCodigo() { return categoriaRegulatoriaCodigo; }
    public String codigoAnexoSunat() { return codigoAnexoSunat; }
    public String codigoDigemid() { return codigoDigemid; }
    public String direccion() { return direccion; }
    public String ubigeo() { return ubigeo; }
    public String referencia() { return referencia; }
    public BigDecimal latitud() { return latitud; }
    public BigDecimal longitud() { return longitud; }
    public String telefono() { return telefono; }
    public String email() { return email; }
    public boolean esPrincipal() { return esPrincipal; }
    public boolean permiteVentaOnline() { return permiteVentaOnline; }
    public boolean permiteDelivery() { return permiteDelivery; }
    public PerfilOperacion perfilOperacion() { return perfilOperacion; }
    public String zonaHoraria() { return zonaHoraria; }
    public EstadoEstablecimiento estadoOperativo() { return estadoOperativo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
