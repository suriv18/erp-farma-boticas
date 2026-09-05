# DOM-FAR-007 — Lenguaje Ubicuo

**Versión:** 0.1  
**Objetivo:** usar los mismos términos en negocio, requisitos, código, API y documentación dentro de cada Bounded Context.

## 1. Términos organizacionales

### Cadena

Conjunto empresarial/operativo de establecimientos administrados por la solución. No es sinónimo de `empresa legal`.

### Empresa Operadora

Persona jurídica/entidad que opera uno o más establecimientos dentro del sistema.

### Establecimiento Farmacéutico

Local autorizado para las actividades farmacéuticas que correspondan a su categoría. DIGEMID lo define dentro del Reglamento de Establecimientos Farmacéuticos. [REF-02]

### Director Técnico (DT)

Responsabilidad profesional regulatoria. **No significa** administrador del sistema.

### Químico Farmacéutico (QF)

Profesional con competencias y responsabilidades farmacéuticas según normativa/aplicación.

## 2. Catálogo

### Producto Regulado

Identidad del producto desde la perspectiva sanitaria/regulatoria: registro, condición de venta, forma, concentración, clasificación, etc.

### SKU

Unidad/presentación comercial administrada por retail. Es término comercial/de software; no sustituye al producto regulado.

### Registro Sanitario (RS)

Identificador/registro regulatorio aplicable al producto, con estado/vigencia.

### Condición de Venta

Clasificación regulatoria que determina cómo puede venderse/dispensarse un producto. DIGEMID mantiene un estándar específico. [REF-06]

### DCI / IFA

Denominación Común Internacional / Ingrediente Farmacéutico Activo según corresponda al contexto regulatorio. Evitar usar “genérico” como sinónimo técnico indiscriminado.

## 3. Inventario

### Lote

Conjunto identificado por número de lote/serie cuando corresponda, con trazabilidad y vencimiento.

### Posición de Inventario

Cantidad de un SKU/lote en una ubicación/estado concreto.

### Stock Disponible

Cantidad que puede reservarse/venderse después de considerar reservas y estados no vendibles.

### Cuarentena

Estado de inventario que impide su disponibilidad hasta evaluación/liberación.

### Bloqueo

Restricción operativa que impide usar/vender una unidad/lote/posición.

### Inmovilización

Separación/control de existencias afectadas por una medida sanitaria o de calidad.

### FEFO

First Expired, First Out: política de selección por vencimiento más próximo. En el proyecto es una política configurable, no una norma universal.

### Kardex

Historial de movimientos de inventario; no debe confundirse con la posición/saldo actual.

## 4. Retail

### Venta

Transacción comercial confirmada con líneas, precios, pagos y referencias operativas.

### POS

Punto de venta/sistema de caja retail.

### Turno de Caja

Período operativo de una caja/cajero para registrar ventas, medios de pago y arqueo.

### Arqueo

Comparación de valores esperados vs valores físicos/evidenciados antes del cierre.

### Devolución Comercial

Proceso de reversa/reembolso de una venta. No significa reingreso automático de la mercadería a stock vendible.

## 5. Farmacéutico

### Prescripción / Receta

Documento/orden clínica que contiene la indicación del prescriptor. En el código se utilizará un término consistente por contexto; `Prescripcion` puede ser el agregado y `receta` el documento/evidencia cuando esa distinción aporte claridad.

### Dispensación

Acto farmacéutico que incluye validación, análisis, preparación/selección, registro y entrega/información según buenas prácticas. No es sinónimo de cobrar. [REF-25]

### Expendio

Término regulatorio relacionado con la entrega/venta al consumidor según establecimiento; no debe usarse como nombre genérico para cualquier `Venta` del sistema sin contexto. Farmacias/boticas son establecimientos donde se dispensan y expenden productos al consumidor final. [REF-39][REF-41]

### Producto Controlado / Fiscalizado

Producto/sustancia sujeto a reglas especiales. La categoría exacta debe provenir de clasificación regulatoria y no de una etiqueta manual genérica.

### Receta Especial

Tipo de receta aplicable a determinados productos/listas controladas. No toda receta de producto controlado tiene idénticas reglas.

## 6. Fiscal

### CPE

Comprobante de Pago Electrónico. No es la venta misma.

### Boleta Electrónica

Comprobante destinado principalmente a consumidor final según SUNAT. [REF-13]

### Factura Electrónica

Comprobante con reglas/efectos tributarios propios según SUNAT.

### Nota de Crédito

Documento relacionado con comprobante previo para anulaciones, descuentos, bonificaciones, devoluciones u otros supuestos. [REF-27]

### CDR / Respuesta Fiscal

Evidencia/estado de recepción o validación según el sistema de emisión aplicable. No usar códigos externos sin normalización.

## 7. Seguridad del producto

### Alerta Sanitaria

Comunicación oficial de riesgo/observación emitida por autoridad o fuente válida.

### Recall / Retiro del Mercado

Proceso de retiro/inmovilización de producto/lote afectado. DIGEMID mantiene consulta de retiros con situación. [REF-30]

### Lote Afectado

Lote dentro del alcance documentado del caso de recall.

## 8. Farmacovigilancia

### SRAM

Sospecha de Reacción Adversa a Medicamentos.

### NotiMED

Mecanismo electrónico de DIGEMID/CENAFyT para notificación de sospechas de reacciones adversas a medicamentos y otros productos farmacéuticos. [REF-33]

### Reporte de Seguridad

Agregado interno de la solución que organiza la información antes/durante el proceso de notificación; no pretende sustituir la terminología oficial de NotiMED.

## 9. ERP

### Posting

Publicación controlada de hechos operativos hacia contabilidad/ERP. Es término técnico/ERP, no una figura sanitaria.

### Cuenta por Pagar (CxP)

Obligación financiera derivada de documento/aceptación correspondiente. No es una recepción de stock.

### Three-way Match

Conciliación Orden de Compra–Recepción–Factura. Política ERP, no obligación sanitaria universal.

## 10. Términos que no deben usarse como sinónimos

| No confundir | Con |
|---|---|
| Producto Regulado | SKU |
| Lote | Stock |
| Stock | Kardex |
| Venta | Dispensación |
| Venta | CPE |
| Devolución | Reingreso a stock |
| Nota de Crédito | Devolución física |
| Recall | Devolución comercial |
| Director Técnico | Administrador TI |
| Receta | Autorización informática |
| Reporte de farmacovigilancia | Reclamo comercial |
| Cierre de caja | Cierre contable |
| Precio publicado | Costo contable |
