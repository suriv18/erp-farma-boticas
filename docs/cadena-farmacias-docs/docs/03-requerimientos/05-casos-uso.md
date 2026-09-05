# CU-FAR — Casos de Uso Detallados

**Versión:** 0.2  
**Estado:** Borrador funcional trazable  
**Fecha de corte:** 2026-08-30  
**Alcance:** Casos de uso prioritarios derivados de `BPM-FAR-001..013`, RF y RN vigentes.

## 1. Convenciones

- Un CU describe comportamiento de negocio, no pantallas ni tablas.
- Los pasos regulatorios remiten a `RN-*`; la fuente primaria está registrada en `docs/99-referencias.md`.
- Los flujos `ERP/DOM/CFG` no se presentan como obligación legal.
- `POS`, `Dispensación`, `Pago`, `CPE`, `Inventario` y `Posting ERP` se mantienen como conceptos separados aunque participen en una experiencia integrada.
- Los CU de seguridad no otorgan competencia profesional por el solo hecho de poseer un permiso informático.

## 2. Mapa de casos prioritarios

| CU | Caso | Núcleo |
|---|---|---|
| `CU-CAT-001` | Registrar y publicar producto/SKU farmacéutico | CAT |
| `CU-ORG-001` | Habilitar establecimiento farmacéutico y responsables | ORG |
| `CU-COM-001` | Solicitar, aprobar y emitir orden de compra | COM |
| `CU-COM-002` | Recibir compra y conciliar OC–recepción–factura | COM |
| `CU-INV-001` | Gestionar inventario, lotes y movimientos | INV |
| `CU-INV-002` | Bloquear, inmovilizar y liberar lote | INV |
| `CU-TRF-001` | Transferir inventario entre establecimientos | TRF |
| `CU-PRE-001` | Definir y publicar precio/promoción | PRE |
| `CU-POS-001` | Abrir turno y registrar venta POS | POS |
| `CU-POS-002` | Emitir y gestionar comprobante electrónico | POS |
| `CU-POS-003` | Arqueo y cierre de turno de caja | POS |
| `CU-DSP-001` | Registrar y validar prescripción | DSP |
| `CU-DSP-002` | Realizar dispensación farmacéutica | DSP |
| `CU-CTL-001` | Dispensar producto fiscalizado/controlado | CTL |
| `CU-CTL-002` | Conciliar existencias y preparar balance de controlados | CTL |
| `CU-DEV-001` | Gestionar devolución de cliente, reembolso y nota de crédito | DEV |
| `CU-RCL-001` | Ejecutar alerta sanitaria, inmovilización y recall | RCL |
| `CU-FVG-001` | Registrar y gestionar reporte de farmacovigilancia/tecnovigilancia | FVG |
| `CU-ERP-001` | Publicar cierre retail al ERP de forma idempotente | ERP |
| `CU-OBS-001` | Preparar y registrar reporte mensual de precios | OBS |
| `CU-SEC-001` | Autorizar operación por rol, ámbito y competencia profesional | SEC |
| `CU-SEC-002` | Resolver navegación dinámica de la sesión | SEC |
| `CU-AUD-001` | Consultar trazabilidad de operación crítica | AUD |

## 3. Casos de uso detallados

### CU-CAT-001 — Registrar y publicar producto/SKU farmacéutico

**Actor principal:** Administrador de catálogo farmacéutico  
**Actores secundarios:** Químico Farmacéutico / Cumplimiento regulatorio  

**RF relacionadas:** `RF-CAT-001`, `RF-CAT-002`, `RF-CAT-007`, `RF-CAT-008`, `RF-CAT-009`, `RF-CAT-013`, `RF-CAT-015`, `RF-CAT-018`  
**RN relacionadas:** `RN-CAT-001`, `RN-CAT-002`, `RN-CAT-003`, `RN-CAT-004`  
**Fuentes/soporte:** Ley 29459; D.S. 016-2011-SA; DIGEMID — Estándares/Registro Sanitario

**Disparador:** Se requiere incorporar o modificar una presentación comercial que será comprada, almacenada, dispensada o vendida por la cadena.

**Precondiciones**
- El actor posee permiso de mantenimiento del maestro.
- La categoría regulatoria del ítem ha sido identificada.

**Datos mínimos / contexto**
- Identificación del producto y SKU.
- Datos regulatorios aplicables.
- Condición de venta y clasificación fiscalizada cuando corresponda.
- Vigencia de la versión.

**Flujo principal**
1. Buscar previamente por registro sanitario, código de barras, DCI/marca y claves internas.
2. Crear o seleccionar el producto maestro.
3. Crear la presentación/SKU comercial sin duplicar la identidad regulatoria.
4. Registrar atributos regulatorios aplicables y evidencia/fuente.
5. Asignar condición de venta y clasificación fiscalizada mediante catálogos versionados.
6. Validar incompatibilidades y duplicidades.
7. Publicar una nueva versión efectiva.
8. Registrar auditoría de los atributos críticos.

**Flujos alternativos / extensiones**
- Si el producto ya existe, se crea/actualiza únicamente la presentación o versión correspondiente.
- Si una fuente externa propone un cambio, este queda pendiente de revisión antes de publicarse.

**Excepciones bloqueantes**
- Registro duplicado incompatible.
- Clasificación regulatoria/condición de venta inconsistente.
- Intento de sobrescribir una versión histórica ya utilizada.

**Postcondiciones**
- El SKU queda disponible para los procesos permitidos por su estado y condición de venta.
- Las transacciones históricas conservan la versión regulatoria aplicada.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-ORG-001 — Habilitar establecimiento farmacéutico y responsables

**Actor principal:** Administrador corporativo autorizado  
**Actores secundarios:** Director Técnico / Cumplimiento  

**RF relacionadas:** `RF-ORG-002`, `RF-ORG-003`, `RF-ORG-004`, `RF-ORG-005`, `RF-ORG-006`, `RF-ORG-010`, `RF-ORG-012`  
**RN relacionadas:** `RN-ORG-001`, `RN-ORG-002`, `RN-ORG-003`, `RN-ORG-004`  
**Fuentes/soporte:** D.S. 015-2025-SA; DIGEMID — Establecimientos Farmacéuticos

