package com.softprimesolutions.compras.application.usecase.command;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.error;
import static com.softprimesolutions.compras.ComprasFixtures.proveedor;
import static com.softprimesolutions.compras.ComprasFixtures.proveedorInput;
import static com.softprimesolutions.compras.ComprasFixtures.value;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.compras.application.dto.command.ActualizarProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.CambiarEstadoProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.CrearProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.ProveedorInput;
import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProveedorHandlersTest {

    private static final ProveedorInput INVALIDO = new ProveedorInput(
            "6", "123", "Laboratorios", null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null);

    private final ProveedorWritePort port = mock(ProveedorWritePort.class);
    private final CrearProveedorHandler crear = new CrearProveedorHandler(port, () -> PROVEEDOR, () -> AHORA);
    private final ActualizarProveedorHandler actualizar = new ActualizarProveedorHandler(port, () -> AHORA);
    private final CambiarEstadoProveedorHandler cambiarEstado = new CambiarEstadoProveedorHandler(port, () -> AHORA);

    private Proveedor saved(boolean insert) {
        var captor = ArgumentCaptor.forClass(Proveedor.class);
        if (insert) verify(port).insertar(captor.capture());
        else verify(port).actualizar(captor.capture());
        return captor.getValue();
    }

    @Test
    void createsAnActiveProveedorAudited() {
        when(port.insertar(any())).thenReturn(GuardadoOutcome.GUARDADO);

        var result = value(crear.execute(new CrearProveedorCommand(TENANT, ACTOR_ID, proveedorInput())));

        assertThat(result.id()).isEqualTo(PROVEEDOR);
        assertThat(result.estado()).isEqualTo("ACTIVO");
        var proveedor = saved(true);
        assertThat(proveedor.tenantId()).isEqualTo(TENANT);
        assertThat(proveedor.createdBy()).isEqualTo(ACTOR_ID.toString());
        assertThat(proveedor.createdAt()).isEqualTo(AHORA);
    }

    @Test
    void doesNotCreateAProveedorWithInvalidData() {
        var error = error(crear.execute(new CrearProveedorCommand(TENANT, ACTOR_ID, INVALIDO)));

        assertThat(error.code()).isEqualTo("COM_PROVEEDOR_INVALIDO");
        assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
        verify(port, never()).insertar(any());
    }

    @Test
    void reportsADuplicatedDocumentWhenCreating() {
        when(port.insertar(any())).thenReturn(GuardadoOutcome.DUPLICADO);

        var error = error(crear.execute(new CrearProveedorCommand(TENANT, ACTOR_ID, proveedorInput())));

        assertThat(error.code()).isEqualTo("COM_PROVEEDOR_DUPLICADO");
    }

    @Test
    void updatesTheDataOfAnExistingProveedor() {
        when(port.findById(TENANT, PROVEEDOR)).thenReturn(Optional.of(proveedor(EstadoProveedor.ACTIVO)));
        when(port.actualizar(any())).thenReturn(GuardadoOutcome.GUARDADO);
        var input = new ProveedorInput(
                null, "20100070970", "Razon nueva", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);

        var result = value(actualizar.execute(new ActualizarProveedorCommand(TENANT, PROVEEDOR, ACTOR_ID, input)));

        assertThat(result.razonSocial()).isEqualTo("Razon nueva");
        var proveedor = saved(false);
        assertThat(proveedor.updatedBy()).isEqualTo(ACTOR_ID.toString());
        assertThat(proveedor.updatedAt()).isEqualTo(AHORA);
    }

    @Test
    void doesNotUpdateAnUnknownProveedorOrWithInvalidData() {
        var command = new ActualizarProveedorCommand(TENANT, PROVEEDOR, ACTOR_ID, proveedorInput());
        assertThat(error(actualizar.execute(command)).code()).isEqualTo("COM_PROVEEDOR_NO_ENCONTRADO");

        when(port.findById(TENANT, PROVEEDOR)).thenReturn(Optional.of(proveedor(EstadoProveedor.ACTIVO)));
        var invalido = new ActualizarProveedorCommand(TENANT, PROVEEDOR, ACTOR_ID, INVALIDO);
        assertThat(error(actualizar.execute(invalido)).code()).isEqualTo("COM_PROVEEDOR_INVALIDO");
        verify(port, never()).actualizar(any());
    }

    @Test
    void reportsADuplicatedDocumentWhenUpdating() {
        when(port.findById(TENANT, PROVEEDOR)).thenReturn(Optional.of(proveedor(EstadoProveedor.ACTIVO)));
        when(port.actualizar(any())).thenReturn(GuardadoOutcome.DUPLICADO);

        var error = error(actualizar.execute(
                new ActualizarProveedorCommand(TENANT, PROVEEDOR, ACTOR_ID, proveedorInput())));

        assertThat(error.code()).isEqualTo("COM_PROVEEDOR_DUPLICADO");
    }

    @Test
    void changesTheStateOfAnExistingProveedor() {
        when(port.findById(TENANT, PROVEEDOR)).thenReturn(Optional.of(proveedor(EstadoProveedor.ACTIVO)));

        var result = value(cambiarEstado.execute(
                new CambiarEstadoProveedorCommand(TENANT, PROVEEDOR, "BLOQUEADO", ACTOR_ID)));

        assertThat(result.estado()).isEqualTo("BLOQUEADO");
        assertThat(saved(false).estado()).isEqualTo(EstadoProveedor.BLOQUEADO);
    }

    @Test
    void rejectsAnUnknownStateOrAnUnknownProveedorWhenChangingTheState() {
        assertThat(error(cambiarEstado.execute(
                new CambiarEstadoProveedorCommand(TENANT, PROVEEDOR, "OTRO", ACTOR_ID))).code())
                .isEqualTo("COM_ESTADO_INVALIDO");
        assertThat(error(cambiarEstado.execute(
                new CambiarEstadoProveedorCommand(TENANT, PROVEEDOR, "ACTIVO", ACTOR_ID))).code())
                .isEqualTo("COM_PROVEEDOR_NO_ENCONTRADO");
        verify(port, never()).actualizar(any());
    }
}
