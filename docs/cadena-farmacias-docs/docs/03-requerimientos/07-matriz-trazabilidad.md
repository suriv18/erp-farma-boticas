# TRA-FAR-001 — Matriz inicial de trazabilidad

## 1. Objetivo

Relacionar procesos TO-BE con módulos de requisitos, reglas y futuros casos de uso/pruebas.

| Proceso | Capacidades/RF principales | RN principales | Próximo CU |
|---|---|---|---|
| BPM-FAR-002 Abastecimiento/Recepción | RF-COM-*, RF-INV-* | RN-INV-*, RN-ERP-* | CU-COM-001 Comprar y recibir |
| BPM-FAR-003 Transferencias | RF-TRF-*, RF-INV-* | RN-TRF-*, RN-INV-* | CU-TRF-001 Transferir stock |
| BPM-FAR-004 Venta/Dispensación | RF-POS-*, RF-DSP-* | RN-POS-*, RN-DSP-*, RN-ORG-* | CU-POS-001 Vender / CU-DSP-001 Dispensar |
| BPM-FAR-005 Precios/Observatorio | RF-PRE-*, RF-OBS-* | RN-PRE-*, RN-OBS-* | CU-PRE-001 Publicar precio / CU-OBS-001 Reportar precios |
| BPM-FAR-006 Compras ERP | RF-COM-*, RF-ERP-* | RN-ERP-*, RN-INV-* | CU-COM-002 Conciliar compra |
| BPM-FAR-007 Caja/POS | RF-POS-* | RN-POS-* | CU-POS-002 Cerrar turno |
| BPM-FAR-008 Devoluciones | RF-DEV-*, RF-POS-* | RN-DEV-*, RN-POS-* | CU-DEV-001 Devolver venta |
| BPM-FAR-009 Controlados | RF-CTL-*, RF-DSP-* | RN-CTL-* | CU-CTL-001 Dispensar controlado |
| BPM-FAR-010 Recall | RF-RCL-*, RF-INV-* | RN-RCL-* | CU-RCL-001 Ejecutar recall |
| BPM-FAR-011 Farmacovigilancia | RF-FVG-* | RN-FVG-*, RN-SEC-* | CU-FVG-001 Gestionar reporte |
| BPM-FAR-012 Cierre ERP | RF-ERP-*, RF-POS-* | RN-ERP-*, RN-INT-* | CU-ERP-001 Publicar cierre |

## 2. Trazabilidad de RF críticos

| RF | Proceso | Regla/fuente principal | Prueba candidata |
|---|---|---|---|
| RF-DSP-016 | BPM-FAR-004 | RN-ORG-001 / D.S. 015-2025-SA | TST-DSP-SEC-001 técnico intenta dispensar bajo receta → denegado |
| RF-CTL-003 | BPM-FAR-009 | RN-CTL-001..006 / D.S. 023-2001-SA | TST-CTL-REC-001 receta especial vencida → no atender |
| RF-POS-009 | BPM-FAR-007 | RN-POS fiscal / SUNAT | TST-POS-CPE-001 reintento → un solo CPE |
| RF-DEV-004 | BPM-FAR-008 | SUNAT nota crédito + RN-DEV | TST-DEV-001 nota emitida no aumenta stock |
| RF-RCL-004 | BPM-FAR-010 | RN-RCL | TST-RCL-001 lote bloqueado → venta rechazada |
| RF-FVG-001 | BPM-FAR-011 | RN-FVG-001 + privacidad | TST-FVG-001 reporte sin venta → permitido |
| RF-ERP-010 | BPM-FAR-012 | RN-ERP/INT idempotencia | TST-ERP-001 doble envío → un posting |
| RF-OBS-003 | BPM-FAR-005 | RN-OBS-001 | TST-OBS-001 generar período mensual y preservar fuente |
| RF-SEC-013 / RF-SEC-014 | Seguridad transversal | RN-SEC-006 / ADR-010 | TST-SEC-NAV-001 menú visible/oculto no cambia autorización del endpoint |

## 3. Próxima extensión

La trazabilidad prioritaria ya usa `RF → RN → CU → CA`; el siguiente incremento es completar sistemáticamente `→ dominio → API → dato → prueba` para todos los módulos.
