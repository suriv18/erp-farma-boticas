package com.softprimesolutions.compras.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import com.softprimesolutions.testsupport.RealLogin;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class ComprasApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("7e3a1b5c-4d2f-4a6e-9b80-2c1d3e4f5a6b");
    private static final String ORG = "/api/v1/organizacion";
    private static final String COM = "/api/v1/compras";
    private static final String INV = "/api/v1/inventario";
    private static final String UNIDAD = "UNDCMP";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private RealLogin realLogin;
    private String noPermissions;

    private UUID userId;
    private String accessToken;
    private UUID establecimientoId;
    private UUID almacenId;
    private UUID otroAlmacenId;
    private UUID skuId;

    @BeforeEach
    void createTenantAdministratorAndTheMasterDataOfAPurchase() throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'CMPTEST', 'Tenant compras', 'tenant-compras', 'test')
                        """).param("tenantId", TENANT_ID).update();
        realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        var session = realLogin.login(TENANT_ID, "cmp.admin", "^(organizacion|inventario|compras)\\.");
        userId = session.userId();
        accessToken = session.bearer();
        noPermissions = realLogin.login(TENANT_ID, "sin.permisos", null).bearer();

        var empresaId = created(post(ORG + "/empresas").content("""
                {"tenantId":"%s","ruc":"20123456786","razonSocial":"Boticas Compras SAC",
                 "monedaFuncional":"PEN","zonaHoraria":"America/Lima","permiteVentaOnline":false}
                """.formatted(TENANT_ID)));
        establecimientoId = created(post(ORG + "/establecimientos").content(establecimiento(empresaId, "EST001", "0001", true)));
        almacenId = created(post(ORG + "/almacenes").content(almacen(establecimientoId, "ALM001")));
        var otroEstablecimientoId = created(post(ORG + "/establecimientos")
                .content(establecimiento(empresaId, "EST002", "0002", false)));
        otroAlmacenId = created(post(ORG + "/almacenes").content(almacen(otroEstablecimientoId, "ALM002")));

        skuId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion) VALUES (:codigo, 'Unidad')")
                .param("codigo", UNIDAD).update();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.sku_comercial
                            (uuid_publico, tenant_id, tipo_sku, codigo_interno, descripcion_comercial,
                             unidad_venta_codigo)
                        SELECT :skuId, t.id, 'NO_REGULADO', 'SKU-CMP-1', 'Paracetamol 500 mg x 100', :unidad
                          FROM sch_admin.tenant t WHERE t.uuid_publico = :tenantId
                        """).param("skuId", skuId).param("unidad", UNIDAD).param("tenantId", TENANT_ID).update();
    }

    @Test
    void takesAPurchaseFromProveedorToStockAsTheDdlDefines() throws Exception {
        var proveedorId = proveedor("20100070970", "Laboratorios Peru SAC");
        var ordenId = ordenEmitida(proveedorId, "10", "10");
        var vencimiento = LocalDate.now().plusYears(2);

        mockMvc.perform(get(COM + "/ordenes/{id}", ordenId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EMITIDA"))
                .andExpect(jsonPath("$.moneda").value("PEN"))
                .andExpect(jsonPath("$.condicionPago").value("CREDITO 30"))
                .andExpect(jsonPath("$.diasCredito").value(30))
                .andExpect(jsonPath("$.subtotal").value(55.0))
                .andExpect(jsonPath("$.impuestoTotal").value(9.9))
                .andExpect(jsonPath("$.total").value(64.9))
                .andExpect(jsonPath("$.lineas[0].descripcion").value("Paracetamol 500 mg x 100"))
                .andExpect(jsonPath("$.lineas[0].totalLinea").value(64.9))
                .andExpect(jsonPath("$.lineas[0].cantidadRecibida").value(0))
                .andExpect(jsonPath("$.lineas[0].cantidadPendiente").value(10));

        var primera = recepcion(ordenId, almacenId, "LP-001", vencimiento, "6", "1", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.lineas[0].cantidadAceptada").value(5))
                .andExpect(jsonPath("$.lineas[0].decisionCalidad").value("ACEPTADO_PARCIAL"))
                .andExpect(jsonPath("$.lineas[0].motivoDecision").value("Envase danado"))
                .andReturn().getResponse().getContentAsString();
        var recepcionId = UUID.fromString(JsonPath.read(primera, "$.id"));
        var loteId = UUID.fromString(JsonPath.read(primera, "$.lineas[0].loteId"));
        assertThat((String) JsonPath.read(primera, "$.numero")).startsWith("REC-");

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer())
                        .param("almacenId", almacenId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].loteId").value(loteId.toString()))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(5))
                .andExpect(jsonPath("$.items[0].vendible").value(true));
        var kardex = jdbcClient.sql("""
                        SELECT m.tipo_movimiento, m.tipo_operacion_sunat, m.naturaleza, m.cantidad,
                               m.documento_tipo, m.documento_uuid, m.business_uuid IS NOT NULL AS con_clave,
                               m.actor, m.observacion
                          FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).query().singleRow();
        assertThat(kardex).containsEntry("tipo_movimiento", "INGRESO_COMPRA")
                .containsEntry("tipo_operacion_sunat", "02").containsEntry("naturaleza", "E")
                .containsEntry("documento_tipo", "RECEPCION_COMPRA").containsEntry("documento_uuid", recepcionId)
                .containsEntry("con_clave", true).containsEntry("actor", userId.toString());
        var origen = jdbcClient.sql("""
                        SELECT l.proveedor_id IS NOT NULL AS con_proveedor,
                               l.origen_recepcion_linea_id IS NOT NULL AS con_origen
                          FROM sch_inventario.lote l WHERE l.uuid_publico = :loteId
                        """).param("loteId", loteId).query().singleRow();
        assertThat(origen).containsEntry("con_proveedor", true).containsEntry("con_origen", true);

        mockMvc.perform(get(COM + "/ordenes/{id}", ordenId).header("Authorization", bearer()))
                .andExpect(jsonPath("$.estado").value("PARCIALMENTE_RECIBIDA"))
                .andExpect(jsonPath("$.lineas[0].cantidadRecibida").value(5))
                .andExpect(jsonPath("$.lineas[0].cantidadPendiente").value(5));

        recepcion(ordenId, almacenId, "LP-001", vencimiento, "6", "0", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lineas[0].decisionCalidad").value("ACEPTADO"))
                .andExpect(jsonPath("$.lineas[0].loteId").value(loteId.toString()));
        mockMvc.perform(get(COM + "/ordenes/{id}", ordenId).header("Authorization", bearer()))
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.lineas[0].cantidadRecibida").value(11))
                .andExpect(jsonPath("$.lineas[0].cantidadPendiente").value(0));
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(11));
        mockMvc.perform(get(COM + "/recepciones/{id}", recepcionId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenCompraId").value(ordenId.toString()))
                .andExpect(jsonPath("$.almacenId").value(almacenId.toString()))
                .andExpect(jsonPath("$.establecimientoId").value(establecimientoId.toString()))
                .andExpect(jsonPath("$.guiaRemisionRemitente").value("T001-45"))
                .andExpect(jsonPath("$.temperaturaRecepcionC").value(22.5))
                .andExpect(jsonPath("$.lineas[0].numeroLineaOrden").value(1))
                .andExpect(jsonPath("$.lineas[0].loteId").value(loteId.toString()));
        mockMvc.perform(get(COM + "/ordenes").header("Authorization", bearer()).param("estado", "RECIBIDA")
                        .param("proveedorId", proveedorId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].proveedorRazonSocial").value("Laboratorios Peru SAC"));

        recepcion(ordenId, almacenId, "LP-002", vencimiento, "1", "0", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COM_RECEPCION_ORDEN_NO_RECEPCIONABLE"));
    }

    @Test
    void validatesProveedorsAndKeepsAnInactiveOneOutOfNewOrders() throws Exception {
        var proveedorId = proveedor("20100070970", "Laboratorios Peru SAC");

        mockMvc.perform(post(COM + "/proveedores").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroDocumento\":\"30100070970\",\"razonSocial\":\"RUC invalido\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COM_PROVEEDOR_INVALIDO"));
        mockMvc.perform(post(COM + "/proveedores").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroDocumento\":\"20100070970\",\"razonSocial\":\"Duplicado\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COM_PROVEEDOR_DUPLICADO"));
        mockMvc.perform(post(COM + "/proveedores").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"razonSocial\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(COM + "/proveedores/{id}", proveedorId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroDocumento\":\"20100070970\",\"razonSocial\":\"Laboratorios Peru SA\","
                                + "\"email\":\"ventas@labperu.example\",\"diasCreditoDefault\":45}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Laboratorios Peru SA"))
                .andExpect(jsonPath("$.email").value("ventas@labperu.example"))
                .andExpect(jsonPath("$.diasCreditoDefault").value(45));
        mockMvc.perform(get(COM + "/proveedores").header("Authorization", bearer())
                        .param("texto", "peru").param("estado", "ACTIVO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(COM + "/proveedores").header("Authorization", bearer()).param("estado", "XX"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(COM + "/proveedores").header("Authorization", bearer()).param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch(COM + "/proveedores/{id}/estado", proveedorId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"BLOQUEADO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("BLOQUEADO"));
        var auditoria = jdbcClient.sql("""
                        SELECT p.estado, p.updated_by FROM sch_abastecimiento.proveedor p
                         WHERE p.uuid_publico = :id
                        """).param("id", proveedorId).query().singleRow();
        assertThat(auditoria).containsEntry("estado", "BLOQUEADO").containsEntry("updated_by", userId.toString());
        mockMvc.perform(post(COM + "/ordenes").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(orden(proveedorId, "10", "0")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COM_PROVEEDOR_NO_OPERABLE"));
        mockMvc.perform(patch(COM + "/proveedores/{id}/estado", proveedorId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"OTRO\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(COM + "/proveedores/{id}", UUID.randomUUID()).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void enforcesTheOrderLifecycleAndItsReferences() throws Exception {
        var proveedorId = proveedor("20100070970", "Laboratorios Peru SAC");
        var ordenId = ordenBorrador(proveedorId, "10", "0");

        mockMvc.perform(post(COM + "/ordenes/{id}/emision", ordenId).header("Authorization", bearer()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COM_ORDEN_ESTADO_INVALIDO"));
        mockMvc.perform(post(COM + "/ordenes/{id}/aprobacion", ordenId).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.aprobadoAt").isNotEmpty());
        var aprobado = jdbcClient.sql("""
                        SELECT o.aprobado_por_usuario_id IS NOT NULL AS con_usuario, o.updated_by
                          FROM sch_abastecimiento.orden_compra o WHERE o.uuid_publico = :id
                        """).param("id", ordenId).query().singleRow();
        assertThat(aprobado).containsEntry("con_usuario", true).containsEntry("updated_by", userId.toString());
        mockMvc.perform(post(COM + "/ordenes/{id}/aprobacion", ordenId).header("Authorization", bearer()))
                .andExpect(status().isConflict());
        recepcion(ordenId, almacenId, "LP-009", LocalDate.now().plusYears(1), "1", "0", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COM_RECEPCION_ORDEN_NO_RECEPCIONABLE"));
        mockMvc.perform(post(COM + "/ordenes/{id}/anulacion", ordenId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(COM + "/ordenes/{id}/anulacion", ordenId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Proveedor sin stock\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.observacion").value("Anulada: Proveedor sin stock | Reposicion"));
        mockMvc.perform(post(COM + "/ordenes/{id}/anulacion", ordenId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"otra vez\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post(COM + "/ordenes/{id}/emision", UUID.randomUUID()).header("Authorization", bearer()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(COM + "/ordenes").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(orden(UUID.randomUUID(), "10", "0")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COM_PROVEEDOR_NO_ENCONTRADO"));
        mockMvc.perform(post(COM + "/ordenes").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden(proveedorId, "10", "0").replace(establecimientoId.toString(),
                                UUID.randomUUID().toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COM_ESTABLECIMIENTO_NO_ENCONTRADO"));
        mockMvc.perform(post(COM + "/ordenes").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden(proveedorId, "10", "0").replace(skuId.toString(), UUID.randomUUID().toString())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COM_SKU_NO_ENCONTRADO"));
        mockMvc.perform(post(COM + "/ordenes").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden(proveedorId, "10", "0").replace(UNIDAD, "NOEXISTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM_UNIDAD_MEDIDA_NO_ENCONTRADA"));
        mockMvc.perform(post(COM + "/ordenes").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orden(proveedorId, "10", "0").replace("\"cantidad\":10", "\"cantidad\":10.00001")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COM_ORDEN_INVALIDA"));
        mockMvc.perform(get(COM + "/ordenes/{id}", UUID.randomUUID()).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(COM + "/ordenes").header("Authorization", bearer()).param("estado", "XX"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aRetryWithTheSameIdempotencyKeyReturnsTheSameReceptionWithoutMovingStockTwice() throws Exception {
        var proveedorId = proveedor("20100070970", "Laboratorios Peru SAC");
        var ordenId = ordenEmitida(proveedorId, "10", "0");
        var vencimiento = LocalDate.now().plusYears(1);

        var primero = recepcion(ordenId, almacenId, "LP-010", vencimiento, "4", "0", "clave-rec-1")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var repetido = recepcion(ordenId, almacenId, "LP-010", vencimiento, "4", "0", "clave-rec-1")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(repetido, "$.id")).isEqualTo(JsonPath.read(primero, "$.id"));
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_abastecimiento.recepcion_compra r
                          JOIN sch_admin.tenant t ON t.id = r.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).query(Long.class).single()).isEqualTo(1L);
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer()))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(4));
        recepcion(ordenId, almacenId, "LP-010", vencimiento, "5", "0", "clave-rec-1")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COM_IDEMPOTENCY_CONFLICT"));
        recepcion(ordenId, almacenId, "LP-010", vencimiento, "1", "0", "   ")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COM_IDEMPOTENCY_KEY_INVALID"));
    }

    @Test
    void validatesTheReceptionAgainstTheOrderAndTheWarehouse() throws Exception {
        var proveedorId = proveedor("20100070970", "Laboratorios Peru SAC");
        var ordenId = ordenEmitida(proveedorId, "10", "0");
        var vencimiento = LocalDate.now().plusYears(1);

        recepcion(ordenId, almacenId, "LP-012", vencimiento, "11", "0", null)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COM_RECEPCION_EXCEDE_PENDIENTE"));
        recepcion(ordenId, otroAlmacenId, "LP-012", vencimiento, "1", "0", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COM_ALMACEN_DE_OTRO_ESTABLECIMIENTO"));
        recepcion(ordenId, UUID.randomUUID(), "LP-012", vencimiento, "1", "0", null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COM_ALMACEN_NO_ENCONTRADO"));
        recepcion(UUID.randomUUID(), almacenId, "LP-012", vencimiento, "1", "0", null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COM_ORDEN_NO_ENCONTRADA"));
        recepcion(ordenId, almacenId, "LP-012", vencimiento, "2", "3", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM_RECEPCION_INVALIDA"));
        recepcion(ordenId, almacenId, "LP-012", vencimiento, "2", "1", null)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lineas[0].cantidadAceptada").value(1));
        mockMvc.perform(post(COM + "/recepciones").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recepcionJson(ordenId, almacenId, "LP-013", vencimiento, "1", "0")
                                .replace("\"numeroLineaOrden\":1", "\"numeroLineaOrden\":7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM_RECEPCION_LINEA_NO_ENCONTRADA"));
        mockMvc.perform(get(COM + "/recepciones/{id}", UUID.randomUUID()).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void theManualMovementEndpointRejectsThePurchaseMovementType() throws Exception {
        mockMvc.perform(post(INV + "/movimientos").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"almacenId":"%s","skuId":"%s","tipo":"INGRESO_COMPRA","cantidad":5,"motivo":"x",
                                 "numeroLote":"L-X","fechaVencimiento":"%s"}
                                """.formatted(almacenId, skuId, LocalDate.now().plusYears(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INV_TIPO_MOVIMIENTO_INVALIDO"));
    }

    @Test
    void listsTheReceptionsOfAnOrderNewestFirstWithTheirLinesAndPaginates() throws Exception {
        var proveedorId = proveedor("20100070970", "Laboratorios Peru SAC");
        var ordenId = ordenEmitida(proveedorId, "10", "0");
        var otraOrdenId = ordenEmitida(proveedorId, "10", "0");
        var vencimiento = LocalDate.now().plusYears(1);
        var primera = recepcion(ordenId, almacenId, "LP-020", vencimiento, "4", "0", null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var segunda = recepcion(ordenId, almacenId, "LP-021", vencimiento, "3", "0", null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        recepcion(otraOrdenId, almacenId, "LP-022", vencimiento, "2", "0", null).andExpect(status().isCreated());

        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", ordenId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].id").value((String) JsonPath.read(segunda, "$.id")))
                .andExpect(jsonPath("$.items[1].id").value((String) JsonPath.read(primera, "$.id")))
                .andExpect(jsonPath("$.items[0].lineas[0].numeroLote").value("LP-021"))
                .andExpect(jsonPath("$.items[0].lineas[0].loteId").isNotEmpty())
                .andExpect(jsonPath("$.items[1].lineas[0].cantidadAceptada").value(4));
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", ordenId.toString()).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value((String) JsonPath.read(primera, "$.id")));
    }

    @Test
    void listingReceptionsValidatesTheParametersAndTheirScope() throws Exception {
        var ordenId = ordenEmitida(proveedor("20100070970", "Laboratorios Peru SAC"), "10", "0");

        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", ordenId.toString()).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COM_PAGINACION_INVALIDA"));
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", bearer())
                        .param("ordenCompraId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get(COM + "/recepciones").header("Authorization", noPermissions)
                        .param("ordenCompraId", ordenId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(COM + "/recepciones").param("ordenCompraId", ordenId.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesAccessWithoutTheRequiredPermissionOrToken() throws Exception {
        mockMvc.perform(post(COM + "/recepciones").with(csrf())
                        .header("Authorization", noPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recepcionJson(UUID.randomUUID(), almacenId, "L", LocalDate.now().plusDays(5), "1", "0")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(COM + "/proveedores").header("Authorization", noPermissions))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(COM + "/ordenes")).andExpect(status().isUnauthorized());
    }

    private UUID proveedor(String ruc, String razonSocial) throws Exception {
        return created(post(COM + "/proveedores").content("""
                {"numeroDocumento":"%s","razonSocial":"%s","condicionPagoDefault":"CREDITO 30",
                 "diasCreditoDefault":30,"esLaboratorio":true}
                """.formatted(ruc, razonSocial)));
    }

    private UUID ordenBorrador(UUID proveedorId, String cantidad, String tolerancia) throws Exception {
        return created(post(COM + "/ordenes").content(orden(proveedorId, cantidad, tolerancia)));
    }

    private UUID ordenEmitida(UUID proveedorId, String cantidad, String tolerancia) throws Exception {
        var ordenId = ordenBorrador(proveedorId, cantidad, tolerancia);
        mockMvc.perform(post(COM + "/ordenes/{id}/aprobacion", ordenId).header("Authorization", bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(post(COM + "/ordenes/{id}/emision", ordenId).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EMITIDA"));
        return ordenId;
    }

    private String orden(UUID proveedorId, String cantidad, String tolerancia) {
        return """
                {"proveedorId":"%s","establecimientoDestinoId":"%s","observacion":"Reposicion",
                 "lineas":[{"skuId":"%s","cantidad":%s,"unidadMedidaCodigo":"%s","precioUnitario":5.5,
                            "descuento":0,"impuesto":9.9,"toleranciaExcesoPct":%s}]}
                """.formatted(proveedorId, establecimientoId, skuId, cantidad, UNIDAD, tolerancia);
    }

    private ResultActions recepcion(
            UUID ordenId, UUID almacen, String lote, LocalDate vencimiento, String recibida, String rechazada,
            String clave) throws Exception {
        var request = post(COM + "/recepciones").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(recepcionJson(ordenId, almacen, lote, vencimiento, recibida, rechazada));
        return mockMvc.perform(clave == null ? request : request.header("Idempotency-Key", clave));
    }

    private static String recepcionJson(
            UUID ordenId, UUID almacen, String lote, LocalDate vencimiento, String recibida, String rechazada) {
        return """
                {"ordenCompraId":"%s","almacenId":"%s","documentoProveedorSerie":"F001",
                 "documentoProveedorNumero":"123","guiaRemisionRemitente":"T001-45","temperaturaRecepcionC":22.5,
                 "items":[{"numeroLineaOrden":1,"numeroLote":"%s","fechaVencimiento":"%s",
                           "cantidadRecibida":%s,"cantidadRechazada":%s,"motivoRechazo":"Envase danado",
                           "costoUnitario":5.5}]}
                """.formatted(ordenId, almacen, lote, vencimiento, recibida, rechazada);
    }

    private static String establecimiento(UUID empresaId, String codigo, String anexo, boolean principal) {
        return """
                {"tenantId":"%s","empresaId":"%s","codigo":"%s","nombre":"Botica %s",
                 "tipoEstablecimiento":"BOTICA","codigoAnexoSunat":"%s","esPrincipal":%s,
                 "permiteVentaOnline":false,"permiteDelivery":false,"perfilOperacion":"ONLINE",
                 "zonaHoraria":"America/Lima"}
                """.formatted(TENANT_ID, empresaId, codigo, codigo, anexo, principal);
    }

    private static String almacen(UUID establecimientoId, String codigo) {
        return """
                {"tenantId":"%s","establecimientoId":"%s","codigo":"%s","nombre":"Almacen %s",
                 "tipo":"GENERAL","permiteLotes":true,"permiteVencimiento":true,"permiteVenta":true,
                 "permiteDespacho":true,"controlTemperatura":false}
                """.formatted(TENANT_ID, establecimientoId, codigo, codigo);
    }

    private UUID created(MockHttpServletRequestBuilder request) throws Exception {
        var body = mockMvc.perform(request.header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private String bearer() {
        return accessToken;
    }
}
