# DOC-FAR-001 — Visión y Alcance

## 1. Nombre de trabajo

**Plataforma Integral de Gestión para Cadena de Farmacias**

No se utilizará todavía el nombre “ERP Farmacia” como denominación única porque el alcance incluye capacidades especializadas de retail, logística y operación farmacéutica que trascienden un ERP administrativo tradicional.

## 2. Problema de negocio

Una cadena de farmacias opera simultáneamente como:

- empresa multi-sucursal;
- retailer con POS y cajas;
- operador logístico con inventario distribuido;
- establecimiento farmacéutico sujeto a regulación sanitaria;
- organización financiera que compra, vende, paga, cobra y consolida resultados;
- canal potencialmente omnicanal (tienda física, web/app, retiro y delivery).

El riesgo de tratar todo como un único “módulo ERP” es mezclar responsabilidades y crear dependencias innecesarias entre procesos contables, operación de tienda, control farmacéutico y logística.

## 3. Objetivo general

Diseñar e implementar una plataforma modular que permita gestionar de extremo a extremo la operación de una cadena de farmacias, manteniendo trazabilidad de inventario y productos, cumplimiento regulatorio, continuidad del POS, control financiero y capacidad de crecimiento omnicanal.

## 4. Objetivos específicos

1. Centralizar maestros de empresas, establecimientos, productos, proveedores y clientes.
2. Controlar inventario por establecimiento, ubicación y lote, incluyendo vencimientos.
3. Soportar recepción, transferencias, ajustes, inventarios físicos, reposición y distribución.
4. Gestionar ventas POS, cajas, turnos, pagos, devoluciones y comprobantes electrónicos.
5. Aplicar controles farmacéuticos según condición de venta y receta.
6. Registrar responsables profesionales y Director Técnico por establecimiento cuando corresponda.
7. Controlar productos fiscalizados mediante flujos específicos cuando sean aplicables.
8. Integrar compras, proveedores, cuentas por pagar, tesorería y contabilidad.
9. Permitir precios, promociones y fidelización con reglas centralizadas y ejecución por tienda.
10. Preparar la plataforma para canales digitales, pedidos, retiro en tienda y delivery.
11. Proveer auditoría, seguridad y segregación de funciones.
12. Generar información regulatoria, operacional, comercial y financiera.

## 5. Alcance funcional inicial

### 5.1 Incluido en el análisis

- Organización y establecimientos.
- Autorización sanitaria y responsables profesionales como información del establecimiento.
- Catálogo maestro farmacéutico y retail.
- Proveedores y compras.
- Centros de distribución, almacenes y ubicaciones.
- Lotes, series cuando aplique, fechas de vencimiento y alertas.
- Inventario y movimientos.
- Transferencias entre locales.
- Reposición.
- POS y cajas.
- Ventas y devoluciones.
- Precios y promociones.
- Clientes y fidelización.
- Recetas y dispensación.
- Productos controlados.
- Retiro/recall y bloqueo de lotes/productos.
- Farmacovigilancia / tecnovigilancia como capacidad a delimitar.
- ERP financiero.
- Reportes y BI.
- Omnicanalidad.
- IAM y auditoría.

### 5.2 No se declara todavía como MVP

La inclusión en el mapa de capacidades **no significa** que todo deba implementarse en la primera versión. El MVP se definirá después del análisis de procesos y priorización.

## 6. Principios funcionales preliminares

1. **Una venta no debe poder evadir la condición de venta sanitaria del producto.** `NORM`
2. **El inventario farmacéutico debe mantener trazabilidad por lote y vencimiento cuando corresponda.** `NORM/DOM`
3. **Los establecimientos deben estar asociados a su autorización sanitaria vigente cuando aplique.** `NORM`
4. **El POS debe continuar operando frente a interrupciones de conectividad si el modelo de negocio exige continuidad.** `MKT/POR_VALIDAR`
5. **ERP y POS son capacidades distintas aunque se integren.** `DOM`
6. **El producto maestro debe diferenciar atributos farmacéuticos de atributos comerciales.** `DOM`
7. **Precio, promoción y margen no deben modificar reglas sanitarias de dispensación.** `DOM`
8. **Los movimientos físicos deben producir trazabilidad auditable.** `DOM/MKT`
9. **Las funciones del personal deben respetar perfiles y responsabilidades regulatorias.** `NORM/DOM`
10. **No se modelará una regla sanitaria como dato fijo sin fuente vigente.** `DOM`

## 7. Evidencia regulatoria principal

- Ley N.° 29459: regula productos farmacéuticos, dispositivos médicos y productos sanitarios.
- D.S. N.° 014-2011-SA y modificatorias: Reglamento de Establecimientos Farmacéuticos.
- D.S. N.° 015-2025-SA: modifica el artículo 43 y el Anexo 01 del Reglamento, incluyendo responsabilidades del personal técnico y Director Técnico.
- D.S. N.° 016-2011-SA: registro, control y vigilancia sanitaria de productos.
- D.S. N.° 023-2001-SA: productos/sustancias sujetos a fiscalización sanitaria.
- Directiva Sanitaria N.° 105-MINSA/2020/DIGEMID: buenas prácticas de dispensación; incluye recepción/validación, análisis, selección, registro, entrega e información al paciente.
- DIGEMID: Observatorio de Precios; las farmacias y boticas reportan precios de medicamentos.
- SUNAT: emisión de comprobantes electrónicos según obligación tributaria vigente.

## 8. Hipótesis que deben validarse con stakeholders

- Cantidad esperada de empresas, locales, almacenes y cajas.
- Necesidad real de POS offline por tienda.
- Existencia de centro de distribución propio.
- Gestión centralizada o descentralizada de compras.
- Si habrá e-commerce/app desde la primera versión.
- Si se comercializarán productos controlados.
- Si se manejarán fórmulas magistrales.
- Si el ERP contable será propio o integración con un ERP existente.
- Si la cadena utilizará una sola razón social o múltiples empresas.
- Modelo de fidelización.
- Política de devoluciones por tipo de producto.
