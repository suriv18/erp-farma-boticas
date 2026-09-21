package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.EstadoPrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarPrincipioActivoHandlerTest {

    private static final UUID PRINCIPIO_ACTIVO_ID = UUID.randomUUID();

    @Test
    void returnsPrincipioActivoResultWhenFound() {
        var principioActivo = PrincipioActivo.restore(
                new PrincipioActivoId(PRINCIPIO_ACTIVO_ID), "PARAC-500", "Paracetamol", "PARACETAMOL", "DIGEMID",
                EstadoPrincipioActivo.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(principioActivo));
        var handler = new ConsultarPrincipioActivoHandler(port);

        var result = handler.execute(new ConsultarPrincipioActivoQuery(PRINCIPIO_ACTIVO_ID));

        assertTrue(result.isSuccess());
        result.fold(
                success -> {
                    assertEquals(PRINCIPIO_ACTIVO_ID, success.id());
                    assertEquals("Paracetamol", success.denominacion());
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarPrincipioActivoHandler(port);

        var result = handler.execute(new ConsultarPrincipioActivoQuery(PRINCIPIO_ACTIVO_ID));

        assertTrue(result.isFailure());
        result.fold(
                success -> null,
                failure -> {
                    assertEquals("CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", failure.code());
                    assertEquals(ErrorCategory.NOT_FOUND, failure.category());
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<PrincipioActivo> principioActivo) implements CatalogoSoportePort {

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            return principioActivo;
        }

        @Override
        public boolean condicionVentaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean viaAdministracionExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean unidadMedidaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean clasificacionControladaExists(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) {
            throw new UnsupportedOperationException();
        }
    }
}