**Disparador:** Se abre, actualiza o inactiva un local de la cadena.

**Precondiciones**
- Empresa operadora existente.
- Actores y profesionales identificados.

**Datos mínimos / contexto**
- Identidad y ubicación del establecimiento.
- Autorización sanitaria/evidencia.
- Director Técnico y período.
- QF asistentes/personal técnico y acreditaciones aplicables.

**Flujo principal**
1. Registrar el establecimiento.
2. Registrar autorización sanitaria y vigencias aplicables.
3. Asignar Director Técnico con fecha de inicio.
4. Asignar QF asistentes y personal técnico.
5. Configurar horarios, almacenes y cajas.
6. Validar que la inactivación no borre historia.
7. Auditar cambios de responsables y autorizaciones.

**Flujos alternativos / extensiones**
- El cambio de Director Técnico cierra el período anterior y abre uno nuevo.

**Excepciones bloqueantes**
- Asignación con períodos incompatibles según política validada.
- Intento de eliminar historia de responsabilidad.

**Postcondiciones**
- El establecimiento queda operativo o inactivo según el estado aprobado.
- Se conserva la historia de responsables.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-COM-001 — Solicitar, aprobar y emitir orden de compra

**Actor principal:** Comprador / Abastecimiento  
**Actores secundarios:** Aprobador de compras / Proveedor  

**RF relacionadas:** `RF-COM-001`, `RF-COM-002`, `RF-COM-003`, `RF-COM-004`, `RF-COM-005`, `RF-COM-006`, `RF-COM-007`, `RF-COM-016`  
**RN relacionadas:** `RN-ERP-001`, `RN-INV-003`  
**Fuentes/soporte:** BPM-FAR-006; Benchmark ERP — Odoo/SAP/Oracle

**Disparador:** Una necesidad de abastecimiento requiere compra externa.

**Precondiciones**
- Proveedor registrado.
- Productos/SKU comprables y establecimiento/almacén destino válidos.

**Datos mínimos / contexto**
- Proveedor.
- Líneas, cantidades, precios/condiciones.
- Destino y fechas.
- Moneda/impuestos según configuración.

**Flujo principal**
1. Crear solicitud de compra.
2. Aplicar flujo de aprobación configurable.
3. Seleccionar proveedor habilitado para el alcance.
4. Generar OC con líneas y condiciones.
5. Enviar/poner a disposición la OC.
6. Si se modifica antes de recepción, crear versión y conservar cambios.
7. Auditar aprobaciones y modificaciones.

**Flujos alternativos / extensiones**
- Una solicitud rechazada vuelve a corrección o se cierra.
- Una OC puede tener entregas parciales.

**Excepciones bloqueantes**
- Proveedor inactivo/no habilitado para política aplicable.
- Solicitud no aprobada cuando el nivel de aprobación es obligatorio.

**Postcondiciones**
- Existe una OC vigente lista para despacho/recepción.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-COM-002 — Recibir compra y conciliar OC–recepción–factura

**Actor principal:** Recepción / Almacén  
**Actores secundarios:** Compras / Cuentas por pagar  

**RF relacionadas:** `RF-COM-008`, `RF-COM-009`, `RF-COM-010`, `RF-COM-011`, `RF-COM-012`, `RF-COM-013`, `RF-COM-014`, `RF-INV-001`, `RF-INV-002`, `RF-INV-003`, `RF-INV-004`, `RF-INV-005`  
**RN relacionadas:** `RN-INV-001`, `RN-INV-003`, `RN-INV-004`, `RN-INV-005`, `RN-ERP-001`, `RN-ERP-006`  
**Fuentes/soporte:** BPM-FAR-002; BPM-FAR-006; Benchmark ERP

**Disparador:** Arriba un despacho del proveedor y posteriormente se recibe la factura.

**Precondiciones**
- OC válida.
- Almacén destino habilitado.

**Datos mínimos / contexto**
- Documento proveedor.
- Lotes/vencimientos aplicables.
- Cantidades recibidas y diferencias.
- Factura del proveedor.

**Flujo principal**
1. Identificar OC y despacho.
2. Registrar recepción total/parcial por SKU y lote cuando corresponda.
3. Registrar vencimiento, origen y estado de inventario.
4. Crear movimientos de inventario; no modificar saldos sin movimiento.
5. Registrar faltantes/sobrantes/daños.
6. Registrar factura de proveedor sin crear stock adicional.
7. Ejecutar matching OC–recepción–factura según política.
8. Aprobar excepción documentada cuando corresponda.
9. Generar cuenta por pagar solo después del evento financiero aplicable.

**Flujos alternativos / extensiones**
- La recepción observada queda en cuarentena/no vendible.
- La factura puede llegar antes o después de la recepción sin alterar inventario.

**Excepciones bloqueantes**
- Lote/vencimiento obligatorio ausente.
- Intento de conciliar diferencias fuera de tolerancia sin autorización.

**Postcondiciones**
- La existencia física está trazada por recepción/movimiento.
- La obligación financiera queda separada del inventario.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-INV-001 — Gestionar inventario, lotes y movimientos

**Actor principal:** Encargado de almacén  
**Actores secundarios:** Auditor de inventario / QF cuando corresponda  

**RF relacionadas:** `RF-INV-001`, `RF-INV-002`, `RF-INV-003`, `RF-INV-004`, `RF-INV-005`, `RF-INV-006`, `RF-INV-013`, `RF-INV-018`, `RF-INV-019`, `RF-INV-020`, `RF-INV-021`  
**RN relacionadas:** `RN-INV-001`, `RN-INV-002`, `RN-INV-005`, `RN-INV-006`, `RN-INV-007`  
**Fuentes/soporte:** BPM-FAR-002; BPM-FAR-003; Benchmark WMS/retail

