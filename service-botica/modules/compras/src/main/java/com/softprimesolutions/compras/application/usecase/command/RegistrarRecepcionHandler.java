package com.softprimesolutions.compras.application.usecase.command;

import com.softprimesolutions.compras.application.dto.command.ItemRecepcionInput;
import com.softprimesolutions.compras.application.dto.command.RegistrarRecepcionCommand;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.port.in.RegistrarRecepcionUseCase;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort.IngresoSolicitado;
import com.softprimesolutions.compras.application.port.out.NumeracionPort;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.application.port.out.RecepcionWritePort;
import com.softprimesolutions.compras.application.port.out.RecepcionWritePort.RecepcionExistente;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort;
import com.softprimesolutions.compras.application.port.out.TransaccionPort;
import com.softprimesolutions.compras.domain.model.DatosRecepcion;
import com.softprimesolutions.compras.domain.model.ItemRecepcion;
import com.softprimesolutions.compras.domain.model.LineaRecepcion;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.compras.domain.model.Recepcion;
import com.softprimesolutions.compras.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class RegistrarRecepcionHandler implements RegistrarRecepcionUseCase {

    private static final int MAX_INTENTOS = 3;
    private static final int CLAVE_IDEMPOTENCIA_MAX = 200;
    private static final String DOCUMENTO_PROVEEDOR_DEFAULT = "01";

    private final OrdenCompraWritePort ordenes;
    private final RecepcionWritePort recepciones;
    private final ComprasReadPort lecturas;
    private final ReferenciasComprasPort referencias;
    private final IngresoInventarioPort inventario;
    private final NumeracionPort numeracion;
    private final TransaccionPort transaccion;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public RegistrarRecepcionHandler(
            OrdenCompraWritePort ordenes, RecepcionWritePort recepciones, ComprasReadPort lecturas,
            ReferenciasComprasPort referencias, IngresoInventarioPort inventario, NumeracionPort numeracion,
            TransaccionPort transaccion, IdentifierGenerator identifiers, ClockPort clock) {
        this.ordenes = Objects.requireNonNull(ordenes, "ordenes es obligatorio");
        this.recepciones = Objects.requireNonNull(recepciones, "recepciones es obligatorio");
        this.lecturas = Objects.requireNonNull(lecturas, "lecturas es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.inventario = Objects.requireNonNull(inventario, "inventario es obligatorio");
        this.numeracion = Objects.requireNonNull(numeracion, "numeracion es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<RecepcionResult, ApplicationError> execute(RegistrarRecepcionCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (claveIdempotenciaInvalida(command.idempotencyKey())) {
            return Result.failure(ComprasErrors.claveIdempotenciaInvalida());
        }
        var solicitud = new Solicitud(command, businessUuid(command), huella(command));
        var resultado = intentar(solicitud);
        for (var intento = 1; intento < MAX_INTENTOS && esConcurrencia(resultado); intento++) {
            resultado = intentar(solicitud);
        }
        return resultado;
    }

    private Result<RecepcionResult, ApplicationError> intentar(Solicitud solicitud) {
        var tenantId = solicitud.command().tenantId();
        var previo = Optional.ofNullable(solicitud.businessUuid())
                .flatMap(clave -> recepciones.findPorBusinessUuid(tenantId, clave));
        if (previo.isPresent()) return repetir(tenantId, previo.get(), solicitud.huella());
        return transaccion.ejecutar(() -> registrar(solicitud)).flatMap(id -> leer(tenantId, id));
    }

    private Result<RecepcionResult, ApplicationError> repetir(
            UUID tenantId, RecepcionExistente previo, String huella) {
        if (!previo.huella().equals(huella)) return Result.failure(ComprasErrors.conflictoIdempotencia());
        return leer(tenantId, previo.id());
    }

    private Result<RecepcionResult, ApplicationError> leer(UUID tenantId, UUID recepcionId) {
        return lecturas.findRecepcion(tenantId, recepcionId)
                .<Result<RecepcionResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ComprasErrors.recepcionNoEncontrada()));
    }

    private Result<UUID, ApplicationError> registrar(Solicitud solicitud) {
        var command = solicitud.command();
        var orden = ordenes.findByIdParaActualizar(command.tenantId(), command.ordenCompraId());
        if (orden.isEmpty()) return Result.failure(ComprasErrors.ordenNoEncontrada());
        var almacen = referencias.almacen(command.tenantId(), command.almacenId());
        if (almacen.isEmpty()) return Result.failure(ComprasErrors.almacenNoEncontrado());
        if (!almacen.get().operable()) return Result.failure(ComprasErrors.almacenNoOperable());
        if (!almacen.get().establecimientoId().equals(orden.get().establecimientoDestinoId())) {
            return Result.failure(ComprasErrors.almacenDeOtroEstablecimiento());
        }
        var ahora = clock.now();
        var actor = new Actor(command.actorId());
        return Recepcion.registrar(
                        identifiers.next(), almacen.get().establecimientoId(), datos(command), orden.get(),
                        command.items().stream().map(RegistrarRecepcionHandler::item).toList(), actor, ahora,
                        identifiers::next)
                .fold(
                        recepcion -> persistir(solicitud, orden.get(), recepcion, actor, ahora),
                        error -> Result.<UUID, ApplicationError>failure(ComprasErrors.fromDomain(error)));
    }

    private Result<UUID, ApplicationError> persistir(
            Solicitud solicitud, OrdenCompra orden, Recepcion recepcion, Actor actor, Instant ahora) {
        if (recepciones.insertar(recepcion, solicitud.businessUuid(), solicitud.huella()) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(ComprasErrors.modificacionConcurrente());
        }
        var fallo = recepcion.lineas().stream()
                .filter(LineaRecepcion::ingresaStock)
                .map(linea -> inventario.ingresar(ingreso(solicitud.command(), recepcion, linea)))
                .flatMap(resultado -> resultado.fold(ingreso -> Stream.<ApplicationError>empty(), Stream::of))
                .findFirst();
        if (fallo.isPresent()) return Result.failure(fallo.get());
        var nueva = orden.conRecepcion(recepcion.aceptadoPorLineaOrden(), actor, ahora);
        if (!ordenes.actualizarEstado(nueva, orden.estado())) {
            return Result.failure(ComprasErrors.modificacionConcurrente());
        }
        return Result.success(recepcion.id());
    }

    private static IngresoSolicitado ingreso(RegistrarRecepcionCommand command, Recepcion recepcion, LineaRecepcion linea) {
        return new IngresoSolicitado(
                command.tenantId(), recepcion.datos().almacenId(), linea.skuId(), linea.numeroLote(),
                linea.fechaVencimiento(), linea.cantidadAceptada(), recepcion.id(), linea.id(),
                recepcion.proveedorId(), command.actorId(), "recepcion:" + recepcion.id() + ":" + linea.numeroLinea());
    }

    private DatosRecepcion datos(RegistrarRecepcionCommand command) {
        return new DatosRecepcion(
                numeracion.siguienteNumeroRecepcion(), command.almacenId(),
                Objects.requireNonNullElse(command.documentoProveedorTipo(), DOCUMENTO_PROVEEDOR_DEFAULT),
                command.documentoProveedorSerie(), command.documentoProveedorNumero(),
                command.guiaRemisionRemitente(), command.guiaRemisionTransportista(),
                command.temperaturaRecepcionC(), command.humedadRelativaPct(), command.observacion());
    }

    private static ItemRecepcion item(ItemRecepcionInput input) {
        return new ItemRecepcion(
                input.numeroLineaOrden(), input.numeroLote(), input.fechaFabricacion(), input.fechaVencimiento(),
                input.cantidadRecibida(), Objects.requireNonNullElse(input.cantidadRechazada(), BigDecimal.ZERO),
                input.motivoRechazo(), input.costoUnitario(), input.observacion());
    }

    private static boolean esConcurrencia(Result<RecepcionResult, ApplicationError> resultado) {
        return resultado.fold(recepcion -> false, error -> ComprasErrors.CONCURRENCIA.equals(error.code()));
    }

    private static boolean claveIdempotenciaInvalida(String clave) {
        return clave != null && (clave.isBlank() || clave.length() > CLAVE_IDEMPOTENCIA_MAX);
    }

    private static UUID businessUuid(RegistrarRecepcionCommand command) {
        return Optional.ofNullable(command.idempotencyKey())
                .map(clave -> UUID.nameUUIDFromBytes(("recepcion:" + clave.trim()).getBytes(StandardCharsets.UTF_8)))
                .orElse(null);
    }

    private static String huella(RegistrarRecepcionCommand command) {
        var canonico = String.join("|",
                text(command.ordenCompraId()), text(command.almacenId()), text(command.documentoProveedorTipo()),
                text(command.documentoProveedorSerie()), text(command.documentoProveedorNumero()),
                text(command.guiaRemisionRemitente()), text(command.guiaRemisionTransportista()),
                text(command.temperaturaRecepcionC()), text(command.humedadRelativaPct()),
                text(command.observacion()), command.items().toString(), text(command.actorId()));
        return UUID.nameUUIDFromBytes(canonico.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String text(Object valor) {
        return Objects.toString(valor, "");
    }

    private record Solicitud(RegistrarRecepcionCommand command, UUID businessUuid, String huella) {
    }
}
