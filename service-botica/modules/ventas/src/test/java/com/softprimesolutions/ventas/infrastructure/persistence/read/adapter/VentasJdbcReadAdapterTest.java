package com.softprimesolutions.ventas.infrastructure.persistence.read.adapter;

import java.util.List;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.LINEA;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.ventas.infrastructure.persistence.Rows;
import org.junit.jupiter.api.Test;

class VentasJdbcReadAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final VentasJdbcReadAdapter adapter = new VentasJdbcReadAdapter(jdbc.client());

    @Test
    void findsATurnoByIdAsAResult() {
        jdbc.rows("tc.uuid_publico = :turnoId", Rows.turno());

        var turno = adapter.findTurno(TENANT, TURNO).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(turno.terminalId()).isEqualTo(TERMINAL);
        assertThat(turno.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(turno.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(turno.estado()).isEqualTo("ABIERTO");
        assertThat(turno.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(jdbc.statementContaining("tc.uuid_publico = :turnoId").params())
                .containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO);
    }

    @Test
    void findsTheOpenTurnoOfATerminalIncludingOnesBeingCounted() {
        jdbc.rows("tc.estado IN ('ABIERTO', 'EN_ARQUEO')", Rows.turno());

        var turno = adapter.findTurnoAbierto(TENANT, TERMINAL).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(jdbc.statementContaining("tc.estado IN ('ABIERTO', 'EN_ARQUEO')").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }

    @Test
    void missingTurnosAreEmpty() {
        assertThat(adapter.findTurno(TENANT, TURNO)).isEmpty();
        assertThat(adapter.findTurnoAbierto(TENANT, TERMINAL)).isEmpty();
    }

    private static final String CABECERA = "tc.uuid_publico AS turno_uuid";
    private static final String LINEAS = "FROM sch_venta.venta_linea vl";
    private static final String LOTES = "FROM sch_venta.venta_linea_lote vll";
    private static final String PAGO = "FROM sch_venta.pago_venta p";

    @Test
    void assemblesASaleWithItsLinesLotesAndPayment() {
        jdbc.rows(CABECERA, Rows.ventaCabecera());
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(LOTES, Rows.ventaLote());
        jdbc.rows(PAGO, Rows.ventaPago());

        var venta = adapter.findVenta(TENANT, VENTA).orElseThrow();

        assertThat(venta.id()).isEqualTo(VENTA);
        assertThat(venta.numeroOperacion()).isEqualTo("EST001-POS01-000001");
        assertThat(venta.terminalId()).isEqualTo(TERMINAL);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(venta.vendedorId()).isEqualTo(ACTOR_ID);
        assertThat(venta.moneda()).isEqualTo("PEN");
        assertThat(venta.total()).isEqualTo(dec("12.50"));
        assertThat(venta.estado()).isEqualTo("CONFIRMADA");
        assertThat(venta.lineas()).hasSize(1);
        var linea = venta.lineas().getFirst();
        assertThat(linea.numeroLinea()).isEqualTo(1);
        assertThat(linea.skuId()).isEqualTo(SKU);
        assertThat(linea.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(linea.totalLinea()).isEqualTo(dec("12.50"));
        assertThat(linea.lotes()).hasSize(1);
        assertThat(linea.lotes().getFirst().loteId()).isEqualTo(LOTE);
        assertThat(linea.lotes().getFirst().cantidad()).isEqualTo(dec("5.0000"));
        assertThat(venta.pago().medioPago()).isEqualTo("EFECTIVO");
        assertThat(venta.pago().vuelto()).isEqualTo(dec("7.50"));
        assertThat(jdbc.statementContaining(CABECERA).params())
                .containsEntry("tenantId", TENANT).containsEntry("ventaId", VENTA);
    }

    @Test
    void aLineWithoutLotesKeepsAnEmptyList() {
        jdbc.rows(CABECERA, Rows.ventaCabecera());
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(PAGO, Rows.ventaPago());

        assertThat(adapter.findVenta(TENANT, VENTA).orElseThrow().lineas().getFirst().lotes()).isEmpty();
    }

    @Test
    void aSaleWithoutAPaymentRowHasNoPaymentInsteadOfFailing() {
        jdbc.rows(CABECERA, Rows.ventaCabecera());
        jdbc.rows(LINEAS, Rows.ventaLinea());

        var venta = adapter.findVenta(TENANT, VENTA).orElseThrow();

        assertThat(venta.pago()).isNull();
        assertThat(venta.lineas()).hasSize(1);
    }

    @Test
    void aMissingSaleIsEmptyWithoutLoadingItsDetail() {
        assertThat(adapter.findVenta(TENANT, VENTA)).isEmpty();
        assertThat(jdbc.statements()).hasSize(1);
    }

    @Test
    void listsTheSalesPageWithItsFiltersAndTotal() {
        jdbc.scalar("SELECT COUNT(*)", 3L);
        jdbc.rows("ORDER BY v.fecha_venta DESC", Rows.ventaResumen());
        var query = new ListarVentasQuery(TENANT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), 2, 10);

        var pagina = adapter.listarVentas(query);

        assertThat(pagina.page()).isEqualTo(2);
        assertThat(pagina.size()).isEqualTo(10);
        assertThat(pagina.totalElements()).isEqualTo(3L);
        assertThat(pagina.items()).hasSize(1);
        assertThat(pagina.items().getFirst().numeroOperacion()).isEqualTo("EST001-POS01-000001");
        assertThat(pagina.items().getFirst().total()).isEqualTo(dec("12.50"));
        var lista = jdbc.statementContaining("ORDER BY v.fecha_venta DESC").params();
        assertThat(lista).containsEntry("tenantId", TENANT).containsEntry("establecimientoId", ESTABLECIMIENTO)
                .containsEntry("desde", Rows.MOMENTO).containsEntry("hasta", Rows.MOMENTO.plusSeconds(60))
                .containsEntry("limit", 10).containsEntry("offset", 20);
    }

    @Test
    void listingWithoutFiltersPassesNullDates() {
        jdbc.scalar("SELECT COUNT(*)", 0L);

        var pagina = adapter.listarVentas(new ListarVentasQuery(TENANT, null, null, null, 0, 20));

        assertThat(pagina.items()).isEmpty();
        assertThat(jdbc.statementContaining("ORDER BY v.fecha_venta DESC").params())
                .containsEntry("desde", null).containsEntry("hasta", null);
    }

    @Test
    void anAnnulledSaleExposesWhoWhenAndWhyItWasAnnulled() {
        var cabecera = Rows.ventaCabecera();
        cabecera.put("estado", "ANULADA");
        cabecera.put("anulada_at", Rows.MOMENTO);
        cabecera.put("anulada_por_uuid", ACTOR_ID);
        cabecera.put("motivo_anulacion", "Error de cobro");
        jdbc.rows(CABECERA, cabecera);
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(PAGO, Rows.ventaPago());

        var venta = adapter.findVenta(TENANT, VENTA).orElseThrow();

        assertThat(venta.estado()).isEqualTo("ANULADA");
        assertThat(venta.anulacion().anuladaAt()).isEqualTo(Rows.MOMENTO.toInstant());
        assertThat(venta.anulacion().anuladaPorId()).isEqualTo(ACTOR_ID);
        assertThat(venta.anulacion().motivo()).isEqualTo("Error de cobro");
    }

    @Test
    void aSaleThatWasNeverAnnulledHasNoAnulacionBlock() {
        jdbc.rows(CABECERA, Rows.ventaCabecera());
        jdbc.rows(LINEAS, Rows.ventaLinea());
        jdbc.rows(PAGO, Rows.ventaPago());

        assertThat(adapter.findVenta(TENANT, VENTA).orElseThrow().anulacion()).isNull();
    }
}
