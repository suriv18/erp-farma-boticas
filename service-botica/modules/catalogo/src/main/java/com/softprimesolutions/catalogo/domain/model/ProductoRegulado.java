package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Ficha regulatoria global de un producto farmacéutico (registro sanitario, composición, condición de venta). */
public final class ProductoRegulado extends AggregateRoot {

    private static final int TIPO_PRODUCTO_MIN_LENGTH = 2;
    private static final int TIPO_PRODUCTO_MAX_LENGTH = 40;
    private static final int RUBRO_CODIGO_MAX_LENGTH = 50;
    private static final int TIPO_REGISTRO_MAX_LENGTH = 40;
    private static final int NUMERO_REGISTRO_MAX_LENGTH = 100;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 500;
    private static final int CONCENTRACION_TEXTO_MAX_LENGTH = 300;
    private static final int PRESENTACION_REGULATORIA_MAX_LENGTH = 500;
    private static final int CODIGO_REFERENCIA_MAX_LENGTH = 40;
    private static final int CLASIFICACION_ATC_MAX_LENGTH = 30;
    private static final int TIPO_LIBERACION_MAX_LENGTH = 40;
    private static final int ORIGEN_FABRICACION_MAX_LENGTH = 40;
    private static final int PAIS_ORIGEN_MAX_LENGTH = 100;
    private static final int SUBPARTIDA_NACIONAL_MAX_LENGTH = 30;
    private static final int PARTE_INTERESADA_MAX_LENGTH = 300;
    private static final int ESTABLECIMIENTO_EXPENDIO_MAX_LENGTH = 200;
    private static final int FUENTE_MAX_LENGTH = 300;
    private static final int VERSION_FUENTE_MAX_LENGTH = 100;

    private final ProductoReguladoId id;
    private final String tipoProducto;
    private final String rubroCodigo;
    private final String tipoRegistro;
    private final String numeroRegistro;
    private final String denominacion;
    private final String concentracionTexto;
    private final String presentacionRegulatoria;
    private final String formaFarmaceuticaCodigo;
    private final String viaAdministracionCodigo;
    private final String unidadMedidaCodigo;
    private final String condicionVentaCodigo;
    private final String clasificacionAtc;
    private final String clasificacionControladaCodigo;
    private final String tipoLiberacion;
    private final String origenFabricacion;
    private final String paisOrigen;
    private final String subpartidaNacional;
    private final String titularRegistro;
    private final String fabricante;
    private final String importador;
    private final String establecimientoExpendio;
    private final LocalDate vigenteDesde;
    private final LocalDate vigenteHasta;
    private final String fuente;
    private final String versionFuente;
    private final List<PrincipioActivoAsociado> principiosActivos;
    private final EstadoRegulatorio estado;
    private final Instant createdAt;
    private final Instant updatedAt;

