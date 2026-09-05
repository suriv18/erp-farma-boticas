# RN-FAR — Catálogo de Reglas de Negocio

**Versión:** 0.1  
**Estado:** Borrador trazable; reglas marcadas `POR_VALIDAR` no deben codificarse como obligación.

## 1. Clasificación

| Etiqueta | Significado |
|---|---|
| `NORM` | Norma/fuente sanitaria oficial. |
| `FISCAL` | SUNAT/tributario. |
| `DOM` | Invariante/decisión del dominio. |
| `ERP` | Práctica de ERP/retail. |
| `SEC` | Seguridad/privacidad. |
| `AUD` | Auditoría. |
| `CFG` | Parametrizable por cadena/sujeto/norma. |
| `TEC` | Decisión técnica. |
| `POR_VALIDAR` | Pendiente de evidencia/decisión. |

## 2. Catálogo inicial (87 reglas)

### 2.1. ORG

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-ORG-001` **Competencia de personal técnico** | El personal técnico en farmacia de farmacias/boticas no puede ejecutar actos correspondientes a la dispensación de productos farmacéuticos de venta bajo receta médica ni ofrecer alternativas al medicamento prescrito. | D.S. 015-2025-SA, art. 43 | `NORM` | Farmacias y boticas |
| `RN-ORG-002` **Supervisión del Director Técnico** | El Director Técnico responde por la competencia técnica y capacita/supervisa permanentemente al personal asistente y técnico. | D.S. 015-2025-SA, art. 43 | `NORM` | Farmacias y boticas |
| `RN-ORG-003` **Acreditación de técnico** | El personal técnico debe contar con título o certificado de estudios culminados que lo acredite como técnico en farmacia. | D.S. 015-2025-SA, art. 43 | `NORM` | Farmacias y boticas |
| `RN-ORG-004` **Histórico de responsables** | El cambio de Director Técnico/QF no elimina el período de responsabilidad anterior. | Derivado de trazabilidad | `DOM/AUD` | Cadena/establecimiento según aplique |

### 2.2. CAT

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-CAT-001` **Condición de venta** | La venta/dispensación debe respetar la condición de venta vigente del producto y las reglas especiales aplicables. | Ley 29459 / D.S. 016-2011-SA / proceso | `NORM/DOM` | Cadena/establecimiento según aplique |
| `RN-CAT-002` **Versión regulatoria** | Cambios de condición de venta o fiscalización no modifican retrospectivamente transacciones cerradas. | Trazabilidad | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-CAT-003` **Controlados por clasificación** | Las obligaciones de fiscalización se determinan por listas/clasificación normativa, no por coincidencias de texto del nombre. | D.S. 023-2001-SA | `NORM/DOM` | Cadena/establecimiento según aplique |
| `RN-CAT-004` **Producto vs SKU** | Un producto regulado puede tener una o más presentaciones/SKU comerciales; esta separación no altera su condición sanitaria. | Modelo retail/farmacéutico | `DOM` | Cadena/establecimiento según aplique |

### 2.3. INV

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-INV-001` **Movimiento obligatorio** | Todo cambio de existencia debe estar sustentado por un movimiento, recepción, transferencia, venta, ajuste o disposición trazable. | BPM-FAR-002/003 | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-INV-002` **Estado vendible** | Existencia en cuarentena/bloqueada/no vendible no se considera stock disponible para venta. | BPM-FAR-002 + seguridad sanitaria | `DOM` | Cadena/establecimiento según aplique |
| `RN-INV-003` **Factura no crea stock** | Registrar una factura de proveedor no incrementa inventario por sí sola. | Procure-to-Pay | `ERP/DOM` | Cadena/establecimiento según aplique |
| `RN-INV-004` **Recepción no implica vendible** | La recepción física puede quedar en cuarentena/observada hasta conformidad aplicable. | BPM-FAR-002 | `DOM` | Cadena/establecimiento según aplique |
| `RN-INV-005` **Lote y origen** | El lote recibido debe conservar vínculo con recepción/origen. | Trazabilidad farmacéutica | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-INV-006` **No doble reserva** | La concurrencia no puede reservar/vender dos veces la misma disponibilidad. | Integridad | `DOM/TEC` | Cadena/establecimiento según aplique |
| `RN-INV-007` **FEFO configurable** | FEFO se aplica solo cuando la política del producto/almacén lo defina; no se asume como obligación universal. | Benchmark WMS | `CFG/ERP` | Cadena/establecimiento según aplique |
| `RN-INV-008` **Ajuste autorizado** | Todo ajuste de inventario exige motivo y actor; umbrales/aprobaciones son configurables. | Control interno | `DOM/AUD/CFG` | Cadena/establecimiento según aplique |

