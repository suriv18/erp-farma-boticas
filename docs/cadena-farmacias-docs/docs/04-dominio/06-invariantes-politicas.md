# DOM-FAR-006 — Invariantes y Políticas de Dominio

**Versión:** 0.1  
**Estado:** Catálogo inicial trazable

## 1. Convenciones

- `NORM`: sustentada directamente por fuente oficial/normativa.
- `FISCAL`: regla de comprobantes/tributaria.
- `DOM`: decisión necesaria para coherencia del dominio.
- `CFG`: política configurable de la cadena.
- `SEC`: seguridad/privacidad.
- `AUD`: trazabilidad.
- `POR_VALIDAR`: no cerrar todavía.

## 2. Organización

### `INV-ORG-001` — Establecimiento autorizado (`NORM`)

Una operación farmacéutica regulada se ejecuta en un establecimiento habilitado según el alcance de su autorización/configuración. DIGEMID indica que los establecimientos dedicados a estas actividades requieren autorización sanitaria previa. [REF-02]

### `INV-ORG-002` — Responsabilidad profesional temporal (`DOM/NORM`)

La asignación de Director Técnico/QF conserva período de vigencia; cambiar de profesional no borra quién era responsable en una operación histórica.

### `INV-ORG-003` — Rol técnico ≠ rol de plataforma (`DOM/SEC`)

Ser Director Técnico no implica ser administrador de sistemas y viceversa.

## 3. Catálogo

### `INV-CAT-001` — Condición de venta desde fuente (`NORM/DOM`)

No se infiere la condición de venta desde marca, DCI o categoría comercial. Debe provenir de una fuente regulatoria/maestra controlada. [REF-06][REF-07]

### `INV-CAT-002` — Registro sanitario histórico (`NORM/AUD`)

Un cambio de estado/vigencia regulatoria se versiona. Las operaciones históricas conservan la versión utilizada.

### `INV-CAT-003` — Estado no comercializable bloquea nueva salida (`NORM`)

No se permite comercializar/dispensar productos con registro vencido, suspendido o cancelado, salvo excepción formal aplicable como agotamiento autorizado de stock cuando corresponda. [REF-40]

### `INV-CAT-004` — SKU no redefine reglas sanitarias (`DOM`)

El SKU comercial no puede modificar localmente la condición de venta del producto regulado.

## 4. Inventario

### `INV-INV-001` — Movimiento trazable (`DOM/AUD`)

Toda alteración de cantidad debe originarse en un movimiento/documento/causa identificable.

### `INV-INV-002` — Lote vencido no vendible (`NORM`)

Un lote cuya fecha de vencimiento ya se alcanzó no puede asignarse a venta/dispensación. DIGEMID prohíbe la comercialización de productos vencidos. [REF-02][REF-38]

### `INV-INV-003` — Lote bloqueado/recalled no vendible (`NORM/DOM`)

Un lote en cuarentena, bloqueo o recall no puede formar parte de disponibilidad vendible hasta liberación autorizada.

### `INV-INV-004` — No sobreconsumo en stock autoritativo (`DOM`)

En operación online autoritativa, una reserva/consumo no puede superar cantidad vendible disponible.

**Nota:** la futura operación offline puede requerir una estrategia distinta y se encuentra `POR_VALIDAR`.

### `INV-INV-005` — Reserva con expiración/causa (`DOM`)

Una reserva debe tener dueño/causa y forma explícita de cumplimiento o liberación.

### `INV-INV-006` — Ajuste requiere motivo (`DOM/AUD`)

Ajustar stock sin causa/documento está prohibido.

### `INV-INV-007` — FEFO no universal (`CFG`)

La política de selección de lote es configurable; ninguna implementación debe asumir FEFO para todo SKU sin política/fuente aplicable.

## 5. Compras

### `INV-PRC-001` — Factura no crea stock (`DOM/ERP`)

Registrar factura del proveedor no incrementa inventario. El hecho físico es la recepción aceptada.

### `INV-PRC-002` — Recepción no crea por sí sola CxP final (`DOM/ERP`)

El compromiso financiero sigue las reglas del proceso Procure-to-Pay.

### `INV-PRC-003` — Diferencias no se borran (`AUD`)

Faltante, sobrante, daño o rechazo quedan registrados como diferencia, no como edición silenciosa de la recepción.

## 6. Precios

### `INV-PRI-001` — Precio reproducible (`DOM/AUD`)

Una venta cerrada debe explicar qué lista/promoción produjo el precio aplicado.

### `INV-PRI-002` — Vigencias no ambiguas (`DOM`)

No deberían coexistir dos precios publicados incompatibles para el mismo scope/clave/período, salvo una regla explícita de prioridad.

## 7. Venta/POS

### `INV-RET-001` — Turno válido (`DOM`)

Una venta presencial exige un turno/caja autorizado y abierto.

### `INV-RET-002` — Venta confirmada inmutable económicamente (`DOM/AUD`)

Correcciones se realizan con cancelación/reversa/devolución/documento vinculado; no editando importes de una venta cerrada.

### `INV-RET-003` — Venta ≠ CPE (`DOM/FISCAL`)

El estado comercial de la venta es independiente del estado fiscal del comprobante.

### `INV-RET-004` — Venta bajo receta requiere decisión farmacéutica (`NORM/DOM`)

