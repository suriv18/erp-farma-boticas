# RF-FAR — Requerimientos Funcionales

**Versión:** 0.1  
**Estado:** Borrador funcional trazable  
**Total inicial:** 260 RF

## 1. Convenciones

Los códigos usan `RF-<MÓDULO>-NNN`. La prioridad es inicial y deberá validarse con la cadena. Los requisitos regulatorios críticos se detallan después del catálogo.

## 2. Resumen por módulo

| Módulo | Nombre | RF |
|---|---|---:|
| `ORG` | Organización y establecimientos | 12 |
| `CAT` | Catálogo farmacéutico y maestro de productos | 18 |
| `COM` | Compras y proveedores / Procure-to-Pay | 16 |
| `INV` | Inventario, lotes, vencimientos y almacenes | 22 |
| `TRF` | Transferencias y reposición | 12 |
| `PRE` | Precios, promociones y Observatorio | 14 |
| `POS` | Retail, POS, caja y comprobantes | 22 |
| `DSP` | Prescripción y dispensación farmacéutica | 22 |
| `CTL` | Productos fiscalizados / controlados | 18 |
| `DEV` | Devoluciones y notas de crédito | 14 |
| `RCL` | Alertas sanitarias, inmovilización y recall | 12 |
| `FVG` | Farmacovigilancia y tecnovigilancia | 14 |
| `ERP` | ERP financiero, tesorería y cierre | 18 |
| `OBS` | Reportes regulatorios / Observatorio | 8 |
| `SEC` | Seguridad, privacidad y autorización | 12 |
| `AUD` | Auditoría y trazabilidad | 8 |
| `INT` | Integraciones | 10 |
| `RPT` | Reportes y analítica | 8 |

## 3. Catálogo

### 3.1. ORG — Organización y establecimientos

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-ORG-001` | Registrar grupo/empresa operadora | Gestionar la organización legal/corporativa que opera la cadena. | Alta |
| `RF-ORG-002` | Registrar establecimiento farmacéutico | Registrar cada farmacia/botica/local con identidad, ubicación y estado operativo. | Alta |
| `RF-ORG-003` | Mantener autorización sanitaria del establecimiento | Registrar datos y evidencias de autorización sanitaria y sus vigencias cuando correspondan. | Alta |
| `RF-ORG-004` | Asignar Director Técnico | Asociar al establecimiento su Director Técnico y período de responsabilidad. | Alta |
| `RF-ORG-005` | Registrar profesionales QF asistentes | Mantener profesionales habilitados/asignados por establecimiento y período. | Alta |
| `RF-ORG-006` | Registrar personal técnico | Mantener personal técnico y evidencia de su acreditación aplicable. | Alta |
| `RF-ORG-007` | Definir horarios operativos | Configurar horarios de establecimiento, caja y atención farmacéutica. | Alta |
| `RF-ORG-008` | Definir almacenes y ubicaciones internas | Configurar almacenes, zonas, cámaras, cuarentena y ubicaciones por establecimiento. | Alta |
| `RF-ORG-009` | Definir cajas y terminales POS | Registrar terminales, cajas y su pertenencia a un establecimiento. | Alta |
| `RF-ORG-010` | Gestionar vigencia/inactivación de local | Impedir nuevas operaciones en establecimientos inactivos sin borrar historia. | Alta |
| `RF-ORG-011` | Consultar estructura corporativa | Consultar empresas, establecimientos, almacenes y responsables vigentes. | Alta |
| `RF-ORG-012` | Auditar cambios organizacionales | Registrar cambios relevantes de autorizaciones, responsables y configuración. | Alta |

### 3.2. CAT — Catálogo farmacéutico y maestro de productos

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-CAT-001` | Registrar producto maestro | Registrar producto farmacéutico, dispositivo médico, producto sanitario u otro artículo permitido. | Alta |
| `RF-CAT-002` | Registrar SKU/presentación comercial | Separar el producto regulado de su presentación/SKU comercial. | Alta |
| `RF-CAT-003` | Registrar denominación/DCI/marca | Mantener denominaciones aplicables y marca comercial cuando corresponda. | Alta |
| `RF-CAT-004` | Registrar concentración y forma farmacéutica | Mantener atributos farmacéuticos estructurados cuando correspondan. | Alta |
| `RF-CAT-005` | Registrar vía/unidad/presentación | Mantener atributos de administración, unidad y presentación cuando correspondan. | Alta |
| `RF-CAT-006` | Registrar laboratorio/titular/fabricante | Mantener las entidades relacionadas al producto según datos disponibles. | Alta |
| `RF-CAT-007` | Registrar registro sanitario | Mantener número, estado, vigencia y evidencia/consulta del registro sanitario cuando aplique. | Alta |
| `RF-CAT-008` | Registrar condición de venta | Clasificar la condición de venta y si requiere receta/control específico. | Alta |
| `RF-CAT-009` | Clasificar producto fiscalizado | Asociar listas/control aplicables sin inferirlos desde el nombre comercial. | Alta |
| `RF-CAT-010` | Registrar código de barras | Permitir uno o más códigos de barras por SKU y vigencia. | Alta |
| `RF-CAT-011` | Registrar clasificación regulatoria | Distinguir farmacéutico, dispositivo, sanitario y otras categorías comerciales. | Alta |
| `RF-CAT-012` | Gestionar estados del producto | Distinguir borrador, activo, suspendido, bloqueado y retirado sin borrar historia. | Alta |
| `RF-CAT-013` | Versionar atributos regulatorios | Conservar la versión efectiva de atributos que afectan dispensación/venta. | Alta |
| `RF-CAT-014` | Importar/actualizar catálogo desde fuente externa | Permitir actualización controlada desde fuentes autorizadas cuando exista integración. | Alta |
| `RF-CAT-015` | Detectar duplicados de producto/SKU | Prevenir duplicidad por identificadores y reglas de negocio. | Alta |
| `RF-CAT-016` | Consultar producto por múltiples claves | Buscar por código interno, barras, DCI, marca, registro sanitario y descripción. | Alta |
| `RF-CAT-017` | Registrar sustitutos/alternativas como datos | Mantener relaciones informativas sin autorizar automáticamente sustitución al usuario. | Alta |
| `RF-CAT-018` | Auditar cambios del maestro | Registrar cambios a condición de venta, fiscalización y atributos regulatorios. | Alta |

