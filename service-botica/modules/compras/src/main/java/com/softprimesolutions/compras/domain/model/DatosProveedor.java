package com.softprimesolutions.compras.domain.model;

import com.softprimesolutions.compras.domain.exception.ComprasErrorCodes;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public record DatosProveedor(
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String nombreComercial,
        String direccion,
        String ubigeo,
        String telefono,
        String email,
        String contactoNombre,
        String contactoTelefono,
        String contactoEmail,
        String condicionPagoDefault,
        int diasCreditoDefault,
        String monedaDefault,
        boolean esLaboratorio,
        boolean esImportador,
        boolean esDistribuidor,
        String calificacion) {

    private static final String TIPO_RUC = "6";
    private static final Pattern RUC = Pattern.compile("^(10|20)[0-9]{9}$");
    private static final Pattern UBIGEO = Pattern.compile("^[0-9]{6}$");
    private static final Pattern MONEDA = Pattern.compile("^[A-Z]{3}$");

    private record Limite(String campo, String valor, int maximo) {

        boolean excede() {
            return Campos.excede(valor, maximo);
        }

        String mensaje() {
            return "El campo " + campo + " admite hasta " + maximo + " caracteres.";
        }
    }

    public static Result<DatosProveedor, ErrorDetail> crear(
            String tipoDocumento, String numeroDocumento, String razonSocial, String nombreComercial,
            String direccion, String ubigeo, String telefono, String email, String contactoNombre,
            String contactoTelefono, String contactoEmail, String condicionPagoDefault, Integer diasCreditoDefault,
            String monedaDefault, Boolean esLaboratorio, Boolean esImportador, Boolean esDistribuidor,
            String calificacion) {
        var datos = new DatosProveedor(
                Objects.requireNonNullElse(Campos.texto(tipoDocumento), TIPO_RUC), Campos.texto(numeroDocumento),
                Campos.texto(razonSocial), Campos.texto(nombreComercial), Campos.texto(direccion),
                Campos.texto(ubigeo), Campos.texto(telefono), Campos.texto(email), Campos.texto(contactoNombre),
                Campos.texto(contactoTelefono), Campos.texto(contactoEmail),
                Objects.requireNonNullElse(Campos.texto(condicionPagoDefault), "CONTADO"),
                Objects.requireNonNullElse(diasCreditoDefault, 0),
                Objects.requireNonNullElse(Campos.texto(monedaDefault), "PEN"),
                Objects.requireNonNullElse(esLaboratorio, false), Objects.requireNonNullElse(esImportador, false),
                Objects.requireNonNullElse(esDistribuidor, true),
                Objects.requireNonNullElse(Campos.texto(calificacion), "CONFIABLE"));
        return datos.mensajeInvalido()
                .<Result<DatosProveedor, ErrorDetail>>map(mensaje -> Result.failure(
                        new ErrorDetail(ComprasErrorCodes.PROVEEDOR_INVALIDO, mensaje, Map.of())))
                .orElseGet(() -> Result.success(datos));
    }

    private Optional<String> mensajeInvalido() {
        if (tipoDocumento.length() > 2) {
            return Optional.of("El tipo de documento admite hasta 2 caracteres.");
        }
        if (numeroDocumento == null || numeroDocumento.length() > 15) {
            return Optional.of("El numero de documento debe tener entre 1 y 15 caracteres.");
        }
        if (TIPO_RUC.equals(tipoDocumento) && !RUC.matcher(numeroDocumento).matches()) {
            return Optional.of("El RUC debe tener 11 digitos y empezar con 10 o 20.");
        }
        if (razonSocial == null || razonSocial.length() > 300) {
            return Optional.of("La razon social debe tener entre 1 y 300 caracteres.");
        }
        if (ubigeo != null && !UBIGEO.matcher(ubigeo).matches()) {
            return Optional.of("El ubigeo debe tener 6 digitos.");
        }
        if (diasCreditoDefault < 0) {
            return Optional.of("Los dias de credito no pueden ser negativos.");
        }
        if (!MONEDA.matcher(monedaDefault).matches()) {
            return Optional.of("La moneda debe ser un codigo de 3 letras mayusculas.");
        }
        return limites().stream().filter(Limite::excede).map(Limite::mensaje).findFirst();
    }

    private List<Limite> limites() {
        return List.of(
                new Limite("nombreComercial", nombreComercial, 300), new Limite("direccion", direccion, 500),
                new Limite("telefono", telefono, 40), new Limite("email", email, 254),
                new Limite("contactoNombre", contactoNombre, 180),
                new Limite("contactoTelefono", contactoTelefono, 40), new Limite("contactoEmail", contactoEmail, 254),
                new Limite("condicionPagoDefault", condicionPagoDefault, 80),
                new Limite("calificacion", calificacion, 30));
    }
}
