package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.ProductoReguladoId;
import com.softprimesolutions.catalogo.domain.valueobject.SkuId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Unidad/presentación comercial que se vende en retail, opcionalmente ligada a un producto regulado. */
public final class SKUComercial extends AggregateRoot {

    private static final int CODIGO_INTERNO_MIN_LENGTH = 2;
    private static final int CODIGO_INTERNO_MAX_LENGTH = 60;
    private static final int DESCRIPCION_COMERCIAL_MIN_LENGTH = 2;
    private static final int DESCRIPCION_COMERCIAL_MAX_LENGTH = 500;
    private static final int NOMBRE_CORTO_MAX_LENGTH = 200;
    private static final int PRESENTACION_COMERCIAL_MAX_LENGTH = 300;
    private static final int CODIGO_REFERENCIA_MAX_LENGTH = 30;
    private static final int IMAGEN_URI_MAX_LENGTH = 2000;

    private final SkuId id;
    private final TenantId tenantId;
    private final ProductoReguladoId productoReguladoId;
    private final CategoriaProductoId categoriaId;
    private final MarcaId marcaId;
    private final TipoSku tipoSku;
    private final String codigoInterno;
    private final String descripcionComercial;
    private final String nombreCorto;
    private final String presentacionComercial;
    private final String unidadVentaCodigo;
    private final BigDecimal contenido;
    private final String unidadContenidoCodigo;
    private final BigDecimal pesoGramos;
    private final BigDecimal altoCm;
    private final BigDecimal anchoCm;
    private final BigDecimal largoCm;
    private final boolean permiteVentaFraccion;
    private final BigDecimal factorFraccion;
    private final String unidadFraccionCodigo;
    private final boolean requiereLote;
    private final boolean requiereVencimiento;
    private final boolean afectoIgv;
    private final BigDecimal stockMinimoDefault;
    private final BigDecimal stockMaximoDefault;
    private final String imagenUri;
    private final List<CodigoBarraSku> codigosBarra;
    private final EstadoComercialSku estado;
    private final String createdBy;
    private final Instant createdAt;
    private final String updatedBy;
    private final Instant updatedAt;