### 3.3. COM — Compras y proveedores / Procure-to-Pay

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-COM-001` | Registrar proveedor | Gestionar proveedores y sus datos comerciales/fiscales. | Alta |
| `RF-COM-002` | Mantener habilitación del proveedor | Registrar evidencias/condiciones aplicables para comprar determinados productos. | Alta |
| `RF-COM-003` | Crear solicitud de compra | Registrar necesidad de compra y sus líneas. | Alta |
| `RF-COM-004` | Aprobar solicitud de compra | Aplicar niveles de aprobación configurables. | Alta |
| `RF-COM-005` | Generar orden de compra | Emitir OC a partir de solicitud/aprobación válida. | Alta |
| `RF-COM-006` | Versionar/modificar OC antes de recepción | Conservar cambios autorizados a cantidades, precios o fechas. | Alta |
| `RF-COM-007` | Registrar condiciones comerciales | Plazos, moneda, impuestos, descuentos y condiciones pactadas. | Alta |
| `RF-COM-008` | Registrar despacho/documento proveedor | Relacionar documento de traslado/entrega con la OC. | Alta |
| `RF-COM-009` | Registrar recepción contra OC | Registrar recepción total/parcial con trazabilidad a la orden. | Alta |
| `RF-COM-010` | Registrar diferencias de recepción | Documentar faltantes, sobrantes, daño u observaciones. | Alta |
| `RF-COM-011` | Registrar factura de proveedor | Registrar documento por pagar sin crear stock por sí mismo. | Alta |
| `RF-COM-012` | Conciliar OC-recepción-factura | Soportar matching configurable antes de pago. | Alta |
| `RF-COM-013` | Gestionar excepción de matching | Permitir aprobación documentada de discrepancias según política. | Media |
| `RF-COM-014` | Generar cuenta por pagar | Crear obligación financiera por factura aceptada. | Media |
| `RF-COM-015` | Gestionar devolución a proveedor | Relacionar productos/lotes y documentos de devolución. | Media |
| `RF-COM-016` | Auditar ciclo Procure-to-Pay | Trazar aprobaciones, recepciones, matching y pago. | Media |

### 3.4. INV — Inventario, lotes, vencimientos y almacenes

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-INV-001` | Registrar lote | Registrar lote/serie cuando sea trazable, con producto, origen y vencimiento. | Alta |
| `RF-INV-002` | Registrar fecha de vencimiento | Mantener vencimiento a nivel de lote/unidad cuando aplique. | Alta |
| `RF-INV-003` | Registrar inventario por ubicación | Mantener existencias por establecimiento, almacén, ubicación, producto y lote. | Alta |
| `RF-INV-004` | Distinguir estado de inventario | Separar disponible, reservado, tránsito, cuarentena, bloqueado y no vendible. | Alta |
| `RF-INV-005` | Registrar movimiento de inventario | Todo cambio de cantidad debe originar un movimiento trazable. | Alta |
| `RF-INV-006` | Consultar kardex | Consultar entradas/salidas/ajustes por producto, lote y establecimiento. | Alta |
| `RF-INV-007` | Realizar conteo cíclico | Registrar conteos físicos y diferencias. | Alta |
| `RF-INV-008` | Realizar inventario general | Soportar campañas de inventario por local/almacén. | Alta |
| `RF-INV-009` | Ajustar inventario con autorización | Los ajustes deben requerir motivo y autorización según política. | Alta |
| `RF-INV-010` | Bloquear lote | Impedir disponibilidad vendible de lote bloqueado. | Alta |
| `RF-INV-011` | Liberar lote bloqueado | Liberar solo mediante proceso autorizado y auditable. | Alta |
| `RF-INV-012` | Gestionar cuarentena | Mantener stock físicamente recibido pero no disponible. | Alta |
| `RF-INV-013` | Consultar próximos vencimientos | Listar lotes por horizonte configurable de vencimiento. | Alta |
| `RF-INV-014` | Gestionar merma | Registrar merma, motivo, lote, cantidad y responsable. | Alta |
| `RF-INV-015` | Gestionar destrucción/disposición | Registrar salida no comercial y evidencia cuando corresponda. | Alta |
| `RF-INV-016` | Reservar inventario | Reservar stock para transferencia/pedido evitando doble asignación. | Alta |
| `RF-INV-017` | Liberar reserva | Liberar reserva por cancelación, expiración o cumplimiento. | Alta |
| `RF-INV-018` | Aplicar política de selección de lote | Resolver FEFO/FIFO/manual u otra política configurada sin vender lotes bloqueados. | Alta |
| `RF-INV-019` | Evitar stock negativo no autorizado | No permitir disponibilidad negativa salvo política explícita y controlada. | Alta |
| `RF-INV-020` | Trazar origen-destino de lote | Conocer recepción, transferencias, ventas y otras salidas del lote. | Alta |
| `RF-INV-021` | Consultar stock corporativo | Consultar stock por local/canal/estado sin confundir disponible con físico. | Alta |
| `RF-INV-022` | Auditar ajustes e inventarios | Registrar actor, motivo y antes/después de operaciones sensibles. | Alta |