**Disparador:** Se produce un evento que cambia, consulta o selecciona existencias.

**Precondiciones**
- Producto/SKU y ubicación válidos.

**Datos mínimos / contexto**
- Producto/SKU.
- Lote/vencimiento cuando aplique.
- Ubicación y estado.
- Cantidad y tipo de movimiento.

**Flujo principal**
1. Registrar o resolver el lote.
2. Registrar el movimiento de entrada/salida/reserva/liberación/ajuste.
3. Actualizar proyecciones de existencia por estado.
4. Impedir que cuarentena/bloqueado/no vendible se exponga como disponible.
5. Aplicar FEFO/FIFO/manual únicamente según política configurada.
6. Exponer kardex y trazabilidad origen–destino.

**Flujos alternativos / extensiones**
- Para artículos sin lote obligatorio, la dimensión lote puede no aplicar según catálogo.

**Excepciones bloqueantes**
- Stock negativo no autorizado.
- Doble reserva concurrente.
- Movimiento sin causa/actor.

**Postcondiciones**
- Todo saldo puede explicarse por movimientos trazables.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-INV-002 — Bloquear, inmovilizar y liberar lote

**Actor principal:** Responsable autorizado de calidad/regulatorio  
**Actores secundarios:** Almacén / Tiendas / POS  

**RF relacionadas:** `RF-INV-010`, `RF-INV-011`, `RF-INV-012`, `RF-RCL-003`, `RF-RCL-004`, `RF-RCL-005`, `RF-RCL-006`, `RF-RCL-007`  
**RN relacionadas:** `RN-INV-002`, `RN-RCL-001`, `RN-RCL-002`, `RN-RCL-003`  
**Fuentes/soporte:** BPM-FAR-010; Alertas DIGEMID

**Disparador:** Se recibe una alerta, decisión interna o incidencia que exige inmovilización.

**Precondiciones**
- Lote identificado.
- Actor con competencia/permiso.

**Datos mínimos / contexto**
- Lote.
- Motivo/fuente.
- Alcance.
- Fecha y evidencia.

**Flujo principal**
1. Registrar motivo y fuente.
2. Identificar todas las existencias y reservas afectadas.
3. Cambiar la disponibilidad a bloqueada/no vendible sin eliminar stock físico.
4. Propagar el bloqueo a POS, e-commerce y reservas.
5. Generar tareas de inmovilización por establecimiento.
6. Confirmar físicamente cantidades inmovilizadas.
7. Liberar solo mediante decisión autorizada y auditable.

**Flujos alternativos / extensiones**
- El alcance puede ser un lote, producto o establecimientos específicos según la alerta.

**Excepciones bloqueantes**
- Venta/reserva nueva del lote bloqueado.
- Liberación por actor no autorizado.

**Postcondiciones**
- El lote queda inmovilizado o liberado con historia completa.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-TRF-001 — Transferir inventario entre establecimientos

**Actor principal:** Responsable de abastecimiento/almacén origen  
**Actores secundarios:** Almacén destino / Aprobador  

**RF relacionadas:** `RF-TRF-001`, `RF-TRF-002`, `RF-TRF-003`, `RF-TRF-004`, `RF-TRF-005`, `RF-TRF-006`, `RF-TRF-007`, `RF-TRF-008`, `RF-TRF-009`, `RF-TRF-010`  
**RN relacionadas:** `RN-TRF-001`, `RN-TRF-002`, `RN-TRF-003`, `RN-TRF-004`, `RN-INV-006`  
**Fuentes/soporte:** BPM-FAR-003; Oracle Retail — Transfers

**Disparador:** Un local/centro necesita mover stock a otro destino de la cadena.

**Precondiciones**
- Origen/destino activos.
- Stock disponible y transferible.

**Datos mínimos / contexto**
- Origen/destino.
- SKU/lote/cantidad.
- Documento de traslado cuando aplique.

**Flujo principal**
1. Crear solicitud.
2. Aprobar según política.
3. Reservar stock en origen.
4. Realizar picking por lote.
5. Despachar: descontar disponible origen y crear tránsito.
6. Registrar documento de traslado.
7. Destino confirma recepción.
8. Convertir lo recibido a existencia destino.
9. Registrar diferencias e incidencia.
10. Cerrar solo cuando las diferencias estén resueltas.

**Flujos alternativos / extensiones**
- Recepción parcial conserva saldo en tránsito/incidencia.

**Excepciones bloqueantes**
- Transferir lote bloqueado como operación ordinaria.
- Aumentar disponible destino antes de recepción.

**Postcondiciones**
- La transferencia conserva trazabilidad origen–tránsito–destino.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-PRE-001 — Definir y publicar precio/promoción

**Actor principal:** Pricing / Comercial autorizado  
**Actores secundarios:** Aprobador / Cumplimiento  

**RF relacionadas:** `RF-PRE-001`, `RF-PRE-002`, `RF-PRE-003`, `RF-PRE-004`, `RF-PRE-005`, `RF-PRE-006`, `RF-PRE-007`, `RF-PRE-008`, `RF-PRE-009`, `RF-PRE-014`  
**RN relacionadas:** `RN-PRE-001`, `RN-PRE-002`, `RN-PRE-003`  
**Fuentes/soporte:** BPM-FAR-005; Benchmark retail

**Disparador:** Se crea o modifica una política de precio/promoción.

**Precondiciones**
- SKU activo.
- Ámbito/canal válido.

**Datos mínimos / contexto**
- Precio base/efectivo.
- Ámbito y vigencia.
- Promoción/descuento y condiciones.

**Flujo principal**
1. Crear precio o promoción en borrador.
2. Definir ámbito, canal y vigencia.
3. Validar solapamientos/incompatibilidades.
4. Aprobar según política.
5. Publicar versión efectiva.
6. POS resuelve precio vigente y conserva evidencia.
7. Descuento manual requiere permiso/motivo cuando la política lo exige.

**Flujos alternativos / extensiones**
- Promociones incompatibles se resuelven según reglas configuradas.

