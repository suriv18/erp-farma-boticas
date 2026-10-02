package com.softprimesolutions.compras.domain.model;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR;
import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.datosProveedor;
import static com.softprimesolutions.compras.ComprasFixtures.domainValue;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProveedorTest {

    private static final Instant DESPUES = AHORA.plusSeconds(60);

    @Test
    void aNewProveedorIsActiveAndAudited() {
        var proveedor = Proveedor.crear(PROVEEDOR, TENANT, datosProveedor(), ACTOR, AHORA);

        assertThat(proveedor.id()).isEqualTo(PROVEEDOR);
        assertThat(proveedor.tenantId()).isEqualTo(TENANT);
        assertThat(proveedor.datos()).isEqualTo(datosProveedor());
        assertThat(proveedor.estado()).isEqualTo(EstadoProveedor.ACTIVO);
        assertThat(proveedor.createdAt()).isEqualTo(AHORA);
        assertThat(proveedor.createdBy()).isEqualTo(ACTOR_ID.toString());
        assertThat(proveedor.updatedAt()).isNull();
        assertThat(proveedor.updatedBy()).isNull();
        assertThat(proveedor.pullDomainEvents()).isEmpty();
    }

    @Test
    void updatingChangesTheDataAndTheUpdateAuditButKeepsStateAndCreation() {
        var original = Proveedor.crear(PROVEEDOR, TENANT, datosProveedor(), ACTOR, AHORA);
        var nuevos = domainValue(DatosProveedor.crear(
                null, "20100070970", "Razon nueva", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null));

        var actualizado = original.actualizar(nuevos, ACTOR, DESPUES);

        assertThat(actualizado.datos().razonSocial()).isEqualTo("Razon nueva");
        assertThat(actualizado.estado()).isEqualTo(EstadoProveedor.ACTIVO);
        assertThat(actualizado.createdAt()).isEqualTo(AHORA);
        assertThat(actualizado.updatedAt()).isEqualTo(DESPUES);
        assertThat(actualizado.updatedBy()).isEqualTo(ACTOR_ID.toString());
    }

    @Test
    void changingTheStateKeepsTheDataAndAuditsTheChange() {
        var original = Proveedor.crear(PROVEEDOR, TENANT, datosProveedor(), ACTOR, AHORA);

        var bloqueado = original.cambiarEstado(EstadoProveedor.BLOQUEADO, ACTOR, DESPUES);

        assertThat(bloqueado.estado()).isEqualTo(EstadoProveedor.BLOQUEADO);
        assertThat(bloqueado.datos()).isEqualTo(original.datos());
        assertThat(bloqueado.updatedAt()).isEqualTo(DESPUES);
    }

    @Test
    void restoringKeepsTheStoredAudit() {
        var restaurado = Proveedor.restore(
                PROVEEDOR, TENANT, datosProveedor(), EstadoProveedor.SUSPENDIDO, AHORA, "creador", DESPUES, "editor");

        assertThat(restaurado.estado()).isEqualTo(EstadoProveedor.SUSPENDIDO);
        assertThat(restaurado.createdBy()).isEqualTo("creador");
        assertThat(restaurado.updatedBy()).isEqualTo("editor");
    }
}