### 3.5. TRF — Transferencias y reposición

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-TRF-001` | Crear solicitud de transferencia | Registrar origen, destino, productos y cantidades. | Alta |
| `RF-TRF-002` | Aprobar transferencia | Aplicar aprobación cuando política lo exija. | Alta |
| `RF-TRF-003` | Reservar stock en origen | Impedir que stock comprometido se venda dos veces. | Alta |
| `RF-TRF-004` | Realizar picking por lote | Seleccionar lotes válidos según política. | Alta |
| `RF-TRF-005` | Despachar transferencia | Mover stock de disponible origen a tránsito. | Alta |
| `RF-TRF-006` | Registrar documento de traslado | Relacionar evidencia/documento de despacho cuando aplique. | Alta |
| `RF-TRF-007` | Recibir transferencia | Registrar recepción explícita por destino. | Alta |
| `RF-TRF-008` | Registrar diferencias de transferencia | Documentar faltante/sobrante/daño/cambio de lote. | Alta |
| `RF-TRF-009` | Resolver incidencia de transferencia | Ajustar diferencias mediante flujo autorizado. | Alta |
| `RF-TRF-010` | Cerrar transferencia | Cerrar cuando cantidades y diferencias estén resueltas. | Alta |
| `RF-TRF-011` | Generar propuesta de reposición | Calcular necesidades por demanda/stock/políticas configuradas. | Alta |
| `RF-TRF-012` | Convertir propuesta en compra o transferencia | Resolver el origen de abastecimiento sin crear movimiento automáticamente. | Alta |

### 3.6. PRE — Precios, promociones y Observatorio

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-PRE-001` | Registrar precio base | Mantener precio base con vigencia. | Alta |
| `RF-PRE-002` | Definir precio por ámbito | Aplicar precio por cadena, zona, establecimiento o canal. | Alta |
| `RF-PRE-003` | Aprobar precio | Separar propuesta, aprobación y publicación. | Alta |
| `RF-PRE-004` | Publicar precio | Distribuir precio vigente a canales. | Alta |
| `RF-PRE-005` | Registrar promoción | Configurar promoción con vigencia, condiciones y ámbito. | Alta |
| `RF-PRE-006` | Validar compatibilidad de promoción | No permitir que promoción eluda receta, bloqueo o controles sanitarios. | Alta |
| `RF-PRE-007` | Resolver precio efectivo | Determinar precio aplicable de forma reproducible y auditable. | Alta |
| `RF-PRE-008` | Registrar descuentos manuales | Requerir permiso/motivo según umbral configurable. | Alta |
| `RF-PRE-009` | Registrar evidencia de precio aplicado | Conservar reglas/versión de precio/promoción usadas en la venta. | Alta |
| `RF-PRE-010` | Preparar reporte Observatorio | Construir conjunto de datos exigible por establecimiento/producto/período. | Alta |
| `RF-PRE-011` | Validar reporte Observatorio | Detectar datos faltantes/inconsistentes antes de envío. | Alta |
| `RF-PRE-012` | Registrar envío/carga al Observatorio | Conservar fecha, canal, responsable y evidencia/acuse cuando exista. | Alta |
| `RF-PRE-013` | Reprocesar errores de reporte | Corregir y reenviar sin duplicar evidencia. | Media |
| `RF-PRE-014` | Consultar histórico de precios | Consultar vigencias y cambios por producto/ámbito. | Media |

### 3.7. POS — Retail, POS, caja y comprobantes

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-POS-001` | Abrir turno de caja | Registrar operador, terminal, caja, fondo y fecha/hora. | Alta |
| `RF-POS-002` | Registrar venta | Crear venta con establecimiento, terminal, turno, operador y canal. | Alta |
| `RF-POS-003` | Agregar producto por escaneo/búsqueda | Resolver SKU/producto y restricciones aplicables. | Alta |
| `RF-POS-004` | Validar stock vendible | Impedir venta de cantidad no disponible/bloqueada. | Alta |
| `RF-POS-005` | Aplicar precio/promoción vigente | Resolver precio efectivo con reglas aprobadas. | Alta |
| `RF-POS-006` | Gestionar cliente de venta | Identificar cliente cuando sea requerido o voluntario conforme a privacidad. | Alta |
| `RF-POS-007` | Gestionar múltiples medios de pago | Soportar efectivo, tarjeta y otros medios configurados. | Alta |
| `RF-POS-008` | Registrar pago mixto | Permitir combinación de medios preservando conciliación. | Alta |
| `RF-POS-009` | Emitir boleta electrónica | Generar CPE conforme a configuración fiscal aplicable. | Alta |
| `RF-POS-010` | Emitir factura electrónica | Requerir datos del adquirente exigibles para factura. | Alta |
| `RF-POS-011` | Registrar estado SUNAT del CPE | Separar venta, emisión y estado/aceptación tributaria. | Alta |
| `RF-POS-012` | Reintentar envío CPE | Reintentar sin duplicar correlativos/documentos. | Alta |
| `RF-POS-013` | Imprimir/entregar representación | Entregar representación física/electrónica según flujo configurado. | Alta |
| `RF-POS-014` | Cancelar venta antes de cierre | Aplicar reglas de anulación según estado y CPE. | Alta |
| `RF-POS-015` | Registrar retiro/ingreso de efectivo | Gestionar movimientos no venta con autorización. | Alta |
| `RF-POS-016` | Realizar arqueo | Comparar esperado vs contado por medio de pago. | Alta |
| `RF-POS-017` | Registrar diferencia de caja | Registrar faltante/sobrante con motivo. | Alta |
| `RF-POS-018` | Cerrar turno | Cerrar operación del turno preservando pendientes de integración. | Alta |
| `RF-POS-019` | Consultar ventas de turno/local | Consultar ventas, devoluciones, pagos y CPE. | Alta |
| `RF-POS-020` | Operar contingencia de conectividad | Soportar estrategia de contingencia según ADR/política futura. | Alta |
| `RF-POS-021` | Sincronizar transacciones de tienda | Sincronizar sin duplicados cuando exista operación distribuida/offline. | Alta |
| `RF-POS-022` | Auditar operaciones POS sensibles | Auditar descuentos, anulaciones, retiros y reaperturas. | Alta |

### 3.8. DSP — Prescripción y dispensación farmacéutica

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-DSP-001` | Registrar/recibir prescripción | Registrar receta física, imagen o electrónica según alcance vigente. | Alta |
| `RF-DSP-002` | Capturar datos del prescriptor | Registrar identidad/colegiatura y datos requeridos según tipo de receta. | Alta |
| `RF-DSP-003` | Capturar datos del paciente | Registrar datos requeridos por receta y minimización aplicable. | Alta |
| `RF-DSP-004` | Registrar líneas prescritas | DCI/producto, concentración, forma, dosis, vía, duración e indicaciones cuando correspondan. | Alta |
| `RF-DSP-005` | Validar integridad de receta | Verificar presencia de datos mínimos aplicables. | Alta |
| `RF-DSP-006` | Validar vigencia de receta | Aplicar regla según tipo/lista/norma, sin plazo universal inventado. | Alta |
| `RF-DSP-007` | Validar condición de venta | Determinar si el producto puede venderse sin receta o requiere dispensación. | Alta |
| `RF-DSP-008` | Asignar QF responsable | Toda dispensación que requiera acto farmacéutico debe identificar profesional competente. | Alta |
| `RF-DSP-009` | Analizar prescripción | Registrar validación/análisis farmacéutico cuando corresponda. | Alta |
| `RF-DSP-010` | Registrar observación al prescriptor | Documentar consulta/subsanación/rechazo cuando receta tenga defectos. | Alta |
| `RF-DSP-011` | Seleccionar producto/lote dispensable | Seleccionar stock válido sin alterar la prescripción de forma no autorizada. | Alta |
| `RF-DSP-012` | Registrar dispensación total | Registrar cantidades efectivamente dispensadas. | Alta |
| `RF-DSP-013` | Registrar dispensación parcial | Permitir parcial cuando la regla del tipo de receta lo admita y preservar saldo. | Alta |
| `RF-DSP-014` | Registrar entrega e información al usuario | Documentar orientación/información mínima cuando el proceso lo requiera. | Alta |
| `RF-DSP-015` | Relacionar dispensación con venta POS | Liberar líneas válidas al cobro sin fusionar ambos procesos. | Alta |
| `RF-DSP-016` | Impedir dispensación por personal no competente | Aplicar las restricciones vigentes al personal técnico. | Alta |
| `RF-DSP-017` | Impedir alternativa automática al medicamento prescrito | El sistema no debe permitir al técnico ofrecer/sustituir alternativas como acto de dispensación. | Alta |
| `RF-DSP-018` | Conservar evidencia de receta atendida | Retener/archivar según tipo de receta y regla aplicable. | Alta |
| `RF-DSP-019` | Registrar rechazo de receta | Registrar motivo, responsable y fecha. | Alta |
| `RF-DSP-020` | Auditar dispensación | Registrar actor, receta, producto, cantidad y resultado. | Alta |
| `RF-DSP-021` | Consultar histórico de dispensaciones | Consultar por receta/paciente/producto con permisos adecuados. | Alta |
| `RF-DSP-022` | Proteger datos de prescripción | Restringir exposición de datos personales/salud según finalidad y rol. | Alta |