**Excepciones bloqueantes**
- Promoción que intente omitir receta o bloqueo sanitario.
- Precio no aprobado cuando la aprobación sea obligatoria.

**Postcondiciones**
- Existe una versión de precio/promoción reproducible por fecha/ámbito.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-POS-001 — Abrir turno y registrar venta POS

**Actor principal:** Cajero/Vendedor autorizado  
**Actores secundarios:** Cliente / Módulo de dispensación  

**RF relacionadas:** `RF-POS-001`, `RF-POS-002`, `RF-POS-003`, `RF-POS-004`, `RF-POS-005`, `RF-POS-006`, `RF-POS-007`, `RF-POS-008`, `RF-POS-014`, `RF-POS-019`, `RF-POS-022`  
**RN relacionadas:** `RN-POS-001`, `RN-POS-002`, `RN-PRE-001`, `RN-PRE-002`, `RN-INV-002`, `RN-DSP-001`  
**Fuentes/soporte:** BPM-FAR-004; BPM-FAR-007; Benchmark POS

**Disparador:** Un cliente solicita adquirir uno o más productos.

**Precondiciones**
- Establecimiento, terminal y turno activos.
- Operador autenticado/autorizado.

**Datos mínimos / contexto**
- Productos/SKU.
- Cliente cuando corresponda.
- Medios de pago.
- Dispensación vinculada cuando sea exigible.

**Flujo principal**
1. Crear venta identificando local, caja, turno y operador.
2. Agregar productos por escaneo/búsqueda.
3. Resolver condición de venta, stock vendible y precio vigente.
4. Si requiere receta, derivar/exigir dispensación válida.
5. Reservar/seleccionar stock evitando doble asignación.
6. Registrar uno o varios medios de pago.
7. Confirmar venta de forma idempotente.
8. Generar movimientos de inventario asociados.
9. Derivar a emisión CPE.

**Flujos alternativos / extensiones**
- La venta puede cancelarse antes de su cierre siguiendo las reglas del estado.
- Pago mixto conserva el detalle por medio.

**Excepciones bloqueantes**
- Lote bloqueado/no vendible.
- Stock insuficiente.
- Producto bajo receta sin dispensación válida.

**Postcondiciones**
- La venta queda cerrada o cancelada con trazabilidad y movimientos consistentes.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-POS-002 — Emitir y gestionar comprobante electrónico

**Actor principal:** Sistema POS / Servicio CPE  
**Actores secundarios:** SUNAT/PSE/OSE según mecanismo  

**RF relacionadas:** `RF-POS-009`, `RF-POS-010`, `RF-POS-011`, `RF-POS-012`, `RF-POS-013`, `RF-INT-001`, `RF-INT-008`, `RF-INT-009`  
**RN relacionadas:** `RN-POS-002`, `RN-POS-003`, `RN-POS-004`, `RN-POS-005`, `RN-POS-006`, `RN-POS-007`, `RN-INT-001`, `RN-INT-002`  
**Fuentes/soporte:** SUNAT — CPE / SEE del contribuyente

**Disparador:** Una venta u operación fiscal requiere emitir boleta/factura/nota.

**Precondiciones**
- Venta u operación fiscal elegible.
- Configuración fiscal vigente.

**Datos mínimos / contexto**
- Tipo CPE.
- Serie/correlativo.
- Adquirente cuando corresponda.
- Importes/impuestos.
- Clave idempotente.

**Flujo principal**
1. Determinar tipo de CPE.
2. Asignar serie/correlativo conforme a configuración.
3. Generar payload/archivo fiscal.
4. Transmitir o encolar según mecanismo/contingencia.
5. Registrar CDR/estado/respuesta.
6. Ante reintento usar la misma intención idempotente.
7. Poner representación/consulta a disposición según política.

**Flujos alternativos / extensiones**
- Un fallo de red deja el CPE pendiente sin deshacer una venta válida por una regla no definida.

**Excepciones bloqueantes**
- Crear segundo CPE para la misma intención por un reintento técnico.
- Emitir factura/boleta con datos incompatibles con el tipo.

**Postcondiciones**
- Venta y CPE conservan estados relacionados pero separados.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-POS-003 — Arqueo y cierre de turno de caja

**Actor principal:** Cajero / Supervisor  
**Actores secundarios:** ERP / Tesorería  

**RF relacionadas:** `RF-POS-015`, `RF-POS-016`, `RF-POS-017`, `RF-POS-018`, `RF-ERP-001`, `RF-ERP-003`, `RF-ERP-014`  
**RN relacionadas:** `RN-POS-008`, `RN-POS-009`, `RN-ERP-003`, `RN-ERP-005`  
**Fuentes/soporte:** BPM-FAR-007; BPM-FAR-012; Benchmark retail/ERP

**Disparador:** Finaliza el turno o se requiere cierre controlado.

**Precondiciones**
- Turno abierto.

**Datos mínimos / contexto**
- Efectivo contado.
- Movimientos de caja.
- Medios de pago conciliables.

**Flujo principal**
1. Detener nuevas ventas en el turno según transición de estado.
2. Calcular ventas, ingresos/retiros y medios de pago.
3. Registrar arqueo físico.
4. Calcular diferencias.
5. Exigir motivo/aprobación según umbral.
6. Cerrar el turno sin borrar CPE/postings pendientes.
7. Generar resumen financiero para integración ERP.

**Flujos alternativos / extensiones**
- Una diferencia puede quedar en investigación según política.

**Excepciones bloqueantes**
- Compensar silenciosamente faltantes/sobrantes.
- Confundir cierre de caja con cierre contable mensual.

**Postcondiciones**
- Turno cerrado con resumen y pendientes explícitos.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-DSP-001 — Registrar y validar prescripción

**Actor principal:** Químico Farmacéutico / personal autorizado para recepción  
**Actores secundarios:** Paciente/cliente / Prescriptor  

