# BPM-FAR-005 — TO-BE Precios, Promociones y Observatorio DIGEMID

## 1. Objetivo

Separar precio comercial, promociones y obligación de reporte regulatorio.

## 2. Modelo conceptual

```text
Costo
  ↓
Precio base
  ↓
Precio por zona/local/canal
  ↓
Promoción / descuento
  ↓
Precio efectivo de transacción
```

El precio sanitario/regulatorio reportado no debe deducirse improvisadamente desde el último ticket; se debe definir qué precio/dato exige el mecanismo oficial de reporte.

## 3. Flujo de gobierno de precios

```text
Propuesta precio
     ↓
Validar vigencia
     ↓
Validar ámbito (cadena/zona/local/canal)
     ↓
Aprobación
     ↓
Publicación
     ↓
Distribución a tiendas/canales
     ↓
POS ejecuta precio vigente
     ↓
Auditoría
```

## 4. Promociones

Una promoción debe ser independiente de:

- registro sanitario;
- condición de venta;
- obligación de receta;
- estado del lote.

Una promoción nunca debe desbloquear un producto no vendible.

## 5. Observatorio DIGEMID

DIGEMID informa que farmacias y boticas privadas reportan mensualmente precios de productos farmacéuticos al Observatorio Peruano de Productos Farmacéuticos.

Por tanto se requiere una capacidad futura:

```text
Fuente de precios
       ↓
Preparar reporte
       ↓
Validar establecimiento / producto
       ↓
Transmitir / cargar
       ↓
Registrar respuesta/evidencia
       ↓
Reprocesar errores
```

La interfaz técnica exacta (API, archivo, portal, etc.) queda `POR_VALIDAR` mediante investigación específica del mecanismo vigente.
