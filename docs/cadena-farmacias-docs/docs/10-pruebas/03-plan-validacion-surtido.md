# TST-MULTI — Plan verificable de surtido diverso

**Estado:** casos definidos; ejecución pendiente. **Fecha:** 2026-09-15.
Referencias: [RF/RN/CA](../03-requerimientos/09-surtido-multiproducto.md), [diccionario](../06-datos/10-diseno-detallado-surtido.md), [API](../07-api/08-contrato-surtido.md), [migración](../11-devops/10-migracion-surtido.md).

## 1. Fixtures sintéticos

Dos tenants T1/T2; T1 con empresa E1 y locales L1/L2; usuarios lector, operador POS, revisor y usuario fuera de ámbito. SKU A de lote con caducidad, B sin lote, C de lote sin caducidad, D serializado; dos variantes de una familia. Expedientes ficticios marcados como datos de prueba, nunca códigos sanitarios reales aparentes. Evidencia verificada, observada, vencida y revocada. Reglas PERMITIDO/PROHIBIDO/PENDIENTE con intervalos y ámbitos distintos.

Probar todos los perfiles de DAT-FAR-010 sin concluir por el nombre comercial del fixture que una clasificación real es correcta.

## 2. Matriz de pruebas

| ID | Requisito | Acción / escenario | Resultado verificable |
|---|---|---|---|
| TST-MULTI-001 | RF-CAT-019 | Crear SKU solo con nombre Vitamina, sin revisión | No clasifica ni publica automáticamente |
| TST-MULTI-002 | RF-CAT-020 | Vincular dos variantes al mismo expediente; cambiar su versión | Un expediente canónico; historia anterior preservada |
| TST-MULTI-003 | RF-CAT-020 | NSO histórica difiere del expediente | Observación y activación bloqueada |
| TST-MULTI-004 | RF-CAT-021 | Sin regla, regla pendiente o revisión vencida | PENDIENTE y POST venta rechazado |
| TST-MULTI-005 | RF-CAT-021 | PERMITIDO local y PROHIBIDO cadena | Bloqueo prevalece |
| TST-MULTI-006 | RF-CAT-021 | Instantes anterior/al inicio/al fin de vigencia | Intervalo [desde,hasta), sin autorización al fin |
| TST-MULTI-007 | RF-CAT-022 | Mismos atributos en distinto orden JSON | Rechaza variante duplicada; talla distinta se acepta |
| TST-MULTI-008 | RF-INV-023 | Recibir, reservar, vender y devolver B sin lote | Cantidades correctas, sin lote ficticio, retorno no vendible |
| TST-MULTI-009 | RF-INV-024 | Recibir C sin fecha y A sin fecha | C aceptado; A rechazado |
| TST-MULTI-010 | RF-INV-023 | Doble alta concurrente de posición con NULL | Una sola clave natural, sin duplicación de stock |
| TST-MULTI-011 | RF-INV-024 | Asignar a SKU A un lote del SKU C | FK contextual o control persistente rechaza |
| TST-MULTI-012 | RF-POS-023 | Venta combinada y reintento idéntico | Una venta; controles por línea; una salida por cantidad |
| TST-MULTI-013 | RF-POS-023 | Bloqueo y venta concurrentes, alternando orden de commit | Bloqueo confirmado primero impide venta; historia previa permanece |
| TST-MULTI-014 | RF-CAT-023 | Publicar política sobre stock incompatible | Exige plan; no reinterpreta movimientos |
| TST-MULTI-015 | RF-CAT-023 | Usar If-Match anterior o misma clave con otro cuerpo | 412 o 409 según contrato, sin escritura adicional |
| TST-MULTI-016 | RF-INV-025 | Vender la misma serie en dos cajas | Solo una confirma; conteo de series coincide con saldos |
| TST-MULTI-017 | RF-INV-025 | Cantidad fraccionaria o serie de otro tenant/SKU | Rechazo, sin alterar existencia |
| TST-MULTI-018 | RF-INV-025 | Transferir y devolver una serie vendida | Historia conserva unidad; devolución a cuarentena |
| TST-MULTI-019 | RF-CAT-021 | Usuario lector publica una regla; referencia expediente de T2 | 403/404 según contrato, sin fuga ni cambio |
| TST-MULTI-020 | RF-POS-023 | Perder conexión después de consulta PERMITIDO | No confirma offline; queda borrador, sin salida de stock |
| TST-MULTI-021 | RF-INV-023 | Reconstruir y migrar fixtures históricos | Conteos/importes/cantidades conciliados; cero datos ficticios |
| TST-MULTI-022 | RF-INV-024 | Escritura que omite flags con rol de aplicación | Control de persistencia rechaza; no basta validación de UI |
| TST-MULTI-023 | RF-CAT-020 | Eventos duplicados o versiones fuera de orden | Proyección no retrocede; ninguna evidencia duplicada |

## 3. Niveles y evidencia

Dominio: clasificación y algoritmo de decisión. Integración PostgreSQL 18: FKs, NULL, transacciones, bloqueos, series y migración. Contrato: tipos, errores, identidad e idempotencia. E2E: alta → revisión → recepción → venta → devolución, incluida desconexión.

Cada ejecución registra commit, versiones de migración, versión del motor, fixture, pasos, resultado esperado/obtenido y evidencia sanitizada. Una fila de esta matriz no acredita una prueba superada. No usar datos reales de pacientes ni documentos regulatorios inventados como válidos.

## 4. Criterio de salida

Todas las pruebas aplicables aprobadas, cero cruces de tenant/SKU/lote, cero descuadres no justificados, ejecución repetible desde cero y migración de copia histórica conciliada. El responsable de catálogo/sanitario valida la clasificación de los artículos que se habilitarán. El cierre documental es independiente de este gate de ejecución.