### 2.4. TRF

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-TRF-001` **Stock en tránsito** | El despacho de transferencia disminuye disponible del origen y crea stock en tránsito; no aumenta disponible del destino hasta recepción. | BPM-FAR-003 | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-TRF-002` **Recepción explícita** | El destino debe confirmar recepción; no se infiere por el despacho. | BPM-FAR-003 | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-TRF-003` **Diferencia documentada** | Faltantes, sobrantes, daño o discrepancia de lote requieren incidencia/motivo y actor. | BPM-FAR-003 | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-TRF-004` **Stock bloqueado** | No se transfiere stock bloqueado como operación ordinaria salvo proceso autorizado específico. | BPM-FAR-003 | `DOM/AUD` | Cadena/establecimiento según aplique |

### 2.5. PRE

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-PRE-001` **Precio vigente** | El POS usa una versión de precio vigente para su ámbito/canal y conserva evidencia del precio aplicado. | BPM-FAR-005 / DIGEMID Observatorio | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-PRE-002` **Promoción no vence controles** | Una promoción/descuento no puede volver vendible un lote bloqueado ni eliminar requisito de receta. | BPM-FAR-005 / DIGEMID Observatorio | `DOM/NORM` | Cadena/establecimiento según aplique |
| `RN-PRE-003` **Descuento manual** | Descuentos manuales requieren permiso/motivo según política configurable. | BPM-FAR-005 / DIGEMID Observatorio | `DOM/AUD/CFG` | Cadena/establecimiento según aplique |
| `RN-PRE-004` **Observatorio mensual** | La capacidad de reporte debe soportar periodicidad mensual para farmacias/boticas privadas conforme a información vigente publicada por DIGEMID. | BPM-FAR-005 / DIGEMID Observatorio | `NORM` | Cadena/establecimiento según aplique |

### 2.6. POS

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-POS-001` **Identificación de operación** | Toda venta identifica establecimiento, terminal/caja, turno, operador y momento. | Retail/auditoría | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-POS-002` **CPE separado del pago** | Pago, venta y estado tributario del CPE son estados relacionados pero independientes. | SUNAT + DDD | `FISCAL/DOM` | Cadena/establecimiento según aplique |
| `RN-POS-003` **Boleta consumidor final** | La boleta electrónica se utiliza para operaciones con consumidores finales conforme a reglas SUNAT. | SUNAT CPE | `FISCAL` | Cadena/establecimiento según aplique |
| `RN-POS-004` **Factura adquirente** | La factura se emite conforme a requisitos del adquirente y reglas SUNAT aplicables. | SUNAT CPE | `FISCAL` | Cadena/establecimiento según aplique |
| `RN-POS-005` **Nota de crédito** | La nota de crédito electrónica se vincula a factura/boleta previa para anulaciones, descuentos, bonificaciones, devoluciones u otros supuestos admitidos. | SUNAT Nota de Crédito | `FISCAL` | Cadena/establecimiento según aplique |
| `RN-POS-006` **Idempotencia CPE** | El reintento técnico no debe crear múltiples comprobantes/correlativos para una misma intención de emisión. | Integridad de integración | `DOM/TEC` | Cadena/establecimiento según aplique |
| `RN-POS-007` **Envío SEE** | El plazo/mecanismo de envío del SEE del contribuyente debe parametrizarse conforme a la regla SUNAT vigente; actualmente SUNAT publica hasta tres días calendario para factura y nota vinculada. | SUNAT SEE del Contribuyente | `FISCAL/CFG` | Cadena/establecimiento según aplique |
| `RN-POS-008` **Cierre no borra pendientes** | Cerrar turno no elimina CPE/postings pendientes de integración. | BPM-FAR-007/012 | `DOM` | Cadena/establecimiento según aplique |
| `RN-POS-009` **Diferencia de caja** | Faltante/sobrante se registra y clasifica; no se compensa silenciosamente. | Retail control interno | `DOM/AUD` | Cadena/establecimiento según aplique |

