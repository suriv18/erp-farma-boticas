package com.softprimesolutions.catalogo.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarTipoDocumentoIdentidadHandlerTest {

    @Test
    void updatesATipoDocumentoIdentidadSuccessfully() {
        var writePort = new FakeCatalogoSoportePort();
        var handler = new ActualizarTipoDocumentoIdentidadHandler(writePort);

        var result = handler.execute(new ActualizarTipoDocumentoIdentidadCommand(
                "1", "DNI", "Documento Nacional de Identidad", 8, 8));

        assertTrue(result.isSuccess());
        assertEquals("1", result.getOrElse(error -> null).codigo());
    }

    @Test
    void failsWithNotFoundWhenCodigoDoesNotExist() {
        var writePort = new FakeCatalogoSoportePort();
        writePort.outcome = CatalogoSoportePort.SaveOutcome.NOT_FOUND;
        var handler = new ActualizarTipoDocumentoIdentidadHandler(writePort);

        var result = handler.execute(new ActualizarTipoDocumentoIdentidadCommand(
                "1", "DNI", "Documento Nacional de Identidad", 8, 8));

        assertTrue(result.isFailure());
        assertEquals(
                "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void failsWithValidationErrorWhenCodigoIsInvalid() {
        var writePort = new FakeCatalogoSoportePort();
        var handler = new ActualizarTipoDocumentoIdentidadHandler(writePort);

        var result = handler.execute(new ActualizarTipoDocumentoIdentidadCommand(
                "", "DNI", "Documento Nacional de Identidad", 8, 8));

        assertTrue(result.isFailure());
        assertEquals(
                "CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoSoportePort implements CatalogoSoportePort {
        private SaveOutcome outcome = SaveOutcome.UPDATED;

        @Override
        public SaveOutcome save(CondicionVenta condicionVenta) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(FormaFarmaceutica formaFarmaceutica) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ViaAdministracion viaAdministracion) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(UnidadMedida unidadMedida) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(ClasificacionControlada clasificacionControlada) { throw new UnsupportedOperationException(); }

        @Override
        public SaveOutcome save(TipoDocumentoIdentidad tipoDocumentoIdentidad) { return outcome; }

        @Override
        public SavePrincipioActivoOutcome save(PrincipioActivo principioActivo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<CondicionVenta> findCondicionVentaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<FormaFarmaceutica> findFormaFarmaceuticaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ViaAdministracion> findViaAdministracionByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<UnidadMedida> findUnidadMedidaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<ClasificacionControlada> findClasificacionControladaByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<TipoDocumentoIdentidad> findTipoDocumentoIdentidadByCodigo(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<PrincipioActivo> findPrincipioActivoById(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean condicionVentaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean formaFarmaceuticaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean viaAdministracionExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean unidadMedidaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean clasificacionControladaExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean tipoDocumentoIdentidadExists(String codigo) { throw new UnsupportedOperationException(); }

        @Override
        public boolean principioActivoExists(UUID principioActivoId) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeCondicionVentaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeFormaFarmaceuticaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeViaAdministracionStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeUnidadMedidaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeClasificacionControladaStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changeTipoDocumentoIdentidadStatus(String codigo, String status, Instant changedAt) { throw new UnsupportedOperationException(); }

        @Override
        public boolean changePrincipioActivoStatus(UUID principioActivoId, String status, Instant changedAt) { throw new UnsupportedOperationException(); }
    }
}
