package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarFormaFarmaceuticaQuery;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultarFormaFarmaceuticaHandlerTest {

    @Test
    void returnsFormaFarmaceuticaResultWhenFound() {
        var formaFarmaceutica = FormaFarmaceutica.restore("TABLETA", "Tableta", "DIGEMID", EstadoCatalogoSoporte.ACTIVO);
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.of(formaFarmaceutica));
        var handler = new ConsultarFormaFarmaceuticaHandler(port);

        var result = handler.execute(new ConsultarFormaFarmaceuticaQuery("TABLETA"));

        assertTrue(result.isSuccess());
        result.fold(
                success -> {
                    assertEquals("TABLETA", success.codigo());
                    assertEquals("Tableta", success.denominacion());
                    return null;
                },
                failure -> null);
    }

    @Test
    void returnsNotFoundWhenMissing() {
        CatalogoSoportePort port = new StubCatalogoSoportePort(Optional.empty());
        var handler = new ConsultarFormaFarmaceuticaHandler(port);

        var result = handler.execute(new ConsultarFormaFarmaceuticaQuery("NO_EXISTE"));

        assertTrue(result.isFailure());
        result.fold(
                success -> null,
                failure -> {
                    assertEquals("CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA", failure.code());
                    assertEquals(ErrorCategory.NOT_FOUND, failure.category());
                    return null;
                });
    }

    private record StubCatalogoSoportePort(Optional<FormaFarmaceutica> formaFarmaceutica)
            implements CatalogoSoportePort {

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
            return formaFarmaceutica;
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
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) {
            throw new UnsupportedOperationException();
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
