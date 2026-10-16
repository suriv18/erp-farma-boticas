package com.softprimesolutions.compras.application.mapper;

import com.softprimesolutions.compras.application.dto.command.ProveedorInput;
import com.softprimesolutions.compras.application.dto.result.LineaOrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.domain.model.DatosProveedor;
import com.softprimesolutions.compras.domain.model.LineaOrdenCompra;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;

public final class ComprasApplicationMapper {

    private ComprasApplicationMapper() {
    }

    public static Result<DatosProveedor, ErrorDetail> toDatos(ProveedorInput input) {
        return DatosProveedor.crear(
                input.tipoDocumento(), input.numeroDocumento(), input.razonSocial(), input.nombreComercial(),
                input.direccion(), input.ubigeo(), input.telefono(), input.email(), input.contactoNombre(),
                input.contactoTelefono(), input.contactoEmail(), input.condicionPagoDefault(),
                input.diasCreditoDefault(), input.monedaDefault(), input.esLaboratorio(), input.esImportador(),
                input.esDistribuidor(), input.calificacion());
    }

    public static ProveedorResult toResult(Proveedor proveedor) {
        var datos = proveedor.datos();
        return new ProveedorResult(
                proveedor.id(), datos.tipoDocumento(), datos.numeroDocumento(), datos.razonSocial(),
                datos.nombreComercial(), datos.direccion(), datos.ubigeo(), datos.telefono(), datos.email(),
                datos.contactoNombre(), datos.contactoTelefono(), datos.contactoEmail(),
                datos.condicionPagoDefault(), datos.diasCreditoDefault(), datos.monedaDefault(),
                datos.esLaboratorio(), datos.esImportador(), datos.esDistribuidor(), datos.calificacion(),
                proveedor.estado().name());
    }

    public static OrdenCompraResult toResult(OrdenCompra orden) {
        var condiciones = orden.condiciones();
        return new OrdenCompraResult(
                orden.id(), orden.numero(), orden.proveedorId(), orden.establecimientoDestinoId(),
                orden.fechaEmision(), condiciones.fechaEntregaEstimada(), condiciones.moneda(),
                condiciones.tipoCambio(), condiciones.condicionPago(), condiciones.diasCredito(), orden.subtotal(),
                orden.descuentoTotal(), orden.impuestoTotal(), orden.total(), orden.estado().name(),
                condiciones.observacion(), orden.aprobadoAt(),
                orden.lineas().stream().map(ComprasApplicationMapper::toResult).toList());
    }

    private static LineaOrdenCompraResult toResult(LineaOrdenCompra linea) {
        return new LineaOrdenCompraResult(
                linea.numeroLinea(), linea.skuId(), linea.descripcionSnapshot(), linea.cantidad(),
                linea.unidadMedidaCodigo(), linea.precioUnitario(), linea.descuento(), linea.impuesto(),
                linea.totalLinea(), linea.toleranciaExcesoPct(), linea.toleranciaDefectoPct(),
                linea.cantidadRecibida(), linea.pendiente());
    }
}