**RF relacionadas:** `RF-DSP-001`, `RF-DSP-002`, `RF-DSP-003`, `RF-DSP-004`, `RF-DSP-005`, `RF-DSP-006`, `RF-DSP-007`, `RF-DSP-008`, `RF-DSP-019`, `RF-DSP-022`  
**RN relacionadas:** `RN-DSP-001`, `RN-DSP-002`, `RN-DSP-005`, `RN-DSP-006`, `RN-SEC-001`, `RN-SEC-004`  
**Fuentes/soporte:** R.M. 013-2009/MINSA — Manual de Buenas Prácticas de Dispensación; Normativa aplicable según tipo de receta

**Disparador:** Se presenta una receta o la condición de venta obliga a validarla.

**Precondiciones**
- Producto/condición de venta identificables.
- Actor con permiso para la etapa que realiza.

**Datos mínimos / contexto**
- Paciente.
- Prescriptor.
- Fecha y soporte.
- Medicamentos, concentración, forma y pauta cuando corresponda.

**Flujo principal**
1. Registrar la receta sin marcarla atendida.
2. Capturar paciente, prescriptor y líneas.
3. Determinar tipo de receta/regla aplicable.
4. Validar integridad, vigencia y condiciones bloqueantes.
5. Asignar QF responsable cuando corresponda.
6. Registrar rechazo y motivo si no es atendible.
7. Proteger el documento y datos sensibles.

**Flujos alternativos / extensiones**
- La información faltante puede originar consulta/observación al prescriptor según el proceso farmacéutico.

**Excepciones bloqueantes**
- Receta vencida según regla aplicable.
- Datos mínimos faltantes que impidan validación.

**Postcondiciones**
- La receta queda validable/validada/rechazada según el workflow, nunca “atendida” solo por registrarse.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-DSP-002 — Realizar dispensación farmacéutica

**Actor principal:** Químico Farmacéutico competente  
**Actores secundarios:** Paciente/cliente / POS  

**RF relacionadas:** `RF-DSP-009`, `RF-DSP-010`, `RF-DSP-011`, `RF-DSP-012`, `RF-DSP-013`, `RF-DSP-014`, `RF-DSP-015`, `RF-DSP-016`, `RF-DSP-017`, `RF-DSP-018`, `RF-DSP-020`, `RF-DSP-021`  
**RN relacionadas:** `RN-ORG-001`, `RN-DSP-001`, `RN-DSP-003`, `RN-DSP-004`, `RN-DSP-007`, `RN-SEC-005`  
**Fuentes/soporte:** R.M. 013-2009/MINSA; D.S. 015-2025-SA

**Disparador:** Existe una receta validable y el paciente solicita atención.

**Precondiciones**
- Receta registrada/validada según tipo.
- QF autorizado para el establecimiento/acto.

**Datos mínimos / contexto**
- Receta.
- Productos/lotes seleccionables.
- Cantidad a dispensar.

**Flujo principal**
1. Analizar e interpretar la prescripción.
2. Registrar observación/consulta cuando sea necesaria.
3. Seleccionar producto y lote vendible compatibles con la prescripción.
4. Registrar dispensación total o parcial solo si la regla lo permite.
5. Registrar entrega e información al paciente.
6. Conservar evidencia de atención/retención cuando corresponda.
7. Vincular la dispensación al POS sin fusionar ambos conceptos.

**Flujos alternativos / extensiones**
- Una dispensación parcial conserva saldo e histórico solo en supuestos permitidos.

**Excepciones bloqueantes**
- Personal técnico intenta confirmar dispensación bajo receta.
- Sustitución/alternativa automática sin decisión profesional/regla aplicable.
- Lote bloqueado/no vendible.

**Postcondiciones**
- La dispensación queda cerrada con QF, receta, cantidades, productos/lotes y evidencia trazables.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-CTL-001 — Dispensar producto fiscalizado/controlado

**Actor principal:** Químico Farmacéutico responsable  
**Actores secundarios:** Adquirente / Cumplimiento regulatorio  

**RF relacionadas:** `RF-CTL-001`, `RF-CTL-002`, `RF-CTL-003`, `RF-CTL-004`, `RF-CTL-005`, `RF-CTL-006`, `RF-CTL-007`, `RF-CTL-008`, `RF-CTL-011`, `RF-CTL-018`  
**RN relacionadas:** `RN-CAT-003`, `RN-DSP-006`, `RN-CTL-001`, `RN-CTL-002`, `RN-CTL-003`, `RN-CTL-004`, `RN-CTL-005`, `RN-CTL-006`  
**Fuentes/soporte:** D.S. 023-2001-SA

**Disparador:** Se intenta dispensar un producto clasificado como fiscalizado/controlado.

**Precondiciones**
- Clasificación/lista normativa vigente identificada.
- QF competente.

**Datos mínimos / contexto**
- Producto/lista.
- Receta exigible.
- Fecha de expedición.
- Adquirente y cantidad.

**Flujo principal**
1. Resolver lista y tipo de receta.
2. Validar receta especial cuando corresponda.
3. Controlar vigencia específica de la receta; para listas aplicables, tres días según D.S. 023-2001-SA.
4. Rechazar receta con enmendaduras o sospecha/evidencia de adulteración/falsificación según regla aplicable.
5. Registrar adquirente y cantidad.
6. Registrar dispensación parcial únicamente si está permitida.
7. Retener/archivar evidencia cuando corresponda.
8. Actualizar registro de existencias controladas.
9. Auditar el acceso y movimiento.

**Flujos alternativos / extensiones**
- Una receta sospechosa sigue el flujo de incidencia/reporte correspondiente.

**Excepciones bloqueantes**
- Receta vencida.
- Receta adulterada/falsificada o con condiciones bloqueantes.
- Actor no competente.

**Postcondiciones**
- Movimiento controlado y evidencia quedan conciliables con existencias.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-CTL-002 — Conciliar existencias y preparar balance de controlados

**Actor principal:** Director Técnico / QF responsable  
**Actores secundarios:** Cumplimiento / Autoridad según mecanismo  