### 3.9. CTL — Productos fiscalizados / controlados

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-CTL-001` | Clasificar producto por lista fiscalizada | Resolver la lista normativa aplicable por versión/fuente. | Alta |
| `RF-CTL-002` | Determinar tipo de receta exigible | Aplicar receta especial o común según clasificación vigente. | Alta |
| `RF-CTL-003` | Validar receta especial | Validar requisitos y vigencia aplicable a listas correspondientes. | Alta |
| `RF-CTL-004` | Bloquear receta especial vencida | No atender recetas fuera de vigencia normativa. | Alta |
| `RF-CTL-005` | Bloquear receta adulterada/sospechosa | Impedir atención cuando existan señales/sospecha conforme a regla aplicable. | Alta |
| `RF-CTL-006` | Retener receta atendida | Registrar retención/archivo cuando norma lo exige. | Alta |
| `RF-CTL-007` | Registrar adquirente y cantidad dispensada | Capturar datos exigibles en la atención de controlados. | Alta |
| `RF-CTL-008` | Registrar dispensación parcial controlada | Mantener saldo y anotaciones/evidencia cuando se admita dispensación parcial. | Alta |
| `RF-CTL-009` | Gestionar adquisición con control especial | Incorporar autorización/formulario cuando el tipo de producto lo exija. | Alta |
| `RF-CTL-010` | Gestionar almacenamiento restringido | Controlar ubicación/estado especial según exigencias aplicables. | Alta |
| `RF-CTL-011` | Registrar libro/registro de existencias | Mantener movimientos y saldos de productos fiscalizados. | Alta |
| `RF-CTL-012` | Conciliar saldo físico vs registro | Detectar faltantes/excedentes y generar incidencia. | Alta |
| `RF-CTL-013` | Registrar robo/sustracción/siniestro | Registrar evento y evidencias/comunicaciones aplicables. | Alta |
| `RF-CTL-014` | Preparar balance trimestral | Consolidar movimientos para listas/sujetos obligados. | Alta |
| `RF-CTL-015` | Registrar presentación de balance | Conservar período, fecha, responsable y evidencia. | Alta |
| `RF-CTL-016` | Reportar receta falsificada/adulterada | Gestionar comunicación dentro del plazo aplicable cuando corresponda. | Alta |
| `RF-CTL-017` | Restringir promoción de controlados | Aplicar restricciones normativas de promoción a listas correspondientes. | Alta |
| `RF-CTL-018` | Auditar acceso y movimientos controlados | Auditoría reforzada de stock, receta y usuario. | Alta |

### 3.10. DEV — Devoluciones y notas de crédito

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-DEV-001` | Registrar solicitud de devolución | Identificar venta, ítem, cantidad, motivo y solicitante. | Alta |
| `RF-DEV-002` | Validar elegibilidad comercial | Aplicar política de devolución sin confundirla con disposición sanitaria. | Alta |
| `RF-DEV-003` | Autorizar devolución | Aplicar permisos/umbrales según política. | Alta |
| `RF-DEV-004` | Emitir nota de crédito | Generar nota electrónica vinculada al comprobante anterior cuando corresponda. | Alta |
| `RF-DEV-005` | Registrar estado SUNAT de nota | Mantener aceptación/rechazo tributario independiente del producto físico. | Alta |
| `RF-DEV-006` | Recibir producto devuelto | Registrar producto/lote/cantidad y condición física. | Alta |
| `RF-DEV-007` | Enviar producto devuelto a evaluación | Ingresar a cuarentena/no vendible mientras se determina disposición cuando aplique. | Alta |
| `RF-DEV-008` | Determinar disposición de producto devuelto | Definir reingreso, devolución proveedor, destrucción u otro destino autorizado. | Alta |
| `RF-DEV-009` | Reingresar a stock solo con autorización | No aumentar stock vendible por mera emisión de nota de crédito. | Alta |
| `RF-DEV-010` | Gestionar devolución parcial | Afectar únicamente cantidades devueltas. | Alta |
| `RF-DEV-011` | Gestionar devolución a proveedor | Relacionar lote/recepción/compra y documentos. | Alta |
| `RF-DEV-012` | Registrar reembolso | Relacionar nota/venta con devolución del medio de pago. | Alta |
| `RF-DEV-013` | Auditar devolución | Registrar antes/después, motivo, aprobación y destino físico. | Media |
| `RF-DEV-014` | Consultar historial de devoluciones | Consultar comercial, tributario y sanitario de forma separada. | Media |

