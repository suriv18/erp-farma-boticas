package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.EmpresaOperadoraId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Pattern;

/** Empresa legal/corporativa que opera una o más boticas. */
public final class EmpresaOperadora extends AggregateRoot {

    private static final Pattern RUC_PATTERN = Pattern.compile("(10|20)[0-9]{9}");
    private static final Pattern UBIGEO_PATTERN = Pattern.compile("[0-9]{6}");
    private static final int[] RUC_WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private final EmpresaOperadoraId id;
    private final TenantId tenantId;
    private final String ruc;
    private final String razonSocial;
    private final String nombreComercial;
    private final String direccionFiscal;
    private final String ubigeoFiscal;
    private final String telefono;
    private final String email;
    private final String sitioWeb;
    private final String monedaFuncional;
    private final String zonaHoraria;
    private final boolean permiteVentaOnline;
    private final EstadoEmpresaOperadora estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private EmpresaOperadora(
            EmpresaOperadoraId id, TenantId tenantId, String ruc, String razonSocial,
            String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono,
            String email, String sitioWeb, String monedaFuncional, String zonaHoraria,
            boolean permiteVentaOnline, EstadoEmpresaOperadora estado, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.direccionFiscal = direccionFiscal;
        this.ubigeoFiscal = ubigeoFiscal;
        this.telefono = telefono;
        this.email = email;
        this.sitioWeb = sitioWeb;
        this.monedaFuncional = monedaFuncional;
        this.zonaHoraria = zonaHoraria;
        this.permiteVentaOnline = permiteVentaOnline;
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<EmpresaOperadora, ErrorDetail> create(
            EmpresaOperadoraId id, TenantId tenantId, String ruc, String razonSocial,
            String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono,
            String email, String sitioWeb, String monedaFuncional, String zonaHoraria,
            boolean permiteVentaOnline, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de empresa es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedRuc = normalize(ruc);
        if (normalizedRuc == null || !RUC_PATTERN.matcher(normalizedRuc).matches()) {
            return invalid("ruc", "El RUC debe tener 11 dígitos e iniciar con 10 o 20.");
        }
        if (!hasValidCheckDigit(normalizedRuc)) {
            return invalid("ruc", "El RUC no es válido: el dígito verificador no coincide.");
        }

        var normalizedRazonSocial = normalizeSpaces(razonSocial);
        if (normalizedRazonSocial == null || normalizedRazonSocial.length() < 2
                || normalizedRazonSocial.length() > 300) {
            return invalid("razonSocial", "La razón social debe tener entre 2 y 300 caracteres.");
        }

        var normalizedNombreComercial = normalizeSpaces(nombreComercial);
        if (normalizedNombreComercial != null && normalizedNombreComercial.length() > 300) {
            return invalid("nombreComercial", "El nombre comercial no debe exceder 300 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeoFiscal);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeoFiscal", "El ubigeo fiscal debe tener 6 dígitos.");
        }

        var normalizedMoneda = normalize(monedaFuncional);
        if (normalizedMoneda == null || normalizedMoneda.length() != 3) {
            return invalid("monedaFuncional", "La moneda funcional debe tener 3 caracteres (ISO 4217).");
        }

        var normalizedZonaHoraria = normalize(zonaHoraria);
        if (normalizedZonaHoraria == null) {
            return invalid("zonaHoraria", "La zona horaria es obligatoria.");
        }

        return Result.success(new EmpresaOperadora(
                id, tenantId, normalizedRuc, normalizedRazonSocial, normalizedNombreComercial,
                normalizeSpaces(direccionFiscal), normalizedUbigeo, normalize(telefono),
                normalize(email), normalize(sitioWeb), normalizedMoneda, normalizedZonaHoraria,
                permiteVentaOnline, EstadoEmpresaOperadora.ACTIVO, createdAt, null));
    }

    public static EmpresaOperadora restore(
            EmpresaOperadoraId id, TenantId tenantId, String ruc, String razonSocial,
            String nombreComercial, String direccionFiscal, String ubigeoFiscal, String telefono,
            String email, String sitioWeb, String monedaFuncional, String zonaHoraria,
            boolean permiteVentaOnline, EstadoEmpresaOperadora estado, Instant createdAt, Instant updatedAt) {
        return new EmpresaOperadora(
                id, tenantId, ruc, razonSocial, nombreComercial, direccionFiscal, ubigeoFiscal,
                telefono, email, sitioWeb, monedaFuncional, zonaHoraria, permiteVentaOnline,
                estado, createdAt, updatedAt);
    }

    public Result<EmpresaOperadora, ErrorDetail> updateDetails(
            String razonSocial, String nombreComercial, String direccionFiscal, String ubigeoFiscal,
            String telefono, String email, String sitioWeb, String monedaFuncional,
            String zonaHoraria, boolean permiteVentaOnline, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");

        var normalizedRazonSocial = normalizeSpaces(razonSocial);
        if (normalizedRazonSocial == null || normalizedRazonSocial.length() < 2
                || normalizedRazonSocial.length() > 300) {
            return invalid("razonSocial", "La razón social debe tener entre 2 y 300 caracteres.");
        }

        var normalizedNombreComercial = normalizeSpaces(nombreComercial);
        if (normalizedNombreComercial != null && normalizedNombreComercial.length() > 300) {
            return invalid("nombreComercial", "El nombre comercial no debe exceder 300 caracteres.");
        }

        var normalizedUbigeo = normalize(ubigeoFiscal);
        if (normalizedUbigeo != null && !UBIGEO_PATTERN.matcher(normalizedUbigeo).matches()) {
            return invalid("ubigeoFiscal", "El ubigeo fiscal debe tener 6 dígitos.");
        }

        var normalizedMoneda = normalize(monedaFuncional);
        if (normalizedMoneda == null || normalizedMoneda.length() != 3) {
            return invalid("monedaFuncional", "La moneda funcional debe tener 3 caracteres (ISO 4217).");
        }

        var normalizedZonaHoraria = normalize(zonaHoraria);
        if (normalizedZonaHoraria == null) {
            return invalid("zonaHoraria", "La zona horaria es obligatoria.");
        }

        return Result.success(new EmpresaOperadora(
                id, tenantId, ruc, normalizedRazonSocial, normalizedNombreComercial,
                normalizeSpaces(direccionFiscal), normalizedUbigeo, normalize(telefono),
                normalize(email), normalize(sitioWeb), normalizedMoneda, normalizedZonaHoraria,
                permiteVentaOnline, estado, createdAt, updatedAt));
    }

    public Result<EmpresaOperadora, ErrorDetail> cambiarEstado(
            EstadoEmpresaOperadora nuevoEstado, Instant updatedAt) {
        if (nuevoEstado == null) return invalid("estado", "El nuevo estado es obligatorio.");
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new EmpresaOperadora(
                id, tenantId, ruc, razonSocial, nombreComercial, direccionFiscal, ubigeoFiscal,
                telefono, email, sitioWeb, monedaFuncional, zonaHoraria, permiteVentaOnline,
                nuevoEstado, createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("ORG_EMPRESA_INVALIDA", message, Map.of("field", field)));
    }

    private static boolean hasValidCheckDigit(String ruc) {
        var sum = 0;
        for (var index = 0; index < RUC_WEIGHTS.length; index++) {
            sum += Character.digit(ruc.charAt(index), 10) * RUC_WEIGHTS[index];
        }
        return (11 - sum % 11) % 10 == Character.digit(ruc.charAt(10), 10);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public EmpresaOperadoraId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String ruc() { return ruc; }
    public String razonSocial() { return razonSocial; }
    public String nombreComercial() { return nombreComercial; }
    public String direccionFiscal() { return direccionFiscal; }
    public String ubigeoFiscal() { return ubigeoFiscal; }
    public String telefono() { return telefono; }
    public String email() { return email; }
    public String sitioWeb() { return sitioWeb; }
    public String monedaFuncional() { return monedaFuncional; }
    public String zonaHoraria() { return zonaHoraria; }
    public boolean permiteVentaOnline() { return permiteVentaOnline; }
    public EstadoEmpresaOperadora estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
