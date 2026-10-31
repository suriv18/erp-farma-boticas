package com.softprimesolutions.clientes.domain.model;

import com.softprimesolutions.clientes.domain.valueobject.ClienteId;
import com.softprimesolutions.clientes.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Pattern;

/** Cliente retail. No equivale a paciente (ADR-012). */
public final class Cliente extends AggregateRoot {

    private static final String DNI = "1";
    private static final String RUC = "6";
    private static final Pattern DNI_PATTERN = Pattern.compile("[0-9]{8}");
    private static final Pattern RUC_PATTERN = Pattern.compile("[0-9]{11}");
    private static final Pattern DOCUMENTO_PATTERN = Pattern.compile("[0-9A-Za-z-]{1,15}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final ClienteId id;
    private final TenantId tenantId;
    private final TipoCliente tipoCliente;
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final Detalles detalles;
    private final EstadoCliente estado;
    private final String motivoBloqueo;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Cliente(
            ClienteId id, TenantId tenantId, TipoCliente tipoCliente, String tipoDocumento,
            String numeroDocumento, Detalles detalles, EstadoCliente estado, String motivoBloqueo,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.tipoCliente = tipoCliente;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.detalles = detalles;
        this.estado = estado;
        this.motivoBloqueo = motivoBloqueo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    private record Detalles(
            String nombres, String apellidos, String razonSocial, String email, String telefono,
            LocalDate fechaNacimiento) {
    }

    public static Result<Cliente, ErrorDetail> create(
            ClienteId id, TenantId tenantId, TipoCliente tipoCliente, String tipoDocumento,
            String numeroDocumento, String nombres, String apellidos, String razonSocial, String email,
            String telefono, LocalDate fechaNacimiento, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del cliente es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (tipoCliente == null) return invalid("tipoCliente", "El tipo de cliente es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var tipo = normalize(tipoDocumento);
        var numero = normalize(numeroDocumento);
        if ((tipo == null) != (numero == null)) {
            return invalid("numeroDocumento", "El tipo y el número de documento se indican juntos.");
        }
        if (tipoCliente == TipoCliente.SIN_IDENTIFICAR && tipo != null) {
            return invalid("tipoDocumento", "Un cliente sin identificar no lleva documento.");
        }
        if (tipoCliente != TipoCliente.SIN_IDENTIFICAR && tipo == null) {
            return invalid("tipoDocumento", "El tipo y el número de documento son obligatorios.");
        }
        if (numero != null && !documentoValido(tipo, numero)) {
            return invalid("numeroDocumento", mensajeDocumento(tipo));
        }
        return validateDetalles(tipoCliente, nombres, apellidos, razonSocial, email, telefono,
                fechaNacimiento, createdAt)
                .map(detalles -> new Cliente(id, tenantId, tipoCliente, tipo, numero, detalles,
                        EstadoCliente.ACTIVO, null, createdAt, null));
    }

    public static Cliente restore(
            ClienteId id, TenantId tenantId, TipoCliente tipoCliente, String tipoDocumento,
            String numeroDocumento, String nombres, String apellidos, String razonSocial, String email,
            String telefono, LocalDate fechaNacimiento, EstadoCliente estado, String motivoBloqueo,
            Instant createdAt, Instant updatedAt) {
        return new Cliente(
                id, tenantId, tipoCliente, tipoDocumento, numeroDocumento,
                new Detalles(nombres, apellidos, razonSocial, email, telefono, fechaNacimiento),
                estado, motivoBloqueo, createdAt, updatedAt);
    }

    public Result<Cliente, ErrorDetail> updateDetails(
            String nombres, String apellidos, String razonSocial, String email, String telefono,
            LocalDate fechaNacimiento, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return validateDetalles(tipoCliente, nombres, apellidos, razonSocial, email, telefono,
                fechaNacimiento, updatedAt)
                .map(nuevos -> new Cliente(id, tenantId, tipoCliente, tipoDocumento, numeroDocumento, nuevos,
                        estado, motivoBloqueo, createdAt, updatedAt));
    }

    public Result<Cliente, ErrorDetail> cambiarEstado(EstadoCliente nuevoEstado, String motivo, Instant updatedAt) {
        if (nuevoEstado == null) return invalid("estado", "El nuevo estado es obligatorio.");
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        var motivoNormalizado = nuevoEstado == EstadoCliente.BLOQUEADO ? normalizeSpaces(motivo) : null;
        if (nuevoEstado == EstadoCliente.BLOQUEADO && motivoNormalizado == null) {
            return invalid("motivoBloqueo", "El motivo de bloqueo es obligatorio.");
        }
        if (excede(motivoNormalizado, 300)) {
            return invalid("motivoBloqueo", "El motivo de bloqueo no debe exceder 300 caracteres.");
        }
        return Result.success(new Cliente(id, tenantId, tipoCliente, tipoDocumento, numeroDocumento, detalles,
                nuevoEstado, motivoNormalizado, createdAt, updatedAt));
    }

    private static Result<Detalles, ErrorDetail> validateDetalles(
            TipoCliente tipoCliente, String nombres, String apellidos, String razonSocial, String email,
            String telefono, LocalDate fechaNacimiento, Instant referencia) {
        var nombresNormalizados = normalizeSpaces(nombres);
        var apellidosNormalizados = normalizeSpaces(apellidos);
        var razonSocialNormalizada = normalizeSpaces(razonSocial);
        var emailNormalizado = normalize(email);
        var telefonoNormalizado = normalize(telefono);

        if (tipoCliente == TipoCliente.NATURAL
                && (nombresNormalizados == null || apellidosNormalizados == null)) {
            return invalid("nombres", "Los nombres y apellidos son obligatorios para un cliente natural.");
        }
        if (tipoCliente == TipoCliente.JURIDICO && razonSocialNormalizada == null) {
            return invalid("razonSocial", "La razón social es obligatoria para un cliente jurídico.");
        }
        if (excede(nombresNormalizados, 150) || excede(apellidosNormalizados, 180)
                || excede(razonSocialNormalizada, 300)) {
            return invalid("nombres", "Los nombres, apellidos o la razón social exceden la longitud permitida.");
        }
        if (emailNormalizado != null && !EMAIL_PATTERN.matcher(emailNormalizado).matches()) {
            return invalid("email", "El correo electrónico no es válido.");
        }
        if (excede(telefonoNormalizado, 40)) {
            return invalid("telefono", "El teléfono no debe exceder 40 caracteres.");
        }
        if (fechaNacimiento != null && fechaNacimiento.isAfter(LocalDate.ofInstant(referencia, ZoneOffset.UTC))) {
            return invalid("fechaNacimiento", "La fecha de nacimiento no puede ser futura.");
        }
        return Result.success(new Detalles(nombresNormalizados, apellidosNormalizados, razonSocialNormalizada,
                emailNormalizado, telefonoNormalizado, fechaNacimiento));
    }

    private static boolean documentoValido(String tipo, String numero) {
        return switch (tipo) {
            case DNI -> DNI_PATTERN.matcher(numero).matches();
            case RUC -> RUC_PATTERN.matcher(numero).matches();
            default -> DOCUMENTO_PATTERN.matcher(numero).matches();
        };
    }

    private static String mensajeDocumento(String tipo) {
        return switch (tipo) {
            case DNI -> "El DNI debe tener 8 dígitos.";
            case RUC -> "El RUC debe tener 11 dígitos.";
            default -> "El número de documento debe tener hasta 15 caracteres alfanuméricos.";
        };
    }

    private static boolean excede(String value, int max) {
        return value != null && value.length() > max;
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CLI_CLIENTE_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public ClienteId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public TipoCliente tipoCliente() { return tipoCliente; }
    public String tipoDocumento() { return tipoDocumento; }
    public String numeroDocumento() { return numeroDocumento; }
    public String nombres() { return detalles.nombres(); }
    public String apellidos() { return detalles.apellidos(); }
    public String razonSocial() { return detalles.razonSocial(); }
    public String email() { return detalles.email(); }
    public String telefono() { return detalles.telefono(); }
    public LocalDate fechaNacimiento() { return detalles.fechaNacimiento(); }
    public EstadoCliente estado() { return estado; }
    public String motivoBloqueo() { return motivoBloqueo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