    private ProductoRegulado(
            ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String fuente, String versionFuente, List<PrincipioActivoAsociado> principiosActivos,
            EstadoRegulatorio estado, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tipoProducto = tipoProducto;
        this.rubroCodigo = rubroCodigo;
        this.tipoRegistro = tipoRegistro;
        this.numeroRegistro = numeroRegistro;
        this.denominacion = denominacion;
        this.concentracionTexto = concentracionTexto;
        this.presentacionRegulatoria = presentacionRegulatoria;
        this.formaFarmaceuticaCodigo = formaFarmaceuticaCodigo;
        this.viaAdministracionCodigo = viaAdministracionCodigo;
        this.unidadMedidaCodigo = unidadMedidaCodigo;
        this.condicionVentaCodigo = condicionVentaCodigo;
        this.clasificacionAtc = clasificacionAtc;
        this.clasificacionControladaCodigo = clasificacionControladaCodigo;
        this.tipoLiberacion = tipoLiberacion;
        this.origenFabricacion = origenFabricacion;
        this.paisOrigen = paisOrigen;
        this.subpartidaNacional = subpartidaNacional;
        this.titularRegistro = titularRegistro;
        this.fabricante = fabricante;
        this.importador = importador;
        this.establecimientoExpendio = establecimientoExpendio;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.fuente = fuente;
        this.versionFuente = versionFuente;
        this.principiosActivos = List.copyOf(principiosActivos);
        this.estado = estado;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<ProductoRegulado, ErrorDetail> create(
            ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String fuente, String versionFuente, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del producto regulado es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedTipoProducto = normalizeSpaces(tipoProducto);
        if (normalizedTipoProducto == null || normalizedTipoProducto.length() < TIPO_PRODUCTO_MIN_LENGTH
                || normalizedTipoProducto.length() > TIPO_PRODUCTO_MAX_LENGTH) {
            return invalid("tipoProducto", "El tipo de producto debe tener entre 2 y 40 caracteres.");
        }

        var normalizedRubroCodigo = normalizeNullable(rubroCodigo);
        if (!withinLength(normalizedRubroCodigo, RUBRO_CODIGO_MAX_LENGTH)) {
            return invalid("rubroCodigo", "El rubro no debe exceder 50 caracteres.");
        }

        var normalizedTipoRegistro = normalizeNullable(tipoRegistro);
        if (!withinLength(normalizedTipoRegistro, TIPO_REGISTRO_MAX_LENGTH)) {
            return invalid("tipoRegistro", "El tipo de registro no debe exceder 40 caracteres.");
        }

        var normalizedNumeroRegistro = normalizeNullable(numeroRegistro);
        if (!withinLength(normalizedNumeroRegistro, NUMERO_REGISTRO_MAX_LENGTH)) {
            return invalid("numeroRegistro", "El número de registro no debe exceder 100 caracteres.");
        }

        if ((normalizedTipoRegistro == null) != (normalizedNumeroRegistro == null)) {
            return invalid("tipoRegistro", "El tipo y número de registro deben informarse juntos.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 500 caracteres.");
        }

        var normalizedConcentracionTexto = normalizeNullable(concentracionTexto);
        if (!withinLength(normalizedConcentracionTexto, CONCENTRACION_TEXTO_MAX_LENGTH)) {
            return invalid("concentracionTexto", "La concentración no debe exceder 300 caracteres.");
        }

        var normalizedPresentacionRegulatoria = normalizeNullable(presentacionRegulatoria);
        if (!withinLength(normalizedPresentacionRegulatoria, PRESENTACION_REGULATORIA_MAX_LENGTH)) {
            return invalid("presentacionRegulatoria", "La presentación regulatoria no debe exceder 500 caracteres.");
        }

        var normalizedFormaFarmaceuticaCodigo = normalizeUpper(formaFarmaceuticaCodigo);
        if (!withinLength(normalizedFormaFarmaceuticaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("formaFarmaceuticaCodigo", "El código de forma farmacéutica no debe exceder 40 caracteres.");
        }

        var normalizedViaAdministracionCodigo = normalizeUpper(viaAdministracionCodigo);
        if (!withinLength(normalizedViaAdministracionCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("viaAdministracionCodigo", "El código de vía de administración no debe exceder 40 caracteres.");
        }

        var normalizedUnidadMedidaCodigo = normalizeUpper(unidadMedidaCodigo);
        if (!withinLength(normalizedUnidadMedidaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("unidadMedidaCodigo", "El código de unidad de medida no debe exceder 40 caracteres.");
        }

        var normalizedCondicionVentaCodigo = normalizeUpper(condicionVentaCodigo);
        if (!withinLength(normalizedCondicionVentaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("condicionVentaCodigo", "El código de condición de venta no debe exceder 40 caracteres.");
        }

        var normalizedClasificacionAtc = normalizeUpper(clasificacionAtc);
        if (!withinLength(normalizedClasificacionAtc, CLASIFICACION_ATC_MAX_LENGTH)) {
            return invalid("clasificacionAtc", "La clasificación ATC no debe exceder 30 caracteres.");
        }

        var normalizedClasificacionControladaCodigo = normalizeUpper(clasificacionControladaCodigo);
        if (!withinLength(normalizedClasificacionControladaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("clasificacionControladaCodigo", "El código de clasificación controlada no debe exceder 40 caracteres.");
        }

        var normalizedTipoLiberacion = normalizeNullable(tipoLiberacion);
        if (!withinLength(normalizedTipoLiberacion, TIPO_LIBERACION_MAX_LENGTH)) {
            return invalid("tipoLiberacion", "El tipo de liberación no debe exceder 40 caracteres.");
        }

        var normalizedOrigenFabricacion = normalizeNullable(origenFabricacion);
        if (!withinLength(normalizedOrigenFabricacion, ORIGEN_FABRICACION_MAX_LENGTH)) {
            return invalid("origenFabricacion", "El origen de fabricación no debe exceder 40 caracteres.");
        }

        var normalizedPaisOrigen = normalizeNullable(paisOrigen);
        if (!withinLength(normalizedPaisOrigen, PAIS_ORIGEN_MAX_LENGTH)) {
            return invalid("paisOrigen", "El país de origen no debe exceder 100 caracteres.");
        }

        var normalizedSubpartidaNacional = normalizeNullable(subpartidaNacional);
        if (!withinLength(normalizedSubpartidaNacional, SUBPARTIDA_NACIONAL_MAX_LENGTH)) {
            return invalid("subpartidaNacional", "La subpartida nacional no debe exceder 30 caracteres.");
        }

        var normalizedTitularRegistro = normalizeNullable(titularRegistro);
        if (!withinLength(normalizedTitularRegistro, PARTE_INTERESADA_MAX_LENGTH)) {
            return invalid("titularRegistro", "El titular de registro no debe exceder 300 caracteres.");
        }

        var normalizedFabricante = normalizeNullable(fabricante);
        if (!withinLength(normalizedFabricante, PARTE_INTERESADA_MAX_LENGTH)) {
            return invalid("fabricante", "El fabricante no debe exceder 300 caracteres.");
        }

        var normalizedImportador = normalizeNullable(importador);
        if (!withinLength(normalizedImportador, PARTE_INTERESADA_MAX_LENGTH)) {
            return invalid("importador", "El importador no debe exceder 300 caracteres.");
        }

        var normalizedEstablecimientoExpendio = normalizeNullable(establecimientoExpendio);
        if (!withinLength(normalizedEstablecimientoExpendio, ESTABLECIMIENTO_EXPENDIO_MAX_LENGTH)) {
            return invalid("establecimientoExpendio", "El establecimiento de expendio no debe exceder 200 caracteres.");
        }

        if (vigenteDesde != null && vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            return invalid("vigenteHasta", "La vigencia hasta no puede ser anterior a la vigencia desde.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        var normalizedVersionFuente = normalizeNullable(versionFuente);
        if (!withinLength(normalizedVersionFuente, VERSION_FUENTE_MAX_LENGTH)) {
            return invalid("versionFuente", "La versión de fuente no debe exceder 100 caracteres.");
        }

        return Result.success(new ProductoRegulado(
                id, normalizedTipoProducto, normalizedRubroCodigo, normalizedTipoRegistro,
                normalizedNumeroRegistro, normalizedDenominacion, normalizedConcentracionTexto,
                normalizedPresentacionRegulatoria, normalizedFormaFarmaceuticaCodigo,
                normalizedViaAdministracionCodigo, normalizedUnidadMedidaCodigo, normalizedCondicionVentaCodigo,
                normalizedClasificacionAtc, normalizedClasificacionControladaCodigo, normalizedTipoLiberacion,
                normalizedOrigenFabricacion, normalizedPaisOrigen, normalizedSubpartidaNacional,
                normalizedTitularRegistro, normalizedFabricante, normalizedImportador,
                normalizedEstablecimientoExpendio, vigenteDesde, vigenteHasta, normalizedFuente,
                normalizedVersionFuente, List.of(), EstadoRegulatorio.VIGENTE, createdAt, null));
    }

    public static ProductoRegulado restore(
            ProductoReguladoId id, String tipoProducto, String rubroCodigo, String tipoRegistro,
            String numeroRegistro, String denominacion, String concentracionTexto,
            String presentacionRegulatoria, String formaFarmaceuticaCodigo, String viaAdministracionCodigo,
            String unidadMedidaCodigo, String condicionVentaCodigo, String clasificacionAtc,
            String clasificacionControladaCodigo, String tipoLiberacion, String origenFabricacion,
            String paisOrigen, String subpartidaNacional, String titularRegistro, String fabricante,
            String importador, String establecimientoExpendio, LocalDate vigenteDesde, LocalDate vigenteHasta,
            String fuente, String versionFuente, List<PrincipioActivoAsociado> principiosActivos,
            EstadoRegulatorio estado, Instant createdAt, Instant updatedAt) {
        return new ProductoRegulado(
                id, tipoProducto, rubroCodigo, tipoRegistro, numeroRegistro, denominacion, concentracionTexto,
                presentacionRegulatoria, formaFarmaceuticaCodigo, viaAdministracionCodigo, unidadMedidaCodigo,
                condicionVentaCodigo, clasificacionAtc, clasificacionControladaCodigo, tipoLiberacion,
                origenFabricacion, paisOrigen, subpartidaNacional, titularRegistro, fabricante, importador,
                establecimientoExpendio, vigenteDesde, vigenteHasta, fuente, versionFuente, principiosActivos,
                estado, createdAt, updatedAt);
    }

    public ProductoRegulado conPrincipioActivoAsociado(PrincipioActivoAsociado asociado) {
        var nuevaLista = new ArrayList<>(principiosActivos);
        nuevaLista.removeIf(existing -> existing.principioActivoId().equals(asociado.principioActivoId()));
        nuevaLista.add(asociado);
        return new ProductoRegulado(
                id, tipoProducto, rubroCodigo, tipoRegistro, numeroRegistro, denominacion, concentracionTexto,
                presentacionRegulatoria, formaFarmaceuticaCodigo, viaAdministracionCodigo, unidadMedidaCodigo,
                condicionVentaCodigo, clasificacionAtc, clasificacionControladaCodigo, tipoLiberacion,
                origenFabricacion, paisOrigen, subpartidaNacional, titularRegistro, fabricante, importador,
                establecimientoExpendio, vigenteDesde, vigenteHasta, fuente, versionFuente, nuevaLista,
                estado, createdAt, updatedAt);
    }

    public ProductoRegulado sinPrincipioActivoAsociado(PrincipioActivoId principioActivoId) {
        var nuevaLista = new ArrayList<>(principiosActivos);
        nuevaLista.removeIf(existing -> existing.principioActivoId().equals(principioActivoId));
        return new ProductoRegulado(
                id, tipoProducto, rubroCodigo, tipoRegistro, numeroRegistro, denominacion, concentracionTexto,
                presentacionRegulatoria, formaFarmaceuticaCodigo, viaAdministracionCodigo, unidadMedidaCodigo,
                condicionVentaCodigo, clasificacionAtc, clasificacionControladaCodigo, tipoLiberacion,
                origenFabricacion, paisOrigen, subpartidaNacional, titularRegistro, fabricante, importador,
                establecimientoExpendio, vigenteDesde, vigenteHasta, fuente, versionFuente, nuevaLista,
                estado, createdAt, updatedAt);
    }

    private static Result<ProductoRegulado, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_PRODUCTO_REGULADO_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        var normalized = normalizeSpaces(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public ProductoReguladoId id() { return id; }
    public String tipoProducto() { return tipoProducto; }
    public String rubroCodigo() { return rubroCodigo; }
    public String tipoRegistro() { return tipoRegistro; }
    public String numeroRegistro() { return numeroRegistro; }
    public String denominacion() { return denominacion; }
    public String concentracionTexto() { return concentracionTexto; }
    public String presentacionRegulatoria() { return presentacionRegulatoria; }
    public String formaFarmaceuticaCodigo() { return formaFarmaceuticaCodigo; }
    public String viaAdministracionCodigo() { return viaAdministracionCodigo; }
    public String unidadMedidaCodigo() { return unidadMedidaCodigo; }
    public String condicionVentaCodigo() { return condicionVentaCodigo; }
    public String clasificacionAtc() { return clasificacionAtc; }
    public String clasificacionControladaCodigo() { return clasificacionControladaCodigo; }
    public String tipoLiberacion() { return tipoLiberacion; }
    public String origenFabricacion() { return origenFabricacion; }
    public String paisOrigen() { return paisOrigen; }
    public String subpartidaNacional() { return subpartidaNacional; }
    public String titularRegistro() { return titularRegistro; }
    public String fabricante() { return fabricante; }
    public String importador() { return importador; }
    public String establecimientoExpendio() { return establecimientoExpendio; }
    public LocalDate vigenteDesde() { return vigenteDesde; }
    public LocalDate vigenteHasta() { return vigenteHasta; }
    public String fuente() { return fuente; }
    public String versionFuente() { return versionFuente; }
    public List<PrincipioActivoAsociado> principiosActivos() { return principiosActivos; }
    public EstadoRegulatorio estado() { return estado; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
