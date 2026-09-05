# ROAD-FAR-002 — MVP Candidato

## 1. Estado

`PROPUESTO / POR VALIDAR con stakeholders`.

No debe confundirse con alcance contractual cerrado.

## 2. Objetivo del MVP

Validar de extremo a extremo una operación farmacéutica real con trazabilidad suficiente:

```text
Producto/SKU
   ↓
Compra/Recepción
   ↓
Lote/Stock
   ↓
Precio
   ↓
Prescripción/Dispensación cuando aplique
   ↓
Venta POS
   ↓
Pago
   ↓
CPE
   ↓
Cierre / Posting
```

## 3. Incluido como candidato

- organización/establecimiento;
- usuarios, roles, permisos, auditoría;
- producto regulado y SKU;
- proveedor;
- orden/recepción;
- lote/vencimiento;
- inventario por establecimiento;
- transferencias básicas;
- precios;
- POS online;
- caja/turno;
- venta/pago;
- receta/dispensación para productos bajo receta;
- CPE adapter;
- devolución/nota de crédito;
- bloqueo/recall básico;
- reporting operacional mínimo.

## 4. Fuera del MVP salvo requisito explícito

- POS offline Store Edge;
- e-commerce/delivery;
- fidelización avanzada;
- optimización de reposición;
- contabilidad ERP completa si se integra un ERP externo;
- Event Sourcing;
- microservicios por dominio;
- automatizaciones DIGEMID no respaldadas por API/acuerdo formal.

## 5. Criterio de éxito

El MVP no se considera exitoso por “tener pantallas”. Debe demostrar:

- integridad de lote/stock;
- imposibilidad de vender producto no vendible;
- trazabilidad receta/dispensación/venta cuando aplique;
- no duplicación de CPE/posting;
- auditoría;
- conciliación operativa;
- cumplimiento de criterios de aceptación prioritarios.