### 3.11. RCL — Alertas sanitarias, inmovilización y recall

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-RCL-001` | Registrar alerta | Registrar fuente, fecha, documento, producto/lote y alcance. | Alta |
| `RF-RCL-002` | Clasificar acción de alerta | Definir acción operativa sin asumir taxonomía normativa no validada. | Alta |
| `RF-RCL-003` | Identificar stock afectado | Localizar lotes y cantidades en todos los establecimientos/canales. | Alta |
| `RF-RCL-004` | Bloquear venta por lote | Impedir nuevas salidas vendibles del lote afectado. | Alta |
| `RF-RCL-005` | Propagar bloqueo a canales | Aplicar bloqueo a POS, e-commerce y reservas. | Alta |
| `RF-RCL-006` | Generar tareas de inmovilización | Asignar acciones por establecimiento/almacén. | Alta |
| `RF-RCL-007` | Confirmar inmovilización física | Registrar cantidad localizada, responsable y fecha. | Alta |
| `RF-RCL-008` | Localizar movimientos/ventas históricos | Trazar movimientos del lote antes del bloqueo. | Alta |
| `RF-RCL-009` | Gestionar destino del lote | Registrar devolución, traslado, destrucción u otro destino indicado. | Alta |
| `RF-RCL-010` | Conciliar recall | Conciliar cantidad afectada, localizada y destino. | Alta |
| `RF-RCL-011` | Cerrar recall | Cerrar solo con evidencia/pendientes resueltos según política. | Alta |
| `RF-RCL-012` | Auditar recall | Preservar fuente, acciones, actores y evidencias. | Alta |

### 3.12. FVG — Farmacovigilancia y tecnovigilancia

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-FVG-001` | Registrar reporte de evento | Registrar sospecha/evento aun sin venta asociada. | Alta |
| `RF-FVG-002` | Clasificar medicamento/dispositivo | Distinguir farmacovigilancia y tecnovigilancia. | Alta |
| `RF-FVG-003` | Registrar producto sospechoso | Relacionar producto/lote cuando la información exista. | Alta |
| `RF-FVG-004` | Registrar paciente protegido | Capturar datos mínimos con acceso restringido. | Alta |
| `RF-FVG-005` | Registrar reportante | Capturar profesional/usuario y contacto según finalidad. | Alta |
| `RF-FVG-006` | Registrar descripción del evento | Mantener narrativa y fechas relevantes. | Alta |
| `RF-FVG-007` | Clasificar gravedad/prioridad | Aplicar catálogos/reglas validadas. | Alta |
| `RF-FVG-008` | Asignar responsable QF | Asignar seguimiento a profesional autorizado. | Alta |
| `RF-FVG-009` | Solicitar información faltante | Registrar seguimientos y respuestas. | Alta |
| `RF-FVG-010` | Determinar plazo aplicable | Resolver plazo por sujeto/gravedad/norma, parametrizable. | Alta |
| `RF-FVG-011` | Registrar notificación oficial | Conservar canal, fecha, responsable e identificador/evidencia. | Alta |
| `RF-FVG-012` | Actualizar reporte sin borrar historia | Versionar/complementar información. | Alta |
| `RF-FVG-013` | Cerrar reporte | Cerrar con estado y motivo. | Media |
| `RF-FVG-014` | Auditar acceso a datos sensibles | Registrar acceso a información protegida. | Media |

