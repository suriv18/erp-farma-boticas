# API-FAR-008 — Contrato de surtido y revisión sanitaria

**Estado:** contrato documental definido; endpoints no implementados. **Fecha:** 2026-09-15.
Aplica [ADR-011](../05-arquitectura/adr/ADR-011-surtido-trazabilidad-admisibilidad.md) y [DAT-FAR-010](../06-datos/10-diseno-detallado-surtido.md). Esta especificación complementa el estándar REST; su serialización completa en OpenAPI forma parte de la implementación.

## 1. Reglas comunes

IDs externos UUID; tenant e identidad se obtienen de la sesión. empresaId/establecimientoId solo seleccionan un ámbito al que el actor ya tiene acceso. Los catálogos devuelven códigos estables y etiquetas por separado. Timestamps RFC 3339 en UTC; fechas sin hora ISO 8601. Campo ausente y null no son equivalentes en PATCH; no se admiten atributos de escritura desconocidos.

Mutaciones requieren Idempotency-Key y, si actualizan un recurso, If-Match con la versión leída. La misma clave y mismo cuerpo devuelve la respuesta original; otro cuerpo genera 409 IDEMPOTENCY_CONFLICT. Una versión obsoleta genera 412 VERSION_CONFLICT. La publicación y la venta respetan el orden transaccional de DAT-FAR-010. Guardar el resultado idempotente junto con la mutación, no después de confirmar.

## 2. Operaciones y autorización

| Método y ruta /api/v1 | Permiso candidato explícito | Entrada | Resultado |
|---|---|---|---|
| GET /rubros-comerciales | CATALOGO:RUBRO:VER | cursor, limit (1–100) | 200 data[], nextCursor |
| POST /rubros-comerciales | CATALOGO:RUBRO:CREAR | codigo, nombre, esFarmaceutico | 201 rubro con UUID y versión |
| POST /expedientes-sanitarios | CATALOGO:EVIDENCIA:CREAR | autoridad, tipoDocumento, numeroDocumento o claveSinRegistro | 201 expediente |
| POST /expedientes-sanitarios/{id}/versiones | CATALOGO:EVIDENCIA:REVISAR | regimen, titular, fechas, fuenteUri, evidenciaUri, hash, justificacion, revisableHasta, estado | 201 versión inmutable |
| POST /skus/{id}/evidencias-sanitarias | CATALOGO:EVIDENCIA:VINCULAR | expedienteId, finalidad | 201 vínculo; incompatibilidad 409 |
| POST /skus/{id}/politicas | CATALOGO:POLITICA:PUBLICAR | regimen, exigeLote, exigeVencimiento, exigeSerie, efectivaDesde | 201 política; transición incompatible 409 |
| POST /skus/{id}/reglas-surtido | CATALOGO:SURTIDO:REVISAR | alcance, canal, resultado, motivo, fechas, evidenciaVersionId | 201 regla versionada |
| GET /skus/{id}/vendibilidad | CATALOGO:SKU:VER | establecimientoId, canal | 200 evaluación informativa |
| POST /ventas | VENTAS:VENTA:CONFIRMAR | contrato de venta y asignaciones por línea | 201 venta confirmada o error sin confirmación parcial |

Los permisos deben crearse en el catálogo RBAC e incorporarse a los roles revisados; no vienen implementados en los seeds actuales. REVISAR exige además asignación vigente del responsable autorizado para esa revisión. Un administrador de menú no obtiene este permiso. Las referencias de expediente y evidencia se verifican dentro del mismo tenant.

## 3. Tipos y campos obligatorios

ReglaSurtido: alcance.tipo enum CADENA/EMPRESA/ESTABLECIMIENTO; empresaId obligatorio para EMPRESA y ESTABLECIMIENTO; establecimientoId solo y obligatorio en ESTABLECIMIENTO. canal=POS en la activación inicial. resultado enum PERMITIDO/PROHIBIDO/PENDIENTE. motivoCodigo string de catálogo. vigenteDesde y vigenteHasta obligatorios con inicio < fin. fuenteUri obligatoria. evidenciaVersionId obligatoria cuando la decisión depende de una autorización sanitaria; en otros casos justificacion obligatoria.

Politica: tres booleanos requeridos, regimen de catálogo y efectivaDesde; no aceptar vencimiento sin lote. El servidor asigna número de versión. Si hay operaciones abiertas o existencias incompatibles, rechazar con CAT_TRANSICION_REQUIERE_PLAN; no aplicar reinterpretación masiva.

AsignacionLinea: skuId, cantidad decimal positiva, posicionId, loteId nullable y series[] de UUID. Para cada política, presencia de lote y cardinalidad de series deben corresponder a cantidad. Un lote debe pertenecer al SKU y al tenant. Una serie debe estar disponible en la posición. No permitir precio/metadata del cliente como prueba de autorización sanitaria.

## 4. Respuesta de vendibilidad

Campos: skuId, establecimientoId, canal, resultado, motivos[], politicaVersionId, reglaVersionIds[], evidenciaVersionIds[], evaluadoAt, validoHasta, requiereConexion=true.

resultado es PERMITIDO, PROHIBIDO o PENDIENTE. motivos usa códigos CAT_SURTIDO_PROHIBIDO, CAT_EVIDENCIA_PENDIENTE, CAT_EVIDENCIA_VENCIDA o CAT_SIN_POLITICA según el caso. validoHasta es el menor límite de vigencia/revisión de todos los componentes de la decisión. Una respuesta PERMITIDO no reserva stock y no funciona como autorización reutilizable para POST /ventas.

## 5. Errores

Usar application/problem+json con type, title, status, detail, instance, code y correlationId, según API-FAR-003.

| HTTP | Código / caso |
|---|---|
| 400 | VALIDATION_ERROR: formato, combinación de flags o campo desconocido |
| 401 | AUTH_REQUIRED: sesión ausente/inválida |
| 403 | AUTHZ_SCOPE_DENIED: actor sin permiso o ámbito |
| 404 | RESOURCE_NOT_FOUND: UUID no visible para el tenant |
| 409 | CAT_SURTIDO_PROHIBIDO, CAT_EVIDENCIA_PENDIENTE, CAT_VARIANTE_DUPLICADA, CAT_TRANSICION_REQUIERE_PLAN, INV_LOTE_SKU_INVALIDO, INV_SERIE_NO_DISPONIBLE, IDEMPOTENCY_CONFLICT |
| 412 | VERSION_CONFLICT: If-Match obsoleto |
| 428 | VERSION_REQUIRED: actualización sin If-Match |

La UI conserva un borrador durante desconexión; no confirma offline el nuevo surtido. Ningún error expone expedientes o identificadores de otro tenant, SQL interno o secretos.

## 6. Eventos de integración

PoliticaSkuPublicada, EvidenciaSanitariaRevisada y AdmisibilidadActualizada incluyen eventId, tenantId, aggregateId, aggregateVersion, occurredAt y correlationId, con UUID públicos y sin contenido íntegro de evidencias. Persistir Outbox en la misma transacción que el cambio. Los consumidores deduplican por eventId y descartan versiones anteriores sin sobrescribir la proyección nueva. Invalidar cachés; confirmación online consulta la fuente autoritativa.

Pruebas de contrato e integración: [TST-MULTI](../10-pruebas/03-plan-validacion-surtido.md).