**RF relacionadas:** `RF-CTL-010`, `RF-CTL-011`, `RF-CTL-012`, `RF-CTL-013`, `RF-CTL-014`, `RF-CTL-015`, `RF-CTL-016`, `RF-CTL-017`  
**RN relacionadas:** `RN-CTL-007`, `RN-CTL-008`, `RN-CTL-009`, `RN-CTL-010`, `RN-CTL-011`, `RN-CTL-012`, `RN-CTL-013`  
**Fuentes/soporte:** D.S. 023-2001-SA; DIGEMID — Psicotrópicos y Estupefacientes

**Disparador:** Cierre periódico o incidencia regulatoria de productos fiscalizados.

**Precondiciones**
- Establecimiento/sujeto y listas aplicables configurados con evidencia.

**Datos mínimos / contexto**
- Saldo inicial.
- Entradas/salidas.
- Saldo físico/final.
- Incidencias.

**Flujo principal**
1. Consolidar movimientos del período por lista/producto.
2. Conciliar registro vs existencia física.
3. Investigar/registrar diferencias.
4. Registrar robo/sustracción/siniestro con flujo regulatorio cuando corresponda.
5. Preparar balance solo para sujetos/listas obligados.
6. Registrar presentación y evidencia.
7. Restringir promociones prohibidas por la norma aplicable.

**Flujos alternativos / extensiones**
- Una diferencia impide cierre o exige justificación/aprobación según regla aplicable.

**Excepciones bloqueantes**
- Generar balance como obligación universal sin validar sujeto/lista.

**Postcondiciones**
- El período y la evidencia quedan auditables y reproducibles.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-DEV-001 — Gestionar devolución de cliente, reembolso y nota de crédito

**Actor principal:** Cajero/Supervisor de devoluciones  
**Actores secundarios:** SUNAT / Inventario / QF-Calidad cuando corresponda  

**RF relacionadas:** `RF-DEV-001`, `RF-DEV-002`, `RF-DEV-003`, `RF-DEV-004`, `RF-DEV-005`, `RF-DEV-006`, `RF-DEV-007`, `RF-DEV-008`, `RF-DEV-009`, `RF-DEV-010`, `RF-DEV-012`, `RF-DEV-013`  
**RN relacionadas:** `RN-POS-005`, `RN-DEV-001`, `RN-DEV-002`, `RN-DEV-003`, `RN-DEV-004`  
**Fuentes/soporte:** SUNAT — Nota de Crédito Electrónica; BPM-FAR-008

**Disparador:** El cliente solicita devolución/anulación/regularización de una venta previa.

**Precondiciones**
- Venta/comprobante previo localizable.
- Política comercial aplicable.

**Datos mínimos / contexto**
- Venta/CPE original.
- Líneas/cantidades.
- Motivo.
- Producto físico recibido cuando aplique.

**Flujo principal**
1. Registrar solicitud vinculada a la venta original.
2. Validar elegibilidad comercial.
3. Autorizar según política.
4. Emitir nota de crédito cuando corresponda, vinculada al CPE previo.
5. Registrar estado fiscal de la nota.
6. Registrar reembolso por separado.
7. Si se recibe producto físico, enviarlo a estado no vendible/evaluación.
8. Determinar disposición.
9. Reingresar a stock vendible únicamente mediante autorización/regla aplicable.
10. Auditar todo el proceso.

**Flujos alternativos / extensiones**
- Puede existir devolución parcial.
- Puede existir ajuste fiscal sin retorno físico y retorno físico sin reingreso vendible.

**Excepciones bloqueantes**
- Modificar destructivamente la venta original.
- Reingreso automático por la sola emisión de nota de crédito.

**Postcondiciones**
- La venta original permanece histórica; devolución, nota, reembolso y disposición quedan vinculados.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-RCL-001 — Ejecutar alerta sanitaria, inmovilización y recall

**Actor principal:** Responsable regulatorio / Director Técnico  
**Actores secundarios:** Tiendas / Almacén / POS / E-commerce  

**RF relacionadas:** `RF-RCL-001`, `RF-RCL-002`, `RF-RCL-003`, `RF-RCL-004`, `RF-RCL-005`, `RF-RCL-006`, `RF-RCL-007`, `RF-RCL-008`, `RF-RCL-009`, `RF-RCL-010`, `RF-RCL-011`, `RF-RCL-012`  
**RN relacionadas:** `RN-RCL-001`, `RN-RCL-002`, `RN-RCL-003`, `RN-RCL-004`, `RN-RCL-005`  
**Fuentes/soporte:** DIGEMID — Alertas sanitarias; BPM-FAR-010

**Disparador:** Se recibe una alerta/retiro o decisión interna que afecta producto/lote.

**Precondiciones**
- Alerta y alcance identificados.

**Datos mínimos / contexto**
- Fuente de alerta.
- Producto/lote/serie.
- Acción requerida.
- Alcance temporal/geográfico.

**Flujo principal**
1. Registrar la alerta sin sobrescribir la fuente.
2. Clasificar acción.
3. Localizar stock, tránsito, reservas y ventas históricas.
4. Bloquear venta y propagar a canales.
5. Generar tareas de inmovilización por local.
6. Confirmar cantidades físicas.
7. Gestionar devolución/destrucción/destino autorizado.
8. Conciliar cantidad afectada, localizada, no localizada y destino final.
9. Cerrar recall con aprobación/evidencia.

**Flujos alternativos / extensiones**
- La alerta puede ampliarse/reducirse mediante nueva versión sin borrar la anterior.

**Excepciones bloqueantes**
- Cerrar recall sin conciliación.
- Permitir venta nueva del lote afectado.

**Postcondiciones**
- El recall es reproducible por lote, local, cantidad y acción.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-FVG-001 — Registrar y gestionar reporte de farmacovigilancia/tecnovigilancia

**Actor principal:** Químico Farmacéutico / Responsable de farmacovigilancia  
**Actores secundarios:** Reportante / DIGEMID-CENAFyT  

