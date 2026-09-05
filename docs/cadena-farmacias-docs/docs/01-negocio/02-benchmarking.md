# DOC-FAR-002 — Benchmarking de Sistemas y Plataformas

## 1. Objetivo

Identificar capacidades implementadas actualmente en soluciones de retail, farmacia, ERP y supply chain para utilizarlas como evidencia de mercado (`MKT`) y fuente de ideas, **sin copiar automáticamente su modelo ni asumir que son requisitos regulatorios peruanos**.

## 2. Plataformas revisadas

### 2.1 LS Central for Pharmacies

Capacidades observadas:

- POS para medicamentos, retail y servicios en una misma transacción.
- Manejo de recetas en diferentes formatos.
- Búsqueda por atributos farmacéuticos como sustancia, ATC, concentración y clasificación.
- Control de vencimientos y recalls.
- Inventario en tiempo real por local.
- Transferencias entre tiendas.
- Reposición automática por producto/local.
- Promociones y fidelización.
- Permisos específicos para usuarios de farmacia.
- Integración con Business Central ERP.
- Pedidos en línea, delivery y click & collect.
- Servicios/citas.

**Conclusión:** confirma que en una farmacia de cadena conviven ERP, retail, farmacia e inventario especializado y que una separación modular es habitual.

### 2.2 Microsoft Dynamics 365 Commerce

Capacidades observadas:

- POS moderno.
- clientes, pricing y descuentos;
- pagos;
- pedidos y fulfillment;
- inventario;
- recibos;
- reporting;
- extensibilidad;
- soporte offline.

La documentación vigente describe un modo offline en el que el POS usa una base local cuando el componente central no está disponible y sincroniza transacciones posteriormente.

**Conclusión:** la continuidad de operación de tienda debe evaluarse como requisito arquitectónico propio, especialmente para sucursales con conectividad variable.

### 2.3 Oracle Retail Merchandising Foundation

Capacidades observadas:

- item management;
- replenishment;
- purchasing;
- inventory;
- transferencias;
- sales audit;
- seguimiento financiero.

**Conclusión:** en retail de escala, merchandising, compras, inventario, transferencias y auditoría de ventas forman un núcleo separado del POS.

### 2.4 SAP Retail / S/4HANA

Capacidades observadas:

- stock por niveles organizacionales y ubicaciones;
- movimientos de mercancía con documentos/audit trail;
- integración del movimiento físico con valoración/contabilidad;
- procurement y recepción;
- análisis por lotes y shelf-life.

**Conclusión:** soporta la separación entre inventario físico, valoración financiera y contabilidad.

### 2.5 Odoo Inventory

Capacidades observadas:

- lotes y números de serie;
- fechas de vencimiento/alerta/remoción;
- estrategias FIFO/LIFO/FEFO;
- FEFO basado en fecha próxima a expirar.

**Conclusión:** FEFO es una práctica de supply chain útil para productos con vencimiento; su aplicación concreta en el proyecto será una política de negocio/configuración y no se etiquetará automáticamente como obligación legal.

## 3. Capacidades comunes identificadas

| Capacidad | LS Central | D365 Commerce | Oracle Retail | SAP Retail | Odoo | Proyecto |
|---|---:|---:|---:|---:|---:|---:|
| POS | Sí | Sí | Integrable | Integrable | Sí | Sí |
| Offline POS | Depende despliegue | Sí | Depende solución | Depende | Limitado/depende | Por validar |
| Inventario multi-local | Sí | Sí | Sí | Sí | Sí | Sí |
| Lotes/vencimientos | Sí | Depende configuración | Sí | Sí | Sí | Sí |
| Transferencias | Sí | Sí | Sí | Sí | Sí | Sí |
| Reposición | Sí | Sí | Sí | Sí | Reglas | Sí |
| Recetas/dispensación | Sí | No nativo específico | No nativo específico | No nativo específico | No nativo específico | Sí |
| Promociones | Sí | Sí | Sí | Sí | Sí | Sí |
| Fidelización | Sí | Sí | Integrable | Integrable | Sí | Sí |
| ERP financiero | Business Central | Dynamics Finance | Integrable | S/4HANA | Sí | Sí/integrable |
| Omnicanal | Sí | Sí | Sí | Sí | Integrable | Fase a definir |

## 4. Lecciones que sí conviene adoptar

1. **Separar el producto farmacéutico del SKU/ítem comercial.**
2. **Separar inventario lógico de lote físico.**
3. **Centralizar precios/promociones pero ejecutar transacciones en tienda.**
4. **Diseñar transferencias como proceso con despacho y recepción, no como simple actualización de stock.**
5. **Modelar reposición como política configurable.**
6. **Usar trazabilidad de movimientos y sales audit.**
7. **Separar receta/dispensación de venta POS, aunque terminen en una misma transacción de cobro.**
8. **Preparar una estrategia de continuidad/offline para POS si se valida el requisito.**
9. **Mantener datos regulatorios del producto separados de precio/costos/promociones.**
10. **Evitar que el ERP financiero gobierne directamente las reglas de dispensación.**

## 5. Lo que no se debe copiar sin análisis

- Modelos de receta de otros países.
- Sistemas de seguros/reembolso extranjeros.
- FMD europeo.
- SSN o registros nacionales de pacientes no aplicables al Perú.
- Reglas de sustitución de medicamentos de otras jurisdicciones.
- Lógica tributaria extranjera.
- Modelos de productos controlados de otras jurisdicciones.

Esas capacidades sirven como benchmarking pero deben adaptarse a la normativa peruana.