### 2.7. DSP

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-DSP-001` **Proceso de dispensación separado** | La dispensación se modela como proceso farmacéutico separado de la venta/cobro. | BPM-FAR-004 + marco farmacéutico | `NORM/DOM` | Cadena/establecimiento según aplique |
| `RN-DSP-002` **Receta pendiente de validación** | Registrar una receta no significa que esté validada o atendida. | Proceso dispensación | `DOM` | Cadena/establecimiento según aplique |
| `RN-DSP-003` **No técnico bajo receta** | El personal técnico no puede confirmar el acto de dispensación bajo receta. | D.S. 015-2025-SA | `NORM` | Cadena/establecimiento según aplique |
| `RN-DSP-004` **No alternativas por técnico** | El personal técnico no puede ofrecer alternativas al medicamento prescrito. | D.S. 015-2025-SA | `NORM` | Cadena/establecimiento según aplique |
| `RN-DSP-005` **Receta con datos mínimos** | La validación de receta debe aplicar los datos mínimos exigibles según tipo de receta/norma vigente. | Normativa de prescripción aplicable | `NORM/CFG` | Cadena/establecimiento según aplique |
| `RN-DSP-006` **Vigencia por tipo** | No existe un plazo universal de receta; la vigencia se resuelve por tipo/clasificación y norma aplicable. | D.S. 023-2001-SA y otras | `NORM/DOM` | Cadena/establecimiento según aplique |
| `RN-DSP-007` **Dispensación parcial** | La dispensación parcial solo se admite cuando la regla aplicable al tipo de receta lo permita y debe conservar saldo/histórico. | D.S. 023-2001-SA para supuestos aplicables | `NORM/DOM` | Cadena/establecimiento según aplique |

### 2.8. CTL

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-CTL-001` **Receta especial por listas** | Para medicamentos con sustancias de las Listas II A, III A, III B y III C se utiliza receta especial conforme al D.S. 023-2001-SA. | D.S. 023-2001-SA, arts. 23 y ss. | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-002` **Vigencia tres días** | La receta especial indicada para esas listas tiene vigencia de tres días desde la expedición. | D.S. 023-2001-SA, art. 23 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-003` **Retención receta especial** | Una receta especial atendida debe quedar retenida/archivada por el establecimiento dispensador conforme a las reglas del art. 30. | D.S. 023-2001-SA, art. 30 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-004` **Archivo dos años** | La copia de receta especial atendida queda archivada en el establecimiento dispensador por dos años; otras recetas de listas señaladas por el art. 36 también tienen retención/archivo por dos años. | D.S. 023-2001-SA, arts. 30 y 36 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-005` **Datos de adquirente** | En la atención de receta especial aplicable se registra cantidad dispensada y datos/firma del adquirente conforme al art. 30. | D.S. 023-2001-SA, art. 30 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-006` **No receta >3 días** | Para los supuestos del art. 31 no se atienden recetas con más de tres días, enmendaduras o sospecha/evidencia de adulteración/falsificación. | D.S. 023-2001-SA, art. 31 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-007` **Responsabilidad QF** | El QF regente es responsable por adquisición, almacenamiento, custodia, dispensación y control de sustancias/medicamentos comprendidos en el reglamento. | D.S. 023-2001-SA, art. 28 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-008` **Registro de existencias** | Los establecimientos que manejan sustancias/medicamentos fiscalizados están obligados a registrar existencias y contabilidad relativa a consumo conforme al reglamento. | D.S. 023-2001-SA, arts. 40-41 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-009` **Concordancia física-registro** | Las existencias deben guardar conformidad con saldos de libros/control correspondientes. | D.S. 023-2001-SA, art. 41 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-010` **Faltante/excedente** | Faltantes/excedentes deben tratarse como incidencia regulatoria; el propietario y DT/regente tienen responsabilidades según art. 42. | D.S. 023-2001-SA, art. 42 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-011` **Falsificación comunicación** | Si se determina receta adulterada/falsificada, el regente la retiene y comunica a la autoridad dentro de las 48 horas de conocido el hecho según art. 35. | D.S. 023-2001-SA, art. 35 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-012` **Promoción restringida** | Está prohibida la promoción de medicamentos con sustancias de las listas señaladas por el art. 38. | D.S. 023-2001-SA, art. 38 | `NORM` | Cadena/establecimiento según aplique |
| `RN-CTL-013` **Balance por sujeto/lista** | La generación de balance trimestral se aplica solo a los sujetos/listas obligados; no se universaliza a todo producto controlado sin validar alcance. | DIGEMID formatos fiscalización | `NORM/CFG` | Cadena/establecimiento según aplique |

### 2.9. DEV

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-DEV-001` **Nota no reingresa stock** | Emitir una nota de crédito no devuelve automáticamente el producto físico al stock vendible. | SUNAT + seguridad sanitaria | `FISCAL/DOM` | Cadena/establecimiento según aplique |
| `RN-DEV-002` **Devolución separada** | Devolución comercial, reembolso, regularización fiscal y disposición sanitaria son subprocesos distintos. | BPM-FAR-008 | `DOM` | Cadena/establecimiento según aplique |
| `RN-DEV-003` **Producto devuelto evaluado** | El producto recibido de cliente entra a estado no vendible/cuarentena hasta decisión aplicable cuando corresponda. | BPM-FAR-008 / BPA con alcance a validar | `DOM/NORM_PARCIAL` | Cadena/establecimiento según aplique |
| `RN-DEV-004` **Histórico no destructivo** | Una devolución no edita la venta original; crea transacciones vinculadas. | Integridad fiscal/retail | `DOM/AUD` | Cadena/establecimiento según aplique |