**RF relacionadas:** `RF-FVG-001`, `RF-FVG-002`, `RF-FVG-003`, `RF-FVG-004`, `RF-FVG-005`, `RF-FVG-006`, `RF-FVG-007`, `RF-FVG-008`, `RF-FVG-009`, `RF-FVG-010`, `RF-FVG-011`, `RF-FVG-012`, `RF-FVG-013`, `RF-FVG-014`  
**RN relacionadas:** `RN-FVG-001`, `RN-FVG-002`, `RN-FVG-003`, `RN-FVG-004`, `RN-FVG-005`, `RN-SEC-001`, `RN-SEC-004`  
**Fuentes/soporte:** DIGEMID — NotiMED/NotiVAC; DIGEMID — formatos de farmacovigilancia/tecnovigilancia

**Disparador:** Un profesional, paciente u otra fuente comunica una sospecha de reacción/evento.

**Precondiciones**
- Canal de reporte habilitado.

**Datos mínimos / contexto**
- Producto sospechoso.
- Paciente protegido.
- Reportante.
- Descripción del evento.
- Gravedad/prioridad cuando pueda determinarse.

**Flujo principal**
1. Registrar el reporte aunque no exista venta POS de la cadena.
2. Clasificar producto/evento.
3. Registrar datos mínimos disponibles y proteger identidad.
4. Asignar responsable.
5. Solicitar información faltante sin borrar lo recibido originalmente.
6. Resolver plazo aplicable según sujeto/gravedad/norma vigente.
7. Registrar notificación oficial/canal/identificador cuando se realice.
8. Actualizar mediante versiones/complementos.
9. Cerrar con motivo y auditoría.

**Flujos alternativos / extensiones**
- Un reporte incompleto puede permanecer abierto y recibir follow-up.

**Excepciones bloqueantes**
- Negar el reporte solo porque no existe ticket de venta.
- Exponer datos de salud a perfiles comerciales.

**Postcondiciones**
- Existe un expediente de reporte trazable y restringido.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-ERP-001 — Publicar cierre retail al ERP de forma idempotente

**Actor principal:** Proceso de integración ERP  
**Actores secundarios:** Finanzas/Tesorería / ERP destino  

**RF relacionadas:** `RF-ERP-001`, `RF-ERP-002`, `RF-ERP-003`, `RF-ERP-007`, `RF-ERP-008`, `RF-ERP-009`, `RF-ERP-010`, `RF-ERP-011`, `RF-ERP-012`, `RF-ERP-013`, `RF-ERP-014`, `RF-ERP-015`, `RF-ERP-018`, `RF-INT-002`, `RF-INT-008`, `RF-INT-009`  
**RN relacionadas:** `RN-ERP-002`, `RN-ERP-003`, `RN-ERP-004`, `RN-ERP-005`, `RN-INT-001`, `RN-INT-002`  
**Fuentes/soporte:** BPM-FAR-012; Benchmark ERP

**Disparador:** Un turno/día/lote contable dispone de transacciones listas para integración.

**Precondiciones**
- Transacciones operativas consolidadas y validadas.

**Datos mínimos / contexto**
- Ventas.
- Devoluciones.
- Medios de pago.
- Movimientos de inventario/costo.
- Clave idempotente.

**Flujo principal**
1. Generar resumen financiero de la fuente.
2. Validar consistencia.
3. Conciliar medios de pago.
4. Construir postings según reglas contables del ERP.
5. Asignar clave idempotente.
6. Enviar al ERP.
7. Registrar aceptación/rechazo e identificador destino.
8. Reintentar errores reintentables sin duplicar.
9. Resolver correcciones con reversa/ajuste, no edición destructiva.

**Flujos alternativos / extensiones**
- Un posting rechazado permanece pendiente mientras la venta sigue en su estado retail histórico.

**Excepciones bloqueantes**
- Dos postings válidos para la misma clave idempotente.
- Tratar cierre de tienda como cierre contable mensual.

**Postcondiciones**
- Cada posting puede reconciliarse con transacciones fuente y respuesta ERP.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-OBS-001 — Preparar y registrar reporte mensual de precios

**Actor principal:** Cumplimiento / Pricing  
**Actores secundarios:** DIGEMID — Observatorio  

**RF relacionadas:** `RF-PRE-010`, `RF-PRE-011`, `RF-PRE-012`, `RF-PRE-013`, `RF-OBS-001`, `RF-OBS-002`, `RF-OBS-003`, `RF-OBS-004`, `RF-OBS-005`, `RF-OBS-006`, `RF-OBS-007`, `RF-OBS-008`  
**RN relacionadas:** `RN-PRE-004`, `RN-OBS-001`, `RN-OBS-002`, `RN-OBS-003`  
**Fuentes/soporte:** DIGEMID — Observatorio/Indicador de precios; R.M. 040-2010/MINSA y fuente vigente del mecanismo

**Disparador:** Corresponde preparar el reporte periódico de precios de establecimientos obligados.

**Precondiciones**
- Sujeto/establecimientos reportables determinados.
- Precios fuente disponibles.

**Datos mínimos / contexto**
- Período.
- Establecimientos.
- Productos reportables.
- Precio/versión fuente.

**Flujo principal**
1. Determinar establecimientos obligados.
2. Seleccionar productos/reportes aplicables.
3. Consolidar precio desde una fuente/version reproducible.
4. Validar campos y consistencia.
5. Generar archivo/carga según mecanismo vigente una vez confirmado.
6. Registrar aprobación/presentación/evidencia.
7. Reprocesar observaciones sin borrar envíos anteriores.

**Flujos alternativos / extensiones**
- El mecanismo técnico permanece configurable mientras la fuente oficial no justifique API/archivo específico.

**Excepciones bloqueantes**
- Inferir obligatoriedad por configuración genérica sin sujeto aplicable.

**Postcondiciones**
- El cumplimiento mensual puede demostrarse por período, fuente y evidencia.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-SEC-001 — Autorizar operación por rol, ámbito y competencia profesional

**Actor principal:** Servicio de autorización  
**Actores secundarios:** Usuario de negocio / Auditoría  

