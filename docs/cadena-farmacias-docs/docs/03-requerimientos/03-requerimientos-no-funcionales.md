# RNF-FAR — Requerimientos No Funcionales

**Versión:** 0.1  
**Estado:** Borrador; SLO/RPO/RTO numéricos pendientes de medición y decisión de negocio.

**Total inicial:** 57 RNF

## 1. Principio

No se inventan umbrales de milisegundos, RPO/RTO, TPS o disponibilidad sin conocer número de locales, volumen, horario, infraestructura y costo de indisponibilidad. El SRS establece capacidades verificables y deja los valores como SLO a definir mediante ADR/medición.

## 1. SEG — Seguridad y privacidad

| Código | Requisito |
|---|---|
| `RNF-SEG-001` | **Autenticación robusta.** Todo acceso no público deberá requerir autenticación mediante un mecanismo aprobado en ADR. |
| `RNF-SEG-002` | **Autorización por mínimo privilegio.** Las acciones deberán validarse por permiso y ámbito; el rol técnico no reemplaza competencias profesionales. |
| `RNF-SEG-003` | **Protección de datos personales.** El tratamiento de datos personales deberá incorporar finalidad, minimización, acceso y retención conforme al marco vigente. |
| `RNF-SEG-004` | **Protección de datos sensibles.** Recetas y farmacovigilancia deberán tener controles reforzados de acceso y auditoría. |
| `RNF-SEG-005` | **Cifrado en tránsito.** Las comunicaciones externas e internas sensibles deberán usar canales cifrados adecuados. |
| `RNF-SEG-006` | **Protección de secretos.** Contraseñas, llaves, tokens y credenciales no deberán persistirse en repositorio ni logs. |
| `RNF-SEG-007` | **MFA extensible.** La plataforma deberá permitir MFA para roles/operaciones de riesgo si la política/ADR lo exige. |
| `RNF-SEG-008` | **Segregación de funciones.** El modelo de autorización deberá soportar restricciones entre funciones incompatibles. |
| `RNF-SEG-009` | **Exportación controlada.** Exportaciones masivas deberán requerir permiso, finalidad y auditoría. |
| `RNF-SEG-010` | **Privacidad por diseño.** Nuevas capacidades de CRM/fidelización/marketing deberán evaluar base legal y datos necesarios antes de implementación. |

## 2. PER — Rendimiento

| Código | Requisito |
|---|---|
| `RNF-PER-001` | **Respuesta interactiva POS.** Operaciones de caja deberán mantener tiempos de respuesta compatibles con atención en tienda; los umbrales se fijarán mediante SLO medidos, no números inventados. |
| `RNF-PER-002` | **Búsqueda de producto.** La búsqueda por código de barras/código interno deberá ser apta para interacción de caja aun con catálogos grandes. |
| `RNF-PER-003` | **Consulta de stock.** Las consultas corporativas podrán usar proyecciones/cachés sin alterar la fuente transaccional. |
| `RNF-PER-004` | **Procesos batch separados.** Reportes regulatorios, conciliación y posting no deberán bloquear ventas. |
| `RNF-PER-005` | **Paginación.** Listados grandes deberán utilizar paginación/cursores apropiados. |

## 3. AVL — Disponibilidad y continuidad

| Código | Requisito |
|---|---|
| `RNF-AVL-001` | **Continuidad POS.** La disponibilidad objetivo del POS deberá definirse por horario de operación y criticidad. |
| `RNF-AVL-002` | **Contingencia SUNAT.** La indisponibilidad de SUNAT deberá manejarse conforme al mecanismo fiscal válido sin perder trazabilidad. |
| `RNF-AVL-003` | **Contingencia ERP.** Una caída del ERP no deberá borrar transacciones retail ya confirmadas. |
| `RNF-AVL-004` | **Offline candidato.** Si la cadena exige venta offline, deberá existir diseño explícito de catálogo/stock/precio local, sincronización, conflictos y seguridad. |
| `RNF-AVL-005` | **Backups.** RPO/RTO se definirán por clase de datos y criticidad antes de producción. |
| `RNF-AVL-006` | **Recuperación probada.** La recuperación de backups deberá probarse periódicamente. |

## 4. INT — Integridad y consistencia

| Código | Requisito |
|---|---|
| `RNF-INT-001` | **Idempotencia.** Operaciones externas reintentables deberán usar claves idempotentes. |
| `RNF-INT-002` | **Atomicidad venta.** Una venta no podrá quedar parcialmente confirmada sin estado explícito y proceso de recuperación. |
| `RNF-INT-003` | **Inventario por movimiento.** Todo cambio de existencia debe poder explicarse por movimientos/ajustes trazables. |
| `RNF-INT-004` | **No stock negativo accidental.** La concurrencia no deberá permitir doble venta/reserva que produzca stock negativo no autorizado. |
| `RNF-INT-005` | **Histórico inmutable lógico.** Registros fiscales, ventas cerradas, recetas atendidas y postings no se editarán destructivamente. |
| `RNF-INT-006` | **Versionado regulatorio.** Atributos que afecten venta/dispensación deberán conservar la versión efectiva usada en la transacción. |

## 5. SCL — Escalabilidad