### 3.13. ERP — ERP financiero, tesorería y cierre

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-ERP-001` | Generar resumen financiero de tienda | Consolidar transacciones por período/establecimiento/turno según diseño. | Alta |
| `RF-ERP-002` | Validar transacciones antes de posting | Validar totales, impuestos, pagos, CPE y referencias. | Alta |
| `RF-ERP-003` | Conciliar medios de pago | Comparar POS con efectivo/adquirentes/otros medios. | Alta |
| `RF-ERP-004` | Gestionar cuentas por pagar | Mantener obligaciones a proveedor. | Alta |
| `RF-ERP-005` | Registrar pago a proveedor | Aplicar pago y conciliación a CxP. | Alta |
| `RF-ERP-006` | Gestionar cuentas por cobrar | Mantener saldos cuando existan ventas a crédito/convenios. | Alta |
| `RF-ERP-007` | Generar asiento/posting de venta | Publicar transacciones/resúmenes al ERP/contabilidad. | Alta |
| `RF-ERP-008` | Generar posting de devoluciones | Contabilizar notas/devoluciones sin editar histórico. | Alta |
| `RF-ERP-009` | Generar posting de inventario/costo | Integrar movimientos de stock/costo según política contable definida. | Alta |
| `RF-ERP-010` | Aplicar clave idempotente de posting | Evitar contabilización duplicada. | Alta |
| `RF-ERP-011` | Registrar aceptación/rechazo ERP | Mantener resultado e identificador del sistema destino. | Alta |
| `RF-ERP-012` | Reintentar posting fallido | Reintentar de manera idempotente. | Alta |
| `RF-ERP-013` | Registrar ajustes/reversas | Corregir con transacciones nuevas trazables. | Media |
| `RF-ERP-014` | Separar cierre de caja y cierre contable | Mantener estados independientes. | Media |
| `RF-ERP-015` | Consultar pendientes de integración | Visibilizar transacciones no contabilizadas. | Media |
| `RF-ERP-016` | Gestionar período contable | Respetar apertura/cierre según ERP/política. | Media |
| `RF-ERP-017` | Integrar impuestos/comprobantes | Transmitir datos requeridos al componente financiero/fiscal. | Media |
| `RF-ERP-018` | Auditar posting financiero | Registrar origen, clave, actor/proceso, fecha y respuesta. | Media |

### 3.14. OBS — Reportes regulatorios / Observatorio

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-OBS-001` | Determinar establecimientos obligados a reporte | Configurar alcance por sujeto y norma vigente. | Alta |
| `RF-OBS-002` | Seleccionar productos reportables | Resolver universo según fuente/regla vigente. | Alta |
| `RF-OBS-003` | Generar reporte mensual de precios | Preparar información por período. | Alta |
| `RF-OBS-004` | Validar consistencia del reporte | Detectar campos/establecimientos/productos incompletos. | Alta |
| `RF-OBS-005` | Registrar presentación | Conservar evidencia, fecha y responsable. | Alta |
| `RF-OBS-006` | Reprocesar observaciones | Corregir sin perder histórico. | Alta |
| `RF-OBS-007` | Consultar cumplimiento por período | Mostrar reportado/pendiente/error. | Alta |
| `RF-OBS-008` | Auditar cambios previos al envío | Registrar transformaciones y aprobaciones. | Alta |

### 3.15. SEC — Seguridad, privacidad y autorización

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-SEC-001` | Autenticar usuarios | Proveer autenticación segura; mecanismo tecnológico se definirá en ADR. | Alta |
| `RF-SEC-002` | Gestionar roles | Definir roles por funciones de negocio. | Alta |
| `RF-SEC-003` | Gestionar permisos | Administrar permisos granulares por capacidad/acción. | Alta |
| `RF-SEC-004` | Gestionar ámbitos de acceso | Limitar acceso por empresa, establecimiento, almacén u otro ámbito. | Alta |
| `RF-SEC-005` | Restringir actos farmacéuticos por competencia | La autorización técnica no sustituye competencia profesional. | Alta |
| `RF-SEC-006` | Proteger datos personales | Aplicar minimización, finalidad y controles de acceso. | Alta |
| `RF-SEC-007` | Proteger datos de salud/recetas/FVG | Aplicar controles reforzados a datos sensibles. | Alta |
| `RF-SEC-008` | Gestionar sesiones/revocación | Permitir cierre/revocación de sesiones conforme a arquitectura definida. | Alta |
| `RF-SEC-009` | Aplicar MFA a roles críticos cuando se defina | Soportar MFA configurable según riesgo/ADR. | Alta |
| `RF-SEC-010` | Registrar accesos denegados | Auditar intentos de acceso relevantes. | Alta |
| `RF-SEC-011` | Gestionar segregación de funciones | Evitar combinaciones de permisos incompatibles cuando se definan. | Alta |
| `RF-SEC-012` | No exponer secretos en logs | Impedir registro de credenciales/tokens/secretos. | Alta |
| `RF-SEC-013` | Resolver navegación dinámica | Construir el menú de la sesión según aplicación, permisos y ámbito vigente. | Media |
| `RF-SEC-014` | Autorizar independientemente del menú | Cada endpoint debe validar autorización aunque la opción esté oculta/no presente en navegación. | Alta |

### 3.16. AUD — Auditoría y trazabilidad

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-AUD-001` | Auditar operaciones críticas | Registrar actor, acción, recurso, fecha, resultado y contexto. | Alta |
| `RF-AUD-002` | Auditar cambios de maestros regulatorios | Registrar antes/después o versión de cambios sensibles. | Alta |
| `RF-AUD-003` | Auditar acceso a datos sensibles | Registrar consulta/descarga/exportación de recetas/FVG cuando corresponda. | Alta |
| `RF-AUD-004` | Auditar ajustes de stock | Registrar motivos y autorizaciones. | Alta |
| `RF-AUD-005` | Auditar descuentos/devoluciones | Registrar operaciones retail susceptibles a fraude. | Alta |
| `RF-AUD-006` | Auditar roles/permisos | Registrar cambios de autorización. | Alta |
| `RF-AUD-007` | Correlacionar operaciones distribuidas | Usar identificador de correlación entre POS/ERP/integraciones. | Alta |
| `RF-AUD-008` | Conservar auditoría sin edición destructiva | No permitir modificación ordinaria del histórico de auditoría. | Alta |