### 2.10. RCL

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-RCL-001` **Bloqueo por lote** | Una alerta que afecte un lote debe permitir bloqueo operativo a nivel de lote. | Alertas DIGEMID / BPM-FAR-010 | `NORM/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-RCL-002` **Propagación omnicanal** | El bloqueo se propaga a POS, e-commerce y reservas del alcance afectado. | Alertas DIGEMID / BPM-FAR-010 | `NORM/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-RCL-003` **Evidencia por local** | La inmovilización física se confirma por establecimiento y cantidad. | Alertas DIGEMID / BPM-FAR-010 | `NORM/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-RCL-004` **Conciliación de recall** | El cierre requiere conciliar cantidad afectada, localizada y destino final. | Alertas DIGEMID / BPM-FAR-010 | `NORM/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-RCL-005` **Historia de ventas** | La trazabilidad debe permitir localizar movimientos/ventas históricas del lote sin alterar esas transacciones. | Alertas DIGEMID / BPM-FAR-010 | `NORM/DOM/AUD` | Cadena/establecimiento según aplique |

### 2.11. FVG

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-FVG-001` **Independencia de venta** | Un reporte de farmacovigilancia/tecnovigilancia puede registrarse aunque la venta no sea de la cadena. | DIGEMID formatos / BPM-FAR-011 | `NORM/DOM` | Cadena/establecimiento según aplique |
| `RN-FVG-002` **Datos protegidos** | Los datos de paciente/reportante/evento están sujetos a privacidad y acceso por finalidad. | Ley 29733 / DS 016-2024-JUS | `NORM/SEC` | Cadena/establecimiento según aplique |
| `RN-FVG-003` **Trazabilidad de envío** | La notificación externa conserva fecha, canal, responsable e identificador/evidencia cuando exista. | BPM-FAR-011 | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-FVG-004` **Plazo por sujeto/gravedad** | Los plazos regulatorios se resuelven por rol del sujeto, gravedad y norma vigente; no se hardcodea un plazo global sin validar obligación de la cadena. | D.S. 016-2011-SA / Manual BPFV | `NORM/CFG` | Cadena/establecimiento según aplique |
| `RN-FVG-005` **Actualización no borra** | La complementación de un reporte conserva versiones/histórico. | Buenas prácticas/trazabilidad | `DOM/AUD` | Cadena/establecimiento según aplique |

### 2.12. ERP

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-ERP-001` **Three-way match configurable** | La conciliación OC-recepción-factura es una política ERP recomendada/configurable, no una obligación sanitaria. | BPM-FAR-006/012 + benchmark ERP | `ERP/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-ERP-002` **Posting idempotente** | Una misma clave de integración no produce dos postings válidos. | BPM-FAR-006/012 + benchmark ERP | `ERP/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-ERP-003` **Venta vs posting** | El estado retail de una venta es independiente del estado de contabilización ERP. | BPM-FAR-006/012 + benchmark ERP | `ERP/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-ERP-004` **Reversa trazable** | Correcciones a operaciones contabilizadas se realizan con ajuste/reversa, no edición destructiva. | BPM-FAR-006/012 + benchmark ERP | `ERP/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-ERP-005` **Cierre de caja vs contable** | Cerrar caja/tienda no equivale a cierre contable mensual. | BPM-FAR-006/012 + benchmark ERP | `ERP/DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-ERP-006` **Factura proveedor sin stock** | La factura de proveedor no es evento de inventario. | BPM-FAR-006/012 + benchmark ERP | `ERP/DOM/AUD` | Cadena/establecimiento según aplique |

