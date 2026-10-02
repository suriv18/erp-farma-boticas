package com.softprimesolutions.compras.application.usecase.command;

import com.softprimesolutions.compras.application.dto.command.CrearOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.command.LineaOrdenCompraInput;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.in.CrearOrdenCompraUseCase;
import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.NumeracionPort;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort.EstablecimientoRef;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort.SkuRef;
import com.softprimesolutions.compras.domain.model.CondicionesOrden;
import com.softprimesolutions.compras.domain.model.LineaNueva;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class CrearOrdenCompraHandler implements CrearOrdenCompraUseCase {

    private static final ZoneId ZONA_OPERATIVA = ZoneId.of("America/Lima");

    private final OrdenCompraWritePort ordenes;
    private final ProveedorWritePort proveedores;
    private final ReferenciasComprasPort referencias;
    private final NumeracionPort numeracion;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public CrearOrdenCompraHandler(
            OrdenCompraWritePort ordenes, ProveedorWritePort proveedores, ReferenciasComprasPort referencias,
            NumeracionPort numeracion, IdentifierGenerator identifiers, ClockPort clock) {
        this.ordenes = Objects.requireNonNull(ordenes, "ordenes es obligatorio");
        this.proveedores = Objects.requireNonNull(proveedores, "proveedores es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.numeracion = Objects.requireNonNull(numeracion, "numeracion es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<OrdenCompraResult, ApplicationError> execute(CrearOrdenCompraCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var proveedor = proveedores.findById(command.tenantId(), command.proveedorId());
        if (proveedor.isEmpty()) return Result.failure(ComprasErrors.proveedorNoEncontrado());
        if (!proveedor.get().estado().admiteCompras()) return Result.failure(ComprasErrors.proveedorNoOperable());
        var establecimiento = referencias.establecimiento(command.tenantId(), command.establecimientoDestinoId());
        if (establecimiento.isEmpty()) return Result.failure(ComprasErrors.establecimientoNoEncontrado());
        if (!establecimiento.get().operable()) return Result.failure(ComprasErrors.establecimientoNoOperable());
        var skus = referencias.skus(
                command.tenantId(), command.lineas().stream().map(LineaOrdenCompraInput::skuId).distinct().toList());
        var errorLineas = command.lineas().stream().map(linea -> errorLinea(linea, skus))
                .flatMap(Optional::stream).findFirst();
        if (errorLineas.isPresent()) return Result.failure(errorLineas.get());
        return crear(command, proveedor.get(), establecimiento.get(), skus);
    }

    private Result<OrdenCompraResult, ApplicationError> crear(
            CrearOrdenCompraCommand command, Proveedor proveedor, EstablecimientoRef establecimiento,
            Map<UUID, SkuRef> skus) {
        var ahora = clock.now();
        var lineas = command.lineas().stream().map(linea -> nueva(linea, skus.get(linea.skuId()))).toList();
        return OrdenCompra.crear(
                        identifiers.next(), command.tenantId(), establecimiento.empresaId(), proveedor.id(),
                        establecimiento.id(), numeracion.siguienteNumeroOrden(), LocalDate.ofInstant(ahora, ZONA_OPERATIVA),
                        condiciones(command, proveedor), lineas, new Actor(command.actorId()), ahora)
                .fold(
                        this::guardar,
                        error -> Result.<OrdenCompraResult, ApplicationError>failure(ComprasErrors.fromDomain(error)));
    }

    private Result<OrdenCompraResult, ApplicationError> guardar(OrdenCompra orden) {
        if (ordenes.insertar(orden) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(ComprasErrors.modificacionConcurrente());
        }
        return Result.success(ComprasApplicationMapper.toResult(orden));
    }

    private Optional<ApplicationError> errorLinea(LineaOrdenCompraInput linea, Map<UUID, SkuRef> skus) {
        var sku = skus.get(linea.skuId());
        if (sku == null) return Optional.of(ComprasErrors.skuNoEncontrado(linea.skuId()));
        if (!sku.operable()) return Optional.of(ComprasErrors.skuNoOperable(linea.skuId()));
        if (!referencias.existeUnidadMedida(linea.unidadMedidaCodigo())) {
            return Optional.of(ComprasErrors.unidadMedidaNoEncontrada(linea.unidadMedidaCodigo()));
        }
        return Optional.empty();
    }

    private static LineaNueva nueva(LineaOrdenCompraInput linea, SkuRef sku) {
        return new LineaNueva(
                linea.skuId(), sku.descripcion(), linea.cantidad(), linea.unidadMedidaCodigo(),
                linea.precioUnitario(), Objects.requireNonNullElse(linea.descuento(), BigDecimal.ZERO),
                Objects.requireNonNullElse(linea.impuesto(), BigDecimal.ZERO),
                Objects.requireNonNullElse(linea.toleranciaExcesoPct(), BigDecimal.ZERO),
                Objects.requireNonNullElse(linea.toleranciaDefectoPct(), BigDecimal.ZERO));
    }

    private static CondicionesOrden condiciones(CrearOrdenCompraCommand command, Proveedor proveedor) {
        var datos = proveedor.datos();
        return new CondicionesOrden(
                command.fechaEntregaEstimada(), Objects.requireNonNullElse(command.moneda(), datos.monedaDefault()),
                Objects.requireNonNullElse(command.tipoCambio(), BigDecimal.ONE),
                Objects.requireNonNullElse(command.condicionPago(), datos.condicionPagoDefault()),
                Objects.requireNonNullElse(command.diasCredito(), datos.diasCreditoDefault()), command.observacion());
    }
}