| Código | Requisito |
|---|---|
| `RNF-SCL-001` | **Multisucursal.** La plataforma deberá soportar crecimiento en número de establecimientos sin duplicar configuración/reglas manualmente. |
| `RNF-SCL-002` | **Volumen transaccional.** La solución deberá escalar con ventas, líneas, movimientos y lotes preservando trazabilidad. |
| `RNF-SCL-003` | **Procesamiento asíncrono.** Integraciones/reportes podrán desacoplarse cuando sea necesario sin perder idempotencia. |
| `RNF-SCL-004` | **Particionamiento futuro.** El diseño de datos no deberá impedir particionamiento/archivo por fecha o establecimiento cuando el volumen lo justifique. |

## 6. AUD — Auditoría y trazabilidad

| Código | Requisito |
|---|---|
| `RNF-AUD-001` | **Auditoría funcional.** Operaciones críticas deben registrar actor, acción, recurso, establecimiento, fecha y resultado. |
| `RNF-AUD-002` | **Correlación.** Procesos que cruzan POS-WMS-ERP-CPE deberán compartir identificadores de correlación. |
| `RNF-AUD-003` | **Reloj consistente.** La infraestructura deberá usar sincronización temporal adecuada. |
| `RNF-AUD-004` | **No datos secretos en auditoría.** Los logs no deberán duplicar secretos ni payloads sensibles completos. |
| `RNF-AUD-005` | **Retención definida.** La retención de auditoría se definirá por obligación/finalidad, no de forma indefinida por defecto. |

## 7. OBS — Observabilidad

| Código | Requisito |
|---|---|
| `RNF-OBS-001` | **Logs estructurados.** Los componentes deberán emitir logs estructurados con correlation/trace id. |
| `RNF-OBS-002` | **Métricas.** Se medirán disponibilidad, latencia, errores, colas e integraciones críticas. |
| `RNF-OBS-003` | **Trazas.** Los flujos distribuidos deberán poder trazarse de extremo a extremo. |
| `RNF-OBS-004` | **Alertas operativas.** Fallos de CPE, ERP, sincronización, stock e integraciones críticas deberán generar alertas accionables. |
| `RNF-OBS-005` | **Separación auditoría-logs.** La auditoría de negocio no se sustituirá con logs técnicos. |

## 8. API — Interoperabilidad y APIs

| Código | Requisito |
|---|---|
| `RNF-API-001` | **Contratos versionados.** APIs e integraciones deberán tener contratos versionados. |
| `RNF-API-002` | **Errores estandarizados.** Las APIs deberán usar un esquema de errores consistente. |
| `RNF-API-003` | **Compatibilidad.** Cambios incompatibles deberán gestionarse mediante política de versión. |
| `RNF-API-004` | **OpenAPI candidato.** La API pública/interna HTTP deberá documentarse mediante OpenAPI si la arquitectura lo confirma. |
| `RNF-API-005` | **Reintentos seguros.** Clientes/adapters deberán distinguir errores reintentables de definitivos. |

## 9. MAN — Mantenibilidad y calidad

| Código | Requisito |
|---|---|
| `RNF-MAN-001` | **Módulos cohesionados.** El software deberá separar ERP, retail, inventario y farmacéutico conforme a límites de dominio. |
| `RNF-MAN-002` | **Reglas centralizadas.** Las reglas sanitarias no deberán duplicarse en POS, e-commerce y backend. |
| `RNF-MAN-003` | **Pruebas automatizadas.** RF/RN críticos deberán tener pruebas unitarias/integración/contrato según corresponda. |
| `RNF-MAN-004` | **Migraciones versionadas.** Cambios de esquema/datos deberán estar versionados y repetibles. |
| `RNF-MAN-005` | **Configuración externa.** Parámetros de negocio no secretos deberán configurarse sin recompilar cuando corresponda. |
| `RNF-MAN-006` | **Deuda explícita.** Decisiones temporales deberán registrarse como ADR/deuda técnica. |

## 10. UX — Usabilidad y accesibilidad

| Código | Requisito |
|---|---|
| `RNF-UX-001` | **POS orientado a teclado/escáner.** La operación frecuente deberá minimizar pasos y soportar escáner/teclado. |
| `RNF-UX-002` | **Mensajes accionables.** Los bloqueos sanitarios/fiscales deberán indicar motivo y acción posible sin revelar datos innecesarios. |
| `RNF-UX-003` | **Confirmación de acciones críticas.** Ajustes, anulaciones, bloqueos y devoluciones deberán tener confirmación/permiso acorde al riesgo. |
| `RNF-UX-004` | **Accesibilidad.** Los canales web administrativos deberán aplicar criterios de accesibilidad definidos por el proyecto. |
| `RNF-UX-005` | **Zona horaria/moneda.** Fechas, moneda e impuestos deberán representarse de forma consistente para Perú y extensible si cambia el alcance. |

## Criterios que requieren ADR/SLO posterior

- Disponibilidad objetivo POS y servicios centrales.
- RPO/RTO por ventas, inventario, CPE, seguridad y reportes.
- Arquitectura offline y tiempo máximo de desconexión.
- Tecnología de autenticación/autorización.
- Modelo de consistencia de stock entre central y tiendas.
- Tamaño de colas/reintentos y retención de eventos.
- Costeo y granularidad de posting ERP.
- Política de archivo/retención por tipo documental.
