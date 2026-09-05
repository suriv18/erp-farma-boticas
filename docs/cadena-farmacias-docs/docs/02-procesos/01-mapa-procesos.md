# BPM-FAR-001 — Mapa de Procesos Macro

## 1. Procesos estratégicos

- Planeamiento comercial.
- Planeamiento de abastecimiento.
- Gestión financiera.
- Gobierno de precios y promociones.
- Gestión de proveedores.
- Cumplimiento sanitario.
- Gobierno de datos maestros.
- Analítica y BI.

## 2. Procesos misionales / core

### P01. Alta y gobierno de producto

```text
Fuente regulatoria / proveedor
        ↓
Validar producto
        ↓
Registrar atributos sanitarios
        ↓
Crear presentación / SKU
        ↓
Configurar venta / inventario / precio
        ↓
Publicar a canales
```

### P02. Abastecimiento

```text
Demanda / stock / política
        ↓
Necesidad de reposición
        ↓
Compra o transferencia
        ↓
Orden
        ↓
Recepción
        ↓
Validación lote / vencimiento
        ↓
Stock disponible o bloqueado
```

### P03. Distribución / transferencia

```text
Solicitud
  ↓
Aprobación
  ↓
Reserva origen
  ↓
Picking
  ↓
Despacho
  ↓
Stock en tránsito
  ↓
Recepción destino
  ↓
Diferencias
  ↓
Cierre
```

### P04. Venta OTC / retail

```text
Cliente
  ↓
Producto
  ↓
Validación disponibilidad
  ↓
Precio / promoción
  ↓
Cobro
  ↓
Comprobante
  ↓
Salida de inventario
  ↓
Cierre POS
```

### P05. Venta con receta / dispensación

```text
Cliente / paciente
        ↓
Receta
        ↓
Recepción y validación
        ↓
Análisis / interpretación
        ↓
Selección del producto
        ↓
Validaciones de stock/lote
        ↓
Registro de dispensación
        ↓
Información al usuario
        ↓
Cobro POS
        ↓
Salida de inventario
```

La Directiva Sanitaria N.° 105-MINSA/2020/DIGEMID sustenta las etapas de recepción/validación, análisis, preparación/selección, registros y entrega/información.

### P06. Producto controlado

```text
Producto identificado como controlado
        ↓
Regla específica
        ↓
Receta/documento requerido
        ↓
Validación profesional
        ↓
Registro de control
        ↓
Dispensación
        ↓
Actualización de existencias/registros
```

La operativa fue profundizada con D.S. N.° 023-2001-SA y fuentes aplicables antes de consolidar los RF/RN y el baseline físico; cualquier cambio regulatorio posterior debe volver a trazarse contra esas reglas.

### P07. Devolución / reclamo

Debe distinguir:

- devolución comercial;
- error de despacho;
- producto deteriorado;
- producto sujeto a retiro;
- devolución a proveedor.

La política por categoría queda `POR_VALIDAR`.

### P08. Retiro / recall

```text
Alerta / orden de retiro
        ↓
Identificar producto/lote
        ↓
Bloquear venta
        ↓
Localizar stock por tienda/almacén
        ↓
Retirar / segregar
        ↓
Registrar destino
        ↓
Cerrar campaña de retiro
```

### P09. Reporte de precios

```text
Maestro de precios vigentes
        ↓
Validación de establecimientos/productos
        ↓
Generación de información
        ↓
Reporte al Observatorio
        ↓
Evidencia / auditoría
```

DIGEMID informa que farmacias y boticas reportan precios mensualmente al Observatorio Peruano de Productos Farmacéuticos.

## 3. Procesos de soporte

- IAM y administración de usuarios.
- Directorio de personal y turnos.
- Auditoría.
- Gestión documental.
- Integraciones.
- Emisión electrónica.
- Soporte POS.
- Configuración de tiendas.
- Gestión de dispositivos/periféricos.
- Observabilidad.
- Continuidad de negocio.