    private SKUComercial(
            SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId,
            MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial,
            String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, String unidadFraccionCodigo,
            boolean requiereLote, boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, List<CodigoBarraSku> codigosBarra,
            EstadoComercialSku estado, String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.productoReguladoId = productoReguladoId;
        this.categoriaId = categoriaId;
        this.marcaId = marcaId;
        this.tipoSku = tipoSku;
        this.codigoInterno = codigoInterno;
        this.descripcionComercial = descripcionComercial;
        this.nombreCorto = nombreCorto;
        this.presentacionComercial = presentacionComercial;
        this.unidadVentaCodigo = unidadVentaCodigo;
        this.contenido = contenido;
        this.unidadContenidoCodigo = unidadContenidoCodigo;
        this.pesoGramos = pesoGramos;
        this.altoCm = altoCm;
        this.anchoCm = anchoCm;
        this.largoCm = largoCm;
        this.permiteVentaFraccion = permiteVentaFraccion;
        this.factorFraccion = factorFraccion;
        this.unidadFraccionCodigo = unidadFraccionCodigo;
        this.requiereLote = requiereLote;
        this.requiereVencimiento = requiereVencimiento;
        this.afectoIgv = afectoIgv;
        this.stockMinimoDefault = stockMinimoDefault;
        this.stockMaximoDefault = stockMaximoDefault;
        this.imagenUri = imagenUri;
        this.codigosBarra = List.copyOf(codigosBarra);
        this.estado = estado;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public static Result<SKUComercial, ErrorDetail> create(
            SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId,
            MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial,
            String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, String unidadFraccionCodigo,
            boolean requiereLote, boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, String createdBy, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del SKU es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (tipoSku == null) return invalid("tipoSku", "El tipo de SKU es obligatorio.");
        if (tipoSku == TipoSku.REGULADO && productoReguladoId == null) {
            return invalid("productoReguladoId", "Un SKU regulado requiere un producto regulado asociado.");
        }
        if (createdBy == null || createdBy.isBlank()) {
            return invalid("createdBy", "El creador del registro es obligatorio.");
        }
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedCodigoInterno = normalizeSpaces(codigoInterno);
        if (normalizedCodigoInterno == null || normalizedCodigoInterno.length() < CODIGO_INTERNO_MIN_LENGTH
                || normalizedCodigoInterno.length() > CODIGO_INTERNO_MAX_LENGTH) {
            return invalid("codigoInterno", "El código interno debe tener entre 2 y 60 caracteres.");
        }

        var normalizedDescripcionComercial = normalizeSpaces(descripcionComercial);
        if (normalizedDescripcionComercial == null
                || normalizedDescripcionComercial.length() < DESCRIPCION_COMERCIAL_MIN_LENGTH
                || normalizedDescripcionComercial.length() > DESCRIPCION_COMERCIAL_MAX_LENGTH) {
            return invalid("descripcionComercial", "La descripción comercial debe tener entre 2 y 500 caracteres.");
        }

        var normalizedNombreCorto = normalizeNullable(nombreCorto);
        if (!withinLength(normalizedNombreCorto, NOMBRE_CORTO_MAX_LENGTH)) {
            return invalid("nombreCorto", "El nombre corto no debe exceder 200 caracteres.");
        }

        var normalizedPresentacionComercial = normalizeNullable(presentacionComercial);
        if (!withinLength(normalizedPresentacionComercial, PRESENTACION_COMERCIAL_MAX_LENGTH)) {
            return invalid("presentacionComercial", "La presentación comercial no debe exceder 300 caracteres.");
        }

        var normalizedUnidadVentaCodigo = normalizeUpper(unidadVentaCodigo);
        if (!withinLength(normalizedUnidadVentaCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("unidadVentaCodigo", "El código de unidad de venta no debe exceder 30 caracteres.");
        }

        if (contenido != null && contenido.signum() <= 0) {
            return invalid("contenido", "El contenido debe ser mayor que cero si se informa.");
        }

        var normalizedUnidadContenidoCodigo = normalizeUpper(unidadContenidoCodigo);
        if (!withinLength(normalizedUnidadContenidoCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("unidadContenidoCodigo", "El código de unidad de contenido no debe exceder 30 caracteres.");
        }

        if (pesoGramos != null && pesoGramos.signum() <= 0) {
            return invalid("pesoGramos", "El peso debe ser mayor que cero si se informa.");
        }
        if (altoCm != null && altoCm.signum() <= 0) {
            return invalid("altoCm", "El alto debe ser mayor que cero si se informa.");
        }
        if (anchoCm != null && anchoCm.signum() <= 0) {
            return invalid("anchoCm", "El ancho debe ser mayor que cero si se informa.");
        }
        if (largoCm != null && largoCm.signum() <= 0) {
            return invalid("largoCm", "El largo debe ser mayor que cero si se informa.");
        }

        if (permiteVentaFraccion && (factorFraccion == null || factorFraccion.signum() <= 0)) {
            return invalid("factorFraccion", "El factor de fracción es obligatorio y positivo cuando se permite venta por fracción.");
        }
        if (!permiteVentaFraccion && factorFraccion != null) {
            return invalid("factorFraccion", "El factor de fracción debe ser nulo cuando no se permite venta por fracción.");
        }

        var normalizedUnidadFraccionCodigo = normalizeUpper(unidadFraccionCodigo);
        if (!withinLength(normalizedUnidadFraccionCodigo, CODIGO_REFERENCIA_MAX_LENGTH)) {
            return invalid("unidadFraccionCodigo", "El código de unidad de fracción no debe exceder 30 caracteres.");
        }
        if (permiteVentaFraccion && normalizedUnidadFraccionCodigo == null) {
            return invalid("unidadFraccionCodigo", "El código de unidad de fracción es obligatorio cuando se permite venta por fracción.");
        }
        if (!permiteVentaFraccion && normalizedUnidadFraccionCodigo != null) {
            return invalid("unidadFraccionCodigo", "El código de unidad de fracción debe ser nulo cuando no se permite venta por fracción.");
        }

        if (stockMinimoDefault == null || stockMinimoDefault.signum() < 0) {
            return invalid("stockMinimoDefault", "El stock mínimo por defecto debe ser mayor o igual a cero.");
        }
        if (stockMaximoDefault != null && stockMaximoDefault.compareTo(stockMinimoDefault) < 0) {
            return invalid("stockMaximoDefault", "El stock máximo por defecto no puede ser menor que el mínimo.");
        }

        var normalizedImagenUri = normalizeNullable(imagenUri);
        if (!withinLength(normalizedImagenUri, IMAGEN_URI_MAX_LENGTH)) {
            return invalid("imagenUri", "La URI de imagen no debe exceder 2000 caracteres.");
        }

        return Result.success(new SKUComercial(
                id, tenantId, productoReguladoId, categoriaId, marcaId, tipoSku, normalizedCodigoInterno,
                normalizedDescripcionComercial, normalizedNombreCorto, normalizedPresentacionComercial,
                normalizedUnidadVentaCodigo, contenido, normalizedUnidadContenidoCodigo, pesoGramos, altoCm,
                anchoCm, largoCm, permiteVentaFraccion, factorFraccion, normalizedUnidadFraccionCodigo,
                requiereLote, requiereVencimiento, afectoIgv, stockMinimoDefault, stockMaximoDefault,
                normalizedImagenUri, List.of(), EstadoComercialSku.ACTIVO, createdBy.trim(), createdAt, null, null));
    }

    public static SKUComercial restore(
            SkuId id, TenantId tenantId, ProductoReguladoId productoReguladoId, CategoriaProductoId categoriaId,
            MarcaId marcaId, TipoSku tipoSku, String codigoInterno, String descripcionComercial,
            String nombreCorto, String presentacionComercial, String unidadVentaCodigo, BigDecimal contenido,
            String unidadContenidoCodigo, BigDecimal pesoGramos, BigDecimal altoCm, BigDecimal anchoCm,
            BigDecimal largoCm, boolean permiteVentaFraccion, BigDecimal factorFraccion, String unidadFraccionCodigo,
            boolean requiereLote, boolean requiereVencimiento, boolean afectoIgv, BigDecimal stockMinimoDefault,
            BigDecimal stockMaximoDefault, String imagenUri, List<CodigoBarraSku> codigosBarra,
            EstadoComercialSku estado, String createdBy, Instant createdAt, String updatedBy,
            Instant updatedAt) {
        return new SKUComercial(
                id, tenantId, productoReguladoId, categoriaId, marcaId, tipoSku, codigoInterno,
                descripcionComercial, nombreCorto, presentacionComercial, unidadVentaCodigo, contenido,
                unidadContenidoCodigo, pesoGramos, altoCm, anchoCm, largoCm, permiteVentaFraccion,
                factorFraccion, unidadFraccionCodigo, requiereLote, requiereVencimiento, afectoIgv,
                stockMinimoDefault, stockMaximoDefault, imagenUri, codigosBarra, estado, createdBy, createdAt,
                updatedBy, updatedAt);
    }

    public SKUComercial conCodigoBarra(CodigoBarraSku codigo) {
        var nuevaLista = new ArrayList<>(codigosBarra);
        nuevaLista.removeIf(existing -> existing.codigoBarra().equals(codigo.codigoBarra()));
        nuevaLista.add(codigo);
        return copyWithCodigosBarra(nuevaLista);
    }

    public SKUComercial sinCodigoBarra(String codigoBarra) {
        var nuevaLista = new ArrayList<>(codigosBarra);
        nuevaLista.removeIf(existing -> existing.codigoBarra().equals(codigoBarra));
        return copyWithCodigosBarra(nuevaLista);
    }

    public SKUComercial conCodigoBarraPrincipal(String codigoBarra) {
        var nuevaLista = codigosBarra.stream()
                .map(existing -> new CodigoBarraSku(
                        existing.codigoBarra(), existing.tipoCodigo(),
                        existing.codigoBarra().equals(codigoBarra), existing.vigenteDesde(),
                        existing.vigenteHasta(), existing.estado()))
                .toList();
        return copyWithCodigosBarra(new ArrayList<>(nuevaLista));
    }

    private SKUComercial copyWithCodigosBarra(List<CodigoBarraSku> nuevaLista) {
        return new SKUComercial(
                id, tenantId, productoReguladoId, categoriaId, marcaId, tipoSku, codigoInterno,
                descripcionComercial, nombreCorto, presentacionComercial, unidadVentaCodigo, contenido,
                unidadContenidoCodigo, pesoGramos, altoCm, anchoCm, largoCm, permiteVentaFraccion,
                factorFraccion, unidadFraccionCodigo, requiereLote, requiereVencimiento, afectoIgv,
                stockMinimoDefault, stockMaximoDefault, imagenUri, nuevaLista, estado, createdBy, createdAt,
                updatedBy, updatedAt);
    }

    private static Result<SKUComercial, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_SKU_INVALIDO", message, Map.of("field", field)));
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

    public SkuId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public ProductoReguladoId productoReguladoId() { return productoReguladoId; }
    public CategoriaProductoId categoriaId() { return categoriaId; }
    public MarcaId marcaId() { return marcaId; }
    public TipoSku tipoSku() { return tipoSku; }
    public String codigoInterno() { return codigoInterno; }
    public String descripcionComercial() { return descripcionComercial; }
    public String nombreCorto() { return nombreCorto; }
    public String presentacionComercial() { return presentacionComercial; }
    public String unidadVentaCodigo() { return unidadVentaCodigo; }
    public BigDecimal contenido() { return contenido; }
    public String unidadContenidoCodigo() { return unidadContenidoCodigo; }
    public BigDecimal pesoGramos() { return pesoGramos; }
    public BigDecimal altoCm() { return altoCm; }
    public BigDecimal anchoCm() { return anchoCm; }
    public BigDecimal largoCm() { return largoCm; }
    public boolean permiteVentaFraccion() { return permiteVentaFraccion; }
    public BigDecimal factorFraccion() { return factorFraccion; }
    public String unidadFraccionCodigo() { return unidadFraccionCodigo; }
    public boolean requiereLote() { return requiereLote; }
    public boolean requiereVencimiento() { return requiereVencimiento; }
    public boolean afectoIgv() { return afectoIgv; }
    public BigDecimal stockMinimoDefault() { return stockMinimoDefault; }
    public BigDecimal stockMaximoDefault() { return stockMaximoDefault; }
    public String imagenUri() { return imagenUri; }
    public List<CodigoBarraSku> codigosBarra() { return codigosBarra; }
    public EstadoComercialSku estado() { return estado; }
    public String createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
    public String updatedBy() { return updatedBy; }
    public Instant updatedAt() { return updatedAt; }
}
