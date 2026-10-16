# Comparativa funcional con el mercado

Fecha de consulta: 10 de octubre de 2026. Proyecto: ERP Botica.

## 1. Resultado

**No hay evidencia suficiente para afirmar que el proyecto ya cubre una operación comparable a Inkafarma, Mifarma u otras cadenas.** Existe una base parcial de POS/ERP con catálogo, compras, inventario, ventas, caja, organización y seguridad. Faltan procesos completos de dispensación, clientes/fidelización, fiscalidad, promociones, pagos integrados, logística y canal digital.

La comparación distingue:

- **Capacidad pública observada:** información accesible en páginas oficiales de las cadenas. No implica haber probado una compra ni conocer sus sistemas internos.
- **Referencia de plataforma:** capacidades documentadas por proveedores ERP/POS. No significa que una cadena concreta use ese producto.
- **Estado del proyecto:** código y DDL revisados; una tabla o una pantalla simulada no cuenta como proceso terminado.
- **Cumplimiento normativo:** exige requisitos aplicables y pruebas propias. No se deduce de parecerse a una tienda online.

Inkafarma y Mifarma se presentan dentro del mismo grupo en la información corporativa de InRetail. Son referencias comerciales distintas, pero no deben contarse como prueba de dos arquitecturas internas independientes. [InRetail — unidades de negocio](https://www.inretail.pe/es/nuestra-empresa/unidades-de-negocio).

## 2. Evidencias públicas de cadenas

### Inkafarma

La web presenta búsqueda, categorías de surtido, selección de dirección, pedidos, acceso de cliente, aplicación, programa de beneficios y acceso a comprobantes. Esto sirve como referencia de experiencia comercial y autoservicio, sin revelar el ERP interno. [Sitio oficial](https://inkafarma.pe/).

Inkaclub comunica acumulación/canje de beneficios y descuentos. El proyecto no tiene un flujo ejecutable de fidelización equivalente; tener carpetas CRM y tablas no acredita acumulación, reversión o conciliación de beneficios. [Programa Inkaclub](https://inkafarma.pe/programa-inkaclub).

Una ficha de producto consultada identifica presentación, condición de receta y alternativas de entrega/retiro. La comparación se limita a esos elementos publicados y no evalúa eficacia clínica. [Ficha oficial consultada](https://inkafarma.pe/producto/etoxblam-120-mg-tabletas-recubiertas/011901).

### Mifarma

El sitio ofrece catálogo digital y acceso a servicios comerciales y beneficios. [Sitio oficial](https://www.mifarma.com.pe/).

La página de retiro en botica describe compra digital y recojo; indica presentar receta cuando corresponde antes de entregar el producto. Esto exige coordinar pedido, disponibilidad y validación de entrega. El ERP revisado no dispone de ese proceso completo. [Retiro en botica](https://www.mifarma.com.pe/atencion-clientes/retiro-en-botica).

Monedero describe puntos, saldo y beneficios/cupones. Una implementación comparable necesitaría movimientos, reglas, idempotencia, reversos y trazabilidad, además de una pantalla de saldo. Estos requisitos son inferencias de diseño del análisis, no descripción de tablas internas de Mifarma. [Monedero](https://www.mifarma.com.pe/monedero).

La ficha consultada expone presentaciones comerciales y condiciones de compra. Es una referencia adicional para no reducir caja/blíster a texto sin relación con cantidad y precio. [Ficha oficial consultada](https://www.mifarma.com.pe/producto/aci-tip-800-mg-40mg-comprimidos-masticables/422016).

### Boticas Perú

El sitio público presenta catálogo, campañas/packs, entrega y acceso a comprobantes. Son capacidades visibles que conviene considerar al definir alcance comercial; no permiten concluir qué motor de inventario, promociones o facturación utiliza. [Sitio oficial](https://www.boticasperu.pe/).

### Farmacia Universal

Su guía de compra describe carrito, entrega o recojo, adjunto de receta, selección de comprobante y alternativas de pago. Es un flujo público útil para comparar el recorrido de un pedido, aunque no se efectuó ninguna transacción. [Cómo comprar](https://www.farmaciauniversal.com/como-comprar).

## 3. Referencias ERP/POS para procesos internos

Las webs de cadenas muestran la experiencia del cliente, pero no permiten inspeccionar cierres, costos, reposición o gestión de lotes. Para estos procesos se consultaron fuentes oficiales de plataformas.

| Referencia | Capacidad documentada relevante | Uso en esta revisión |
| --- | --- | --- |
| [LS Central for pharmacies](https://www.lsretail.com/products/ls-central-for-pharmacies) | POS de farmacia, prescripciones, fidelización, promociones, inventario entre tiendas, reposición y omnicanalidad | Referencia de amplitud funcional; no se atribuye su uso a las cadenas peruanas. |
| [Documentación LS Pharmacy](https://help.lsretail.com/lscentral260/Content/Product-Overview/LS-Pharmacy.htm) | Integración del trabajo farmacéutico con la operación comercial | Separar dispensación de un simple registro de venta. |
| [Dynamics 365 Commerce: operación offline](https://learn.microsoft.com/en-us/dynamics365/commerce/dev-itpro/pos-offline-functionality) | Base local y sincronización para continuidad del POS | Referencia técnica opcional según necesidades de conectividad; no prueba de que las cadenas revisadas operen igual. |
| [Odoo: FEFO](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/shipping_receiving/removal_strategies/fefo.html) | Estrategia de salida por fecha de retirada | Comparar configuración de prioridad y elegibilidad, sin confundir fecha de retirada con vencimiento. |
| [Odoo: vencimientos](https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/product_management/product_tracking/expiration_dates.html) | Seguimiento por lotes/series y fechas asociadas | Referencia para alertas y trazabilidad, no sustituto de reglas sanitarias locales. |

No se usaron reseñas de terceros como prueba de capacidades técnicas de esas plataformas.

## 4. Matriz de brechas del proyecto

“Parcial” significa que se encontró implementación, pero hay límites conocidos. “Diseño” significa que hay tablas o documentación, sin flujo completo encontrado. Las referencias del mercado remiten a las evidencias anteriores; las evidencias del repositorio están en la [revisión integral](2026-10-10-revision-integral-modulos.md) y el [análisis de catálogo](2026-10-10-analisis-modelo-catalogo.md).

| Capacidad | Referencia | Estado del ERP | Brecha concreta | Prioridad |
| --- | --- | --- | --- | --- |
| Catálogo y búsqueda | Cadenas consultadas | Parcial | Completar clasificación, vigencia de barcode y datos comerciales; validar rendimiento real | Alta |
| Variantes y presentaciones | Fichas comerciales; documentación de unidades | Parcial | Conversión caja/blíster/unidad no aplicada de extremo a extremo | Alta |
| Venta presencial y ticket | Plataformas POS | Parcial | Ticket interno; impuestos, precio autorizado y controles sanitarios pendientes | Crítica |
| Caja y turnos | Operación POS | Parcial | Validar conciliación, diferencias y escenarios operativos completos | Alta |
| Receta/dispensación | Mifarma y Farmacia Universal; LS Pharmacy | Diseño | No hay flujo ejecutable que obligue receta y evidencia según producto | Crítica |
| Restricciones sanitarias | Datos del catálogo y reglas aplicables | Parcial | Estado regulatorio/condición no gobiernan confirmación de venta | Crítica |
| Lotes y FEFO | Odoo; plataformas farmacia | Parcial | Coherencia SKU–lote, fecha local y artículos sin lote pendientes | Alta |
| Costos y margen | Necesidad interna del ERP | Parcial | Costo de recepción no llega a posición/movimiento de stock | Crítica |
| Compra y recepción | Necesidad interna del ERP | Parcial | Unidades, costos y casos de recepción deben cerrarse con inventario | Alta |
| Transferencias/reposición | LS Central | Diseño | Sin proceso completo encontrado de transferencia y reposición | Alta para cadena |
| Alertas/recall | Referencia farmacéutica | Diseño | No hay circuito ejecutable de bloqueo, alcance y resolución acreditado | Alta |
| Precios/promociones | Campañas públicas y plataformas | Diseño/parcial | Precio editable y de referencia; sin motor operativo de reglas | Alta |
| Clientes | Experiencia de cadenas | Placeholder/estructura | Sin maestro y procesos completos | Alta |
| Fidelización/monedero | Inkaclub/Monedero | Diseño/estructura | Sin acumulación/canje/reverso ejecutables | Media comercial |
| Pago multimedio | Guía de Farmacia Universal | Estructura | Sin integración/conciliación de medios de pago acreditada | Alta |
| Comprobantes electrónicos | Accesos públicos; SUNAT | Diseño | Sin emisión, respuesta y ciclo fiscal operativo encontrados | Crítica según obligación aplicable |
| Pedido online | Cadenas consultadas | Estructura | Sin flujo cliente–pedido–pago–reserva–entrega | Alta si entra en alcance |
| Retiro en botica | Mifarma/Farmacia Universal | No implementado como flujo | Falta coordinación de disponibilidad, preparación y entrega | Alta si entra en alcance |
| Delivery y seguimiento | Canales públicos consultados | Estructura | Sin despacho y estados integrados acreditados | Alta si entra en alcance |
| Continuidad offline | Dynamics; decisión de negocio | No acreditado | Preferencias locales o flags Store Edge no son venta/sincronización offline | Según conectividad |
| Permisos por local | Requisito multitienda del ERP | Parcial | Permisos aplanados pierden ámbito; OBS-001 | Crítica |
| Auditoría | Necesidad operativa del ERP | Diseño | Tablas sin escritura de auditoría de negocio encontrada | Alta |
| Indicadores | Gestión del ERP | Simulado/parcial | Dashboard estático y endpoint sin implementación encontrada | Media |
| RR. HH./finanzas | Alcance interno declarado | Estructura | No hay módulos ejecutables completos | Según alcance del ERP |

La prioridad “crítica” expresa riesgo del proyecto para su operación pretendida; no es una sanción legal ni un juicio sobre las cadenas comparadas.

## 5. Cumplimiento: lo que puede y no puede concluirse

Un ticket de venta interno no acredita emisión de boleta electrónica. La documentación de SUNAT describe comprobantes electrónicos y sus características; el proyecto requiere su ciclo fiscal específico y validaciones correspondientes. [SUNAT — boleta de venta electrónica](https://cpe.sunat.gob.pe/tipos_de_comprobantes/boleta).

Las clasificaciones oficiales de productos son una fuente para el catálogo, pero su carga debe conectarse con reglas, evidencia y actualización. [DIGEMID — estándares de productos farmacéuticos](https://www.digemid.minsa.gob.pe/webDigemid/estandares-de-productos-farmaceuticos/).

Para determinar cumplimiento deben fijarse tipo de establecimiento, autorizaciones, surtido, condición de venta, canal y obligaciones fiscales concretas. Esta revisión no inspeccionó esos documentos operativos ni un despliegue certificado; **no emite una certificación de cumplimiento sanitario, tributario o de protección de datos**.

Tampoco se debe inferir que todo artículo visible en una web puede venderse desde cualquier local físico. El canal, la entidad que comercializa y el régimen del artículo son dimensiones distintas, coherentes con la separación propuesta en ADR-011.

## 6. Hoja de ruta derivada de la comparación

### Etapa 1: integridad de la operación presencial

Cerrar autorización por ámbito, aislamiento de sesión/caché, contraseña obligatoria, receta/admisibilidad, stock/costo/unidades, impuestos y comprobantes aplicables. Completar auditoría. Validar venta simultánea, anulaciones, cierres y recuperación ante fallos.

**Salida verificable:** compra → recepción con costo → stock por lote/unidad → venta autorizada → comprobante aplicable → anulación/devolución → conciliación, probado sin mocks de las APIs de negocio.

### Etapa 2: operación de varias boticas

Implementar transferencias, reposición, políticas por local, precios y promociones, clientes, pagos y conciliación. Probar bloqueo de establecimiento y alcance de usuarios.

**Salida verificable:** existencias y decisiones coherentes entre locales, trazabilidad de movimientos y ausencia de ventas sobre stock no disponible.

### Etapa 3: experiencia comercial digital

Implementar pedidos, reservas con vencimiento, retiro/delivery, recetas según canal, comunicación y fidelización. Integrar devolución y reverso de beneficios desde el inicio.

**Salida verificable:** pedido completo y cancelación/reembolso reconciliados con stock, pago y beneficios.

### Etapa 4: escalabilidad y continuidad según objetivos

Medir carga y observabilidad, recuperación, respaldo/restauración y continuidad de conectividad. Si se incorpora offline, precisar operaciones permitidas y resolución de conflictos; no habilitar por defecto decisiones que exijan evidencia online actualizada.

**Salida verificable:** objetivos medidos de respuesta, recuperación y conciliación; escenarios de falla reproducidos.

## 7. Límites de esta exploración

Se consultaron páginas públicas oficiales, fichas, guías y documentación técnica. No se iniciaron sesiones de clientes, no se enviaron recetas o reclamos y no se hicieron compras. Algunas páginas dinámicas pueden variar por dirección, disponibilidad o campaña; no se tomaron precios ni promociones puntuales como requisito fijo.

Las capacidades no observadas públicamente se mantienen como desconocidas respecto de las cadenas. Los vacíos del proyecto sí se apoyan en el inventario y en los flujos de código examinados. No se asignó un porcentaje de “equivalencia con Inkafarma” porque ocultaría diferencias de alcance y de verificación.

El criterio de avance propuesto es **proceso implementado + invariantes aplicadas + prueba integral + evidencia operativa**, no cantidad de módulos, tablas o pantallas.

