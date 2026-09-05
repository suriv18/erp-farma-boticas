# BPM-FAR-008 — TO-BE Devoluciones, Reembolsos y Notas de Crédito

## 1. Objetivo

Separar tres decisiones distintas:

1. **devolución comercial** al cliente;
2. **regularización tributaria** de la venta;
3. **disposición sanitaria** del producto físico devuelto.

Nunca deben modelarse como un único booleano `devuelto = true`.

## 2. Flujo de devolución comercial

```text
Cliente solicita devolución
       ↓
Localizar venta/comprobante original
       ↓
Validar política comercial + restricciones regulatorias
       ↓
Identificar ítems/cantidades
       ↓
Autorizar devolución
       ↓
Determinar devolución total/parcial
       ↓
Generar regularización tributaria cuando corresponda
       ↓
Reembolsar por medio permitido
       ↓
Enviar producto físico a evaluación de disposición
```

SUNAT reconoce la Nota de Crédito Electrónica como documento para anulaciones, descuentos, bonificaciones, devoluciones y otras disminuciones relacionadas con una factura o boleta previa.

## 3. Disposición del producto devuelto

**Decisión crítica:** aceptar comercialmente una devolución no autoriza automáticamente el reingreso a stock vendible.

```text
Producto físico devuelto
       ↓
Estado = PENDIENTE_EVALUACION
       ↓
Evaluar integridad / conservación / trazabilidad
       ↓
┌────────────────┬─────────────────┬───────────────────┐
│ REINTEGRABLE   │ NO_REINTEGRABLE │ CUARENTENA/ANÁLISIS│
└────────────────┴─────────────────┴───────────────────┘
```

El Manual de Buenas Prácticas de Almacenamiento aprobado por RM 132-2015/MINSA exige, para los establecimientos a los que aplica expresamente (laboratorios, droguerías, almacenes especializados y almacenes aduaneros), área/procedimiento/registros de devoluciones, identificación y decisión de destino; para productos termosensibles condiciona el retorno al inventario a evidencia de conservación de cadena de frío y autorización del Director Técnico.

**No se extiende automáticamente esa obligación específica a toda botica:** para farmacia minorista se tratará como principio de seguridad de dominio y la política sanitaria exacta se validará con el Director Técnico y la norma aplicable al tipo de establecimiento.

## 4. Devolución a proveedor

```text
Producto no conforme / próximo a vencer / error proveedor
       ↓
Segregar inventario
       ↓
Solicitud devolución proveedor
       ↓
Aprobación
       ↓
Salida trazable por lote
       ↓
Guía/documentación de traslado cuando corresponda
       ↓
Nota de crédito proveedor / ajuste ERP
```

## 5. Reglas candidatas

| Código candidato | Regla | Tipo |
|---|---|---|
| RC-DEV-001 | Toda devolución comercial referencia la venta original, salvo excepción documentada. | DOM |
| RC-DEV-002 | El reembolso no elimina ni modifica la venta histórica original. | DOM/AUD |
| RC-DEV-003 | La regularización tributaria debe conservar la relación con el CPE original. | NORM |
| RC-DEV-004 | Producto devuelto no ingresa automáticamente a stock vendible. | DOM/SEGURIDAD_SANITARIA |
| RC-DEV-005 | Reingreso de termosensible requiere política/evidencia de conservación cuando la norma aplicable así lo exija. | NORM/DOM |
| RC-DEV-006 | Toda disposición final debe registrar motivo, actor y fecha. | DOM/AUD |

## 6. Fuentes

- SUNAT — Nota de Crédito Electrónica: https://cpe.sunat.gob.pe/tipos_de_comprobantes/nota_de_credito
- SUNAT — Comprobantes de Pago: https://orientacion.sunat.gob.pe/04-comprobantes-de-pago
- DIGEMID — RM 132-2015/MINSA, Manual BPA: https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2015/RM_132-2015.pdf
