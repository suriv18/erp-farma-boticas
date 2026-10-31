package com.softprimesolutions.organizacion.api;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarAlmacenRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarTerminalPosRequest;
import com.softprimesolutions.organizacion.api.dto.request.CambiarEstadoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearAlmacenRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearTerminalPosRequest;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public final class OrganizacionApiFixtures {

    public static final UUID TENANT = UUID.randomUUID();
    public static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none")
            .claim("tid", TENANT.toString()).build();
    public static final UUID EMPRESA = UUID.randomUUID();
    public static final UUID ESTABLECIMIENTO = UUID.randomUUID();
    public static final UUID ALMACEN = UUID.randomUUID();
    public static final UUID TERMINAL = UUID.randomUUID();
    public static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-02-01T00:00:00Z");
    public static final ApplicationError CONFLICT = new StandardApplicationError(
            "ORG_CONFLICTO", "Conflicto de prueba.", ErrorCategory.CONFLICT);

    private OrganizacionApiFixtures() {
    }

    public static CambiarEstadoRequest cambiarEstadoRequest(String estado) {
        return new CambiarEstadoRequest(estado);
    }

    public static <T> PaginaResult<T> page(T item) {
        return new PaginaResult<>(List.of(item), 1, 10, 11);
    }

    public static CrearEmpresaOperadoraRequest crearEmpresaRequest() {
        return new CrearEmpresaOperadoraRequest(
                "20123456786", "Boticas SAC", "Boticas", "Av. 1", "150101", "01444", "a@b.pe",
                "https://b.pe", "PEN", "America/Lima", true);
    }

    public static ActualizarEmpresaOperadoraRequest actualizarEmpresaRequest() {
        return new ActualizarEmpresaOperadoraRequest(
                "Boticas SAC", "Boticas", "Av. 1", "150101", "01444", "a@b.pe", "https://b.pe", "PEN",
                "America/Lima", true);
    }

    public static EmpresaOperadoraResult empresaResult() {
        return new EmpresaOperadoraResult(
                EMPRESA, TENANT, "20123456786", "Boticas SAC", "Boticas", "Av. 1", "150101", "01444", "a@b.pe",
                "https://b.pe", "PEN", "America/Lima", true, "ACTIVO", CREATED_AT, UPDATED_AT);
    }

    public static CrearEstablecimientoRequest crearEstablecimientoRequest() {
        return new CrearEstablecimientoRequest(
                EMPRESA, "EST001", "Botica Central", "BOTICA", "CAT", "0001", "DIG001", "Av. 2",
                "150101", "Frente al parque", BigDecimal.ONE, BigDecimal.TEN, "01444", "e@b.pe", true, true,
                true, "ONLINE", "America/Lima");
    }

    public static ActualizarEstablecimientoRequest actualizarEstablecimientoRequest() {
        return new ActualizarEstablecimientoRequest(
                "Botica Central", "BOTICA", "CAT", "0001", "DIG001", "Av. 2", "150101", "Frente al parque",
                BigDecimal.ONE, BigDecimal.TEN, "01444", "e@b.pe", true, true, true, "ONLINE", "America/Lima");
    }

    public static EstablecimientoResult establecimientoResult() {
        return new EstablecimientoResult(
                ESTABLECIMIENTO, TENANT, EMPRESA, "EST001", "Botica Central", "BOTICA", "CAT", "0001", "DIG001",
                "Av. 2", "150101", "Frente al parque", BigDecimal.ONE, BigDecimal.TEN, "01444", "e@b.pe", true,
                true, true, "ONLINE", "America/Lima", "ACTIVO", CREATED_AT, UPDATED_AT);
    }

    public static CrearAlmacenRequest crearAlmacenRequest() {
        return new CrearAlmacenRequest(
                ESTABLECIMIENTO, "ALM001", "Almacén Central", "GENERAL", true, true, true, true, true,
                BigDecimal.ONE, BigDecimal.TEN);
    }

    public static ActualizarAlmacenRequest actualizarAlmacenRequest() {
        return new ActualizarAlmacenRequest(
                "Almacén Central", "GENERAL", true, true, true, true, true, BigDecimal.ONE, BigDecimal.TEN, true);
    }

    public static AlmacenResult almacenResult() {
        return new AlmacenResult(
                ALMACEN, TENANT, ESTABLECIMIENTO, "ALM001", "Almacén Central", "GENERAL", true, true, true, true,
                true, BigDecimal.ONE, BigDecimal.TEN, true, CREATED_AT, UPDATED_AT);
    }

    public static CrearTerminalPosRequest crearTerminalRequest() {
        return new CrearTerminalPosRequest(
                ESTABLECIMIENTO, "POS001", "Caja 1", "B001", "F001", "SN-1", "host-1", "10.0.0.1",
                "IMP01", true);
    }

    public static ActualizarTerminalPosRequest actualizarTerminalRequest() {
        return new ActualizarTerminalPosRequest(
                "Caja 1", "B001", "F001", "SN-1", "host-1", "10.0.0.1", "IMP01", true, "ACTIVO");
    }

    public static TerminalPosResult terminalResult() {
        return new TerminalPosResult(
                TERMINAL, TENANT, ESTABLECIMIENTO, "POS001", "Caja 1", "B001", "F001", "SN-1", "host-1",
                "10.0.0.1", "IMP01", true, "ACTIVO", CREATED_AT, UPDATED_AT);
    }
}