**RF relacionadas:** `RF-SEC-002`, `RF-SEC-003`, `RF-SEC-004`, `RF-SEC-005`, `RF-SEC-006`, `RF-SEC-007`, `RF-SEC-010`, `RF-SEC-011`, `RF-AUD-001`, `RF-AUD-003`  
**RN relacionadas:** `RN-ORG-001`, `RN-SEC-001`, `RN-SEC-004`, `RN-SEC-005`, `RN-AUD-001`  
**Fuentes/soporte:** D.S. 015-2025-SA; Ley 29733 + Reglamento; Política de seguridad

**Disparador:** Un usuario solicita una operación sensible o farmacéutica.

**Precondiciones**
- Usuario autenticado.
- Contexto organizacional disponible.

**Datos mínimos / contexto**
- Usuario.
- Roles/permisos.
- Establecimiento/ámbito.
- Tipo de operación y recurso.

**Flujo principal**
1. Resolver identidad y sesión.
2. Resolver roles/permisos vigentes.
3. Validar ámbito del establecimiento/recurso.
4. Aplicar restricciones de competencia profesional.
5. Aplicar segregación de funciones cuando esté definida.
6. Permitir o denegar.
7. Auditar accesos sensibles y denegaciones.

**Flujos alternativos / extensiones**
- Un administrador técnico puede administrar usuarios sin adquirir competencia farmacéutica.

**Excepciones bloqueantes**
- Permiso informático usado para sobrepasar una prohibición profesional.
- Acceso comercial a receta/FVG sin finalidad/permiso.

**Postcondiciones**
- La decisión de autorización queda trazable.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

### CU-AUD-001 — Consultar trazabilidad de operación crítica

**Actor principal:** Auditor autorizado  
**Actores secundarios:** Cumplimiento / Seguridad  

**RF relacionadas:** `RF-AUD-001`, `RF-AUD-002`, `RF-AUD-003`, `RF-AUD-004`, `RF-AUD-005`, `RF-AUD-006`, `RF-AUD-007`, `RF-AUD-008`  
**RN relacionadas:** `RN-AUD-001`, `RN-AUD-002`, `RN-AUD-003`  
**Fuentes/soporte:** Política de auditoría; Ley 29733 cuando involucra datos personales

**Disparador:** Se requiere investigar o demostrar una operación crítica.

**Precondiciones**
- Actor con permiso de auditoría y ámbito válido.

**Datos mínimos / contexto**
- Período.
- Actor/recurso/correlación.
- Tipo de evento.

**Flujo principal**
1. Buscar eventos por filtros autorizados.
2. Correlacionar operación entre módulos/integraciones.
3. Mostrar actor, local, recurso, tiempo, resultado y referencias sin exponer secretos.
4. Permitir exportación solo con autorización y auditoría.

**Flujos alternativos / extensiones**
- Datos sensibles se minimizan/mascaran según finalidad.

**Excepciones bloqueantes**
- Edición/borrado ordinario de la auditoría.
- Exposición de contraseñas/tokens/secretos.

**Postcondiciones**
- La evidencia consultada conserva integridad y trazabilidad.

**Seguridad y auditoría**
- Toda operación sensible conserva actor, establecimiento/ámbito, fecha/hora, resultado y correlación cuando aplique.
- Los datos personales, recetas, farmacovigilancia y productos controlados se muestran únicamente según finalidad, rol, ámbito y competencia.

**Criterios de aceptación:** ver `08-criterios-aceptacion.md` para los RF asociados.

## 4. Escenarios end-to-end que deberán probarse

### E2E-FAR-001 — Compra a venta

`Producto publicado → OC → recepción → lote/stock → precio → venta POS → CPE → movimiento de inventario → posting ERP`.

### E2E-FAR-002 — Venta bajo receta

`Producto con receta → receta registrada → validación QF → dispensación → lote vendible → POS → pago → CPE`.

### E2E-FAR-003 — Producto controlado

`Clasificación fiscalizada → receta especial → validación por regla vigente → dispensación QF → registro controlado → conciliación/balance`.

### E2E-FAR-004 — Devolución

`Venta histórica → solicitud → autorización → nota de crédito/reembolso → producto físico no vendible → evaluación → disposición/reingreso autorizado`.

### E2E-FAR-005 — Recall

`Alerta → lote afectado → bloqueo omnicanal → inmovilización por local → trazabilidad histórica → destino → conciliación → cierre`.

### E2E-FAR-006 — Farmacovigilancia sin ticket

`Reporte externo → registro → protección de datos → evaluación/follow-up → notificación oficial → cierre`, sin exigir una venta POS previa.

### E2E-FAR-007 — Cierre financiero

`Turno POS → arqueo → cierre tienda → resumen → posting idempotente ERP → aceptación/rechazo → reintento/reversa`.

## 5. Gobierno

Los CU se actualizarán cuando una nueva fuente oficial modifique una regla `NORM/FISCAL`. Cambios de política interna (`CFG/ERP`) no deben reescribir retrospectivamente transacciones cerradas.


### CU-SEC-002 — Resolver navegación dinámica de la sesión

**Actor principal:** Usuario autenticado  
**RF:** RF-SEC-013, RF-SEC-014  
**RN:** RN-SEC-006

**Precondiciones:** sesión/identidad válida y contexto tenant resuelto.

**Flujo principal:**
1. El cliente solicita navegación para una aplicación permitida.
2. El backend obtiene el usuario desde la sesión/token validado.
3. Resuelve roles, permisos y ámbitos vigentes.
4. Selecciona menús activos/visibles.
5. Aplica `AUTENTICADO`, `CUALQUIERA` o `TODOS`.
6. Elimina grupos sin hijos y ordena el árbol.
7. Retorna códigos/rutas/iconos semánticos sin PK internas.

**Alternos/bloqueos:** aplicación no permitida; sesión inválida; contexto no vigente.

**Postcondición:** el usuario recibe un árbol de navegación coherente; ningún permiso adicional se concede por esta operación.