Cuando la condición de venta exige receta, la venta no se confirma sin el flujo de prescripción/dispensación aplicable. DIGEMID recuerda la exigencia de receta para medicamentos bajo prescripción. [REF-39]

### `INV-RET-005` — Reintento idempotente (`DOM/TEC`)

Reintentar `ConfirmarVenta` con la misma clave no crea una segunda venta válida.

## 8. Prescripción y Dispensación

### `INV-DSP-001` — Competencia profesional (`NORM/SEC`)

El sistema no habilita mediante permisos informáticos un acto de dispensación prohibido al perfil del usuario.

### `INV-DSP-002` — Receta válida (`NORM`)

La dispensación bajo receta considera los datos mínimos, vigencia aplicable, integridad del documento y otras reglas de la norma. [REF-25][REF-39]

### `INV-DSP-003` — Condición de venta efectiva (`NORM/DOM`)

La decisión de exigir receta utiliza la condición de venta efectiva del producto, con evidencia de versión.

### `INV-DSP-004` — Dispensación histórica (`AUD`)

La rectificación/complementación conserva quién validó, cuándo y bajo qué datos.

### `INV-DSP-005` — Dispensación no fija precio (`DOM`)

El farmacéutico autoriza/realiza el acto sanitario; `BC-PRI/BC-RET` decide precio/promoción.

## 9. Controlados

### `INV-CTL-001` — Regla por clasificación/lista (`NORM`)

Las reglas de receta especial, vigencia, retención y balance se determinan por clasificación aplicable; no existe una regla única para todo “controlado”. [REF-26]

### `INV-CTL-002` — Tres días no global (`NORM`)

El plazo de tres días se aplica solo a los supuestos expresamente regulados; no se usa para toda receta.

### `INV-CTL-003` — Movimiento controlado append-only (`NORM/DOM/AUD`)

La trazabilidad de movimientos controlados no se reescribe destructivamente.

### `INV-CTL-004` — Balance reconciliable (`NORM/DOM`)

Existencia inicial + entradas - salidas ± ajustes debe explicar la existencia final, con excepciones documentadas.

## 10. CPE/Fiscal

### `INV-FIS-001` — Documento origen (`FISCAL`)

Una nota de crédito se vincula a un comprobante previo conforme al mecanismo de emisión aplicable. [REF-27]

### `INV-FIS-002` — Nota de crédito no repone inventario (`FISCAL/DOM`)

El efecto fiscal y el efecto físico del producto son procesos distintos.

### `INV-FIS-003` — Serie/número no duplicado (`FISCAL/DOM`)

No pueden existir dos documentos válidos con la misma identidad fiscal dentro del mismo emisor/tipo/serie/número.

### `INV-FIS-004` — Estado externo normalizado (`DOM/INT`)

Los códigos de un PSE se traducen a un modelo fiscal interno; no contaminan el resto de los contextos.

## 11. Devoluciones

### `INV-DEV-001` — Venta original preservada (`DOM/AUD`)

La devolución crea una transacción vinculada.

### `INV-DEV-002` — Producto físico en evaluación (`DOM/NORM_PARCIAL`)

Un producto devuelto no se convierte automáticamente en disponible. Se registra recepción/devolución y se resuelve disposición mediante política aplicable.

## 12. Recall

### `INV-RCL-001` — Alcance por producto/lote (`NORM/DOM`)

El caso debe poder referir producto/registro/lote y motivo, coherente con la información de retiros publicada por DIGEMID. [REF-30]

### `INV-RCL-002` — Bloqueo transversal (`DOM`)

Un lote afectado no debe seguir vendible por POS, reservas o canales integrados dentro del alcance del sistema.

### `INV-RCL-003` — Histórico intacto (`AUD`)

Ventas pasadas del lote no se borran; sirven para trazabilidad.

### `INV-RCL-004` — Cierre conciliado (`DOM/AUD`)

No se cierra el recall sin explicación de cantidades localizadas, inmovilizadas, devueltas/destruidas u otra disposición.

## 13. Farmacovigilancia

### `INV-FVG-001` — Venta opcional (`NORM/DOM`)

Un reporte puede existir sin `VentaId`. [REF-33]

### `INV-FVG-002` — Privacidad mínima (`SEC/NORM`)

Solo se recolectan/exponen datos personales necesarios para la finalidad y según permisos.

### `INV-FVG-003` — Complementación versionada (`AUD`)

Actualizar información no elimina la versión/fecha/reportante anterior cuando sea relevante a la trazabilidad.

## 14. ERP

### `INV-FIN-001` — Posting idempotente (`ERP/DOM`)

La misma clave de posting no produce dos registros financieros válidos.

### `INV-FIN-002` — Reversa no edición (`ERP/AUD`)

Un posting contabilizado se corrige por reversa/ajuste, no por actualización destructiva.

### `INV-FIN-003` — Cierre de caja ≠ cierre contable (`ERP/DOM`)

Son procesos diferentes.

## 15. Seguridad transversal

### `INV-SEC-001` — Tenant/empresa/establecimiento (`SEC`)

Conocer un UUID no otorga acceso; se valida ámbito.

### `INV-SEC-002` — Receta/FVG con privilegio mínimo (`SEC/NORM`)

Un rol de caja/marketing no recibe acceso clínico o sensible por defecto.

### `INV-AUD-001` — Operación crítica auditable (`AUD`)

Debe existir evidencia de actor, contexto, resultado, instante y correlación para operaciones regulatorias/económicas sensibles.
