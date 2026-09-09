package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.CrearCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearCondicionVentaHandlerTest {

    @Test
    void createsACondicionVentaSuccessfully() {
        var writePort = new FakeCatalogoSoportePort();
        var handler = new CrearCondicionVentaHandler(writePort);

        var result = handler.execute(new CrearCondicionVentaCommand(
                "SIN-RECETA", "Sin receta médica", false, false, null, null, null, null));

        assertTrue(result.isSuccess());
        assertEquals("SIN-RECETA", result.getOrElse(error -> null).codigo());
    }

    @Test
    void failsWithConflictWhenCodigoAlreadyExists() {
        var writePort = new FakeCatalogoSoportePort();
        writePort.outcome = CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO;
        var handler = new CrearCondicionVentaHandler(writePort);

        var result = handler.execute(new CrearCondicionVentaCommand(
                "SIN-RECETA", "Sin receta médica", false, false, null, null, null, null));

        assertTrue(result.isFailure());
        assertEquals("CAT_CONDICION_VENTA_DUPLICADA", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
        private SaveOutcome outcome = SaveOutcome.CREATED;

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) {
            return outcome;
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