### 3.17. INT — Integraciones

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-INT-001` | Integrar SUNAT CPE | Emitir/enviar/consultar CPE mediante mecanismo aprobado. | Alta |
| `RF-INT-002` | Integrar ERP externo cuando aplique | Publicar/consultar transacciones mediante contratos versionados. | Alta |
| `RF-INT-003` | Integrar adquirentes de pago cuando aplique | Importar/conciliar liquidaciones según proveedor. | Alta |
| `RF-INT-004` | Integrar catálogo/registros sanitarios cuando exista fuente | Consumir fuentes oficiales sin convertir indisponibilidad externa en corrupción del maestro. | Alta |
| `RF-INT-005` | Integrar Observatorio según mecanismo vigente | Soportar API/archivo/portal asistido según investigación específica. | Alta |
| `RF-INT-006` | Integrar e-commerce/app | Exponer stock/precio/pedido con reglas comunes. | Alta |
| `RF-INT-007` | Integrar delivery | Gestionar despacho/entrega preservando restricciones de producto. | Alta |
| `RF-INT-008` | Aplicar idempotencia | Evitar duplicados en mensajes/comandos reintentados. | Alta |
| `RF-INT-009` | Registrar errores/reintentos | Mantener trazabilidad operacional. | Alta |
| `RF-INT-010` | Versionar contratos | Mantener compatibilidad y evolución controlada. | Alta |

### 3.18. RPT — Reportes y analítica

| Código | Requerimiento | Descripción | Prioridad inicial |
|---|---|---|---|
| `RF-RPT-001` | Dashboard ventas | Mostrar indicadores por cadena/local/canal sin sustituir contabilidad. | Media |
| `RF-RPT-002` | Dashboard inventario | Mostrar stock, disponibilidad, lotes y vencimientos. | Media |
| `RF-RPT-003` | Dashboard compras | Mostrar OC, recepciones, facturas y discrepancias. | Media |
| `RF-RPT-004` | Dashboard productos controlados | Mostrar saldos/movimientos/cumplimiento con autorización. | Media |
| `RF-RPT-005` | Dashboard recall | Mostrar lotes afectados y conciliación. | Media |
| `RF-RPT-006` | Dashboard farmacovigilancia | Mostrar cumplimiento agregado preservando confidencialidad. | Media |
| `RF-RPT-007` | Exportar reportes autorizados | Aplicar permisos, límites y auditoría. | Media |
| `RF-RPT-008` | Distinguir dato operativo de dato contable | Etiquetar fuente/fecha de corte de indicadores. | Media |

## 4. RF críticos detallados

### RF-CAT-008 — Registrar condición de venta

**Precondiciones**
- Producto maestro/SKU existente.
- Usuario con permiso de gobierno de catálogo.

**Flujo funcional mínimo**
1. El usuario selecciona el producto/SKU regulado.
2. Registra la condición de venta y su fuente/vigencia.
3. El sistema valida que la clasificación sea compatible con la categoría regulatoria.
4. Se publica una nueva versión efectiva.
5. Los canales de venta consumen la versión vigente; las ventas históricas conservan la versión aplicada.

**Criterios funcionales asociados**
- No se permite cambiar retrospectivamente una venta cerrada al modificar la condición de venta.
- Una condición que exige receta activa el flujo de dispensación antes del cobro.
- El cambio queda auditado.

### RF-INV-003 — Registrar inventario por ubicación

**Precondiciones**
- Establecimiento/almacén/ubicación válidos.
- Producto/SKU válido.

**Flujo funcional mínimo**
1. Una recepción/movimiento identifica producto, lote cuando corresponda, ubicación y estado.
2. El sistema registra el movimiento que explica el cambio de existencia.
3. Se recalculan existencias físicas, reservadas y disponibles sin mezclar estados.

**Criterios funcionales asociados**
- No existe modificación de saldo sin movimiento justificante.
- Stock bloqueado/cuarentena no se expone como vendible.
- La trazabilidad permite reconstruir el saldo por movimientos.

### RF-INV-010 — Bloquear lote

**Precondiciones**
- Lote existente.
- Actor autorizado.
- Motivo/fuente identificada.

**Flujo funcional mínimo**
1. El actor selecciona lote y alcance.
2. Registra motivo, fuente y vigencia si aplica.
3. El sistema cambia la disponibilidad de todas las existencias afectadas.
4. Se propaga la restricción a POS/canales/reservas.

**Criterios funcionales asociados**
- Una venta nueva del lote bloqueado debe rechazarse.
- El stock físico no se elimina por el bloqueo.
- El evento queda auditado por establecimiento/lote.

### RF-POS-002 — Registrar venta

**Precondiciones**
- Turno/caja válidos.
- Operador autenticado/autorizado.
- Establecimiento operativo.

**Flujo funcional mínimo**
1. El operador crea la venta.
2. Agrega líneas y el sistema resuelve producto, condición de venta, stock y precio.
3. Si una línea requiere dispensación, el POS exige referencia válida al proceso farmacéutico.
4. Se registran pagos.
5. Se emite/relaciona el CPE según el flujo fiscal.
6. Se registran movimientos de inventario y cierre transaccional de la venta de manera consistente.

**Criterios funcionales asociados**
- No puede cerrarse una venta con línea bajo receta sin dispensación válida cuando sea exigible.
- La venta conserva establecimiento, terminal, turno y operador.
- Un reintento técnico no debe crear una segunda venta/CPE.

### RF-POS-009 — Emitir boleta electrónica

**Precondiciones**
- Venta apta para comprobante.
- Configuración CPE vigente.

**Flujo funcional mínimo**
1. El sistema asigna serie/correlativo conforme al emisor/configuración.
2. Genera el documento en formato exigido por el mecanismo SUNAT aplicable.
3. Envía o deja en cola de envío según contingencia válida.
4. Registra respuesta/estado tributario.
5. Pone a disposición la representación/consulta según política.

**Criterios funcionales asociados**
- El estado de la venta no se confunde con la aceptación SUNAT.
- El reintento usa idempotencia y no genera un correlativo adicional.
- La nota de crédito referencia un comprobante previo válido conforme a SUNAT.

### RF-DSP-001 — Registrar/recibir prescripción

**Precondiciones**
- Producto potencialmente sujeto a receta o usuario solicita dispensación.
- Actor autorizado para recepción.

**Flujo funcional mínimo**
1. El sistema registra el tipo/soporte de receta dentro del alcance habilitado.
2. Captura prescriptor, paciente, fecha y líneas prescritas.
3. Conserva evidencia/documento conforme a política y base legal.
4. La receta queda pendiente de validación farmacéutica.

**Criterios funcionales asociados**
- No se asume que toda imagen digital tenga validez permanente fuera del marco normativo aplicable.
- Los datos sensibles quedan restringidos por rol/finalidad.
- La receta no se marca atendida al momento de registrarla.

### RF-DSP-016 — Impedir dispensación por personal no competente

**Precondiciones**
- Usuario autenticado.
- Receta/producto sujeto a dispensación bajo receta.

**Flujo funcional mínimo**
1. El sistema identifica la función/competencia del actor.
2. Si el actor es personal técnico en farmacia, no permite ejecutar el acto de dispensación de productos de venta bajo receta.
3. La operación requiere un profesional competente según la regla vigente.

**Criterios funcionales asociados**
- Personal técnico puede ejecutar solo acciones administrativas permitidas por su rol, no confirmar el acto farmacéutico.
- El intento denegado queda auditado.
- La autorización informática no puede sobrepasar la competencia profesional.

### RF-CTL-003 — Validar receta especial

**Precondiciones**
- Producto clasificado en lista que exige receta especial.

**Flujo funcional mínimo**
1. El sistema determina la regla aplicable por lista/version normativa.
2. Valida campos exigibles, ausencia de condiciones bloqueantes y fecha de expedición.
3. Presenta la receta al QF responsable para decisión.
4. Si es atendida, registra cantidad/adquirente/evidencia y retención/archivo según corresponda.

**Criterios funcionales asociados**
- Para listas II A, III A, III B y III C se controla la vigencia de tres días indicada por el D.S. 023-2001-SA.
- No se atiende receta con enmendaduras o sospecha/evidencia de adulteración/falsificación según norma aplicable.
- La receta retenida conserva trazabilidad de archivo.

### RF-CTL-014 — Preparar balance trimestral

**Precondiciones**
- Establecimiento/sujeto obligado configurado.
- Período cerrado.

**Flujo funcional mínimo**
1. El sistema determina productos/listas sujetos al balance.
2. Consolida saldo inicial, entradas, salidas y saldo final desde el registro controlado.
3. Valida consistencia contra existencias y movimientos.
4. Genera salida en el formato/mecanismo vigente o datos para su presentación.

**Criterios funcionales asociados**
- No se presenta balance de una lista/sujeto no aplicable por mera configuración genérica.
- Las diferencias quedan bloqueantes o justificadas según regla.
- Se conserva evidencia de la versión presentada.

### RF-DEV-004 — Emitir nota de crédito

**Precondiciones**
- Comprobante previo elegible.
- Devolución/anulación/ajuste autorizado.

**Flujo funcional mínimo**
1. El sistema identifica factura/boleta previa.
2. Calcula importes y líneas afectadas.
3. Genera nota electrónica según mecanismo SUNAT.
4. Registra estado y relación con devolución/pago cuando corresponda.

**Criterios funcionales asociados**
- La nota de crédito no reingresa automáticamente producto físico a stock vendible.
- No se emite contra comprobante incompatible.
- Los reintentos son idempotentes.

### RF-RCL-004 — Bloquear venta por lote

**Precondiciones**
- Alerta/decisión vigente.
- Lote identificado.

**Flujo funcional mínimo**
1. El responsable activa el bloqueo.
2. El sistema identifica existencias y reservas del lote.
3. Marca la disponibilidad como no vendible y propaga el bloqueo a canales.
4. Genera tareas de inmovilización por establecimiento.

**Criterios funcionales asociados**
- El lote no puede venderse después del bloqueo.
- La venta histórica no se altera.
- El cierre de recall exige conciliación posterior, no solo cambio de estado.

### RF-FVG-001 — Registrar reporte de evento

**Precondiciones**
- Usuario/reportante comunica evento.

**Flujo funcional mínimo**
1. El sistema registra el reporte aunque no exista venta POS.
2. Clasifica medicamento/dispositivo.
3. Captura datos mínimos disponibles del producto, paciente, reportante y evento.
4. Asigna acceso restringido y responsable.

**Criterios funcionales asociados**
- La ausencia de venta de la cadena no impide registrar el evento.
- Los datos personales/salud no se muestran a perfiles comerciales sin base/permiso.
- Las actualizaciones preservan el reporte original.

### RF-ERP-010 — Aplicar clave idempotente de posting

**Precondiciones**
- Lote de integración listo.

**Flujo funcional mínimo**
1. El sistema genera/recibe clave idempotente estable.
2. Publica el posting al ERP.
3. Registra respuesta y clave del destino.
4. Ante reintento, reconoce una operación ya aplicada o reintenta sin duplicar asiento.

**Criterios funcionales asociados**
- Una misma clave no produce dos postings válidos.
- Un error de integración no cambia el estado histórico de la venta.
- Las reversas se modelan como nuevas operaciones trazables.

### RF-OBS-003 — Generar reporte mensual de precios

**Precondiciones**
- Período/establecimientos reportables determinados.
- Datos de precio disponibles.

**Flujo funcional mínimo**
1. El sistema reúne los datos exigibles por el mecanismo oficial vigente.
2. Valida establecimientos, productos y campos obligatorios.
3. Genera el artefacto o carga preparada para presentación.
4. Registra aprobación/envío/evidencia.

**Criterios funcionales asociados**
- La periodicidad mensual se parametriza desde la obligación vigente, no desde el último ticket.
- Los errores se reprocesan sin borrar el envío anterior.
- Se puede demostrar qué fuente de precio produjo cada registro.

## 5. Reglas de diseño funcional

- `Venta`, `Dispensación`, `Pago` y `CPE` son conceptos diferentes aunque formen parte de una misma experiencia de caja.
- `Stock físico`, `stock disponible`, `stock reservado`, `stock en tránsito` y `stock bloqueado` no son equivalentes.
- `Producto`, `SKU/presentación`, `lote` y `existencia por ubicación` se mantendrán conceptualmente separados.
- `Devolución comercial`, `nota de crédito`, `reembolso` y `disposición sanitaria del producto devuelto` son procesos diferentes.
- Los estados históricos cerrados se rectifican/ajustan mediante operaciones trazables; no mediante sobrescritura destructiva.
- Los requisitos de productos fiscalizados se resuelven por clasificación/lista y versión normativa, no por texto libre del producto.
- La operación offline es una capacidad candidata: no se da por aceptada hasta ADR y requisitos de recuperación/sincronización.