### 2.13. OBS

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-OBS-001` **Reporte mensual** | El sistema debe soportar el reporte mensual de precios para farmacias/boticas privadas según la información vigente del Observatorio DIGEMID. | DIGEMID Indicador/Observatorio | `NORM` | Cadena/establecimiento según aplique |
| `RN-OBS-002` **Fuente reproducible** | Cada dato reportado debe poder relacionarse con la fuente/versión de precio utilizada. | Trazabilidad | `DOM/AUD` | Cadena/establecimiento según aplique |
| `RN-OBS-003` **Mecanismo por validar** | API/archivo/portal exacto no se fija hasta investigar el mecanismo operativo vigente para el sujeto. | Investigación pendiente | `POR_VALIDAR` | Cadena/establecimiento según aplique |

### 2.14. SEC

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-SEC-001` **Protección de datos** | El tratamiento de clientes identificados, recetas, farmacovigilancia y fidelización se sujeta a la Ley 29733 y su Reglamento vigente. | Ley 29733 + D.S. 016-2024-JUS | `NORM/SEC` | Cadena/establecimiento según aplique |
| `RN-SEC-002` **Banco de datos** | La organización deberá evaluar/gestionar la inscripción de bancos de datos personales que le corresponda; el sistema debe poder documentar las finalidades/categorías necesarias para esa gestión. | ANPD trámite vigente | `NORM/ORG` | Cadena/establecimiento según aplique |
| `RN-SEC-003` **Marketing separado** | El consentimiento/base para marketing no se infiere de una compra ni de una receta; debe modelarse separadamente cuando se implemente CRM. | D.S. 016-2024-JUS / ANPD | `NORM/SEC` | Cadena/establecimiento según aplique |
| `RN-SEC-004` **Mínimo privilegio** | El permiso comercial no concede acceso a receta/farmacovigilancia sin necesidad y autorización. | Privacidad por diseño | `SEC/DOM` | Cadena/establecimiento según aplique |
| `RN-SEC-005` **Competencia profesional** | Un permiso informático nunca habilita un acto que la norma reserva o prohíbe a un determinado perfil profesional. | D.S. 015-2025-SA | `NORM/SEC` | Cadena/establecimiento según aplique |
| `RN-SEC-006` **Menú no autoriza** | La visibilidad de una opción de navegación no sustituye la autorización del endpoint; esta se resuelve con identidad, permiso, ámbito y reglas contextuales. | ADR-010 / contrato navegación | `SEC/DOM` | Todas las aplicaciones |

### 2.15. AUD

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-AUD-001` **Actor y contexto** | Operaciones críticas conservan actor, establecimiento, recurso, momento y resultado. | Control interno | `AUD` | Cadena/establecimiento según aplique |
| `RN-AUD-002` **No sobrescritura** | Auditoría no se modifica mediante operaciones ordinarias del negocio. | Integridad | `AUD/SEC` | Cadena/establecimiento según aplique |
| `RN-AUD-003` **Exportación sensible** | Las exportaciones de recetas/FVG/datos masivos quedan auditadas. | Privacidad | `SEC/AUD` | Cadena/establecimiento según aplique |

### 2.16. INT

| Código | Regla | Fuente | Tipo | Alcance |
|---|---|---|---|---|
| `RN-INT-001` **Idempotencia externa** | Integraciones reintentables usan identificadores idempotentes. | Arquitectura | `TEC/DOM` | Cadena/establecimiento según aplique |
| `RN-INT-002` **Error reintentable** | Errores externos se clasifican como reintentables/definitivos para evitar bucles o pérdida de transacciones. | Arquitectura | `TEC/DOM` | Cadena/establecimiento según aplique |
| `RN-INT-003` **Contratos versionados** | Las integraciones evolucionan mediante contratos/versionado controlado. | Arquitectura | `TEC` | Cadena/establecimiento según aplique |

## 3. Reglas que NO deben hardcodearse universalmente

- `periodo_receta = 3 días` para todas las recetas: **incorrecto**. Los tres días corresponden a supuestos específicos de receta especial/controlados.
- `FEFO = obligatorio` para todo producto: **no demostrado**; queda como política configurable salvo fuente específica.
- `grave = 24 horas` para toda farmacia en farmacovigilancia: **no se aplicará** sin validar sujeto y obligación exacta.
- `todo producto devuelto = destrucción` o `todo producto devuelto = vendible`: ambos extremos son incorrectos sin política/evaluación aplicable.
- `factura proveedor = recepción`: incorrecto; son hechos distintos.
- `CPE aceptado = venta pagada`: son estados distintos.
- `Director Técnico = usuario administrador`: incorrecto; rol profesional y rol técnico de plataforma son dimensiones distintas.
- `offline = siempre habilitado`: requiere ADR, controles de conflicto, seguridad y fiscalidad.
