# DOC-FAR-003 — Mapa de Capacidades de Negocio

## 1. Vista general

```text
CADENA DE FARMACIAS
│
├── A. Gobierno y Organización
├── B. Maestro de Productos
├── C. ERP Corporativo
├── D. Supply Chain / WMS
├── E. Retail / POS
├── F. Operación Farmacéutica
├── G. Cliente / CRM
├── H. Omnicanalidad
├── I. Cumplimiento / Calidad
├── J. Datos / BI
└── K. Capacidades Transversales
```

---

## A. Gobierno y Organización

### A1. Grupo / tenant
### A2. Empresa / razón social
### A3. Establecimiento farmacéutico
### A4. Centro de distribución / almacén
### A5. Local / tienda
### A6. POS / terminal
### A7. Caja
### A8. Áreas y ubicaciones internas
### A9. Autorización sanitaria
### A10. Director Técnico / QF asistentes
### A11. Horario de funcionamiento
### A12. Personal técnico

**Evidencia:** `NORM + DOM`.

La DIGEMID indica que establecimientos que realizan almacenamiento, comercialización, dispensación o expendio requieren autorización sanitaria previa. El Reglamento y su modificación 2025 establecen responsabilidades del Director Técnico y personal técnico.

---

## B. Maestro de Productos / MDM

### B1. Producto farmacéutico
### B2. Dispositivo médico
### B3. Producto sanitario
### B4. Producto retail no farmacéutico
### B5. Principio activo / IFA
### B6. Concentración
### B7. Forma farmacéutica
### B8. Vía de administración
### B9. Presentación
### B10. Fabricante / laboratorio
### B11. Registro sanitario
### B12. Condición de venta
### B13. Clasificación ATC
### B14. Clasificación/control sanitario
### B15. Unidad de medida
### B16. Código de barras
### B17. SKU comercial
### B18. Marca
### B19. Categoría retail
### B20. Impuestos
### B21. Estado comercial
### B22. Estado sanitario

**Evidencia:** `NORM + MKT + DOM`.

DIGEMID publica estándares vigentes para forma farmacéutica, vía de administración, unidad de medida, condición de venta, clasificación ATC y productos/sustancias controladas. La consulta oficial de registro sanitario expone datos como registro, nombre, fabricante, condición de venta, ATC y principio activo.

---

## C. ERP Corporativo

### C1. Proveedores
### C2. Contratos comerciales
### C3. Solicitud de compra
### C4. Orden de compra
### C5. Recepción y conformidad
### C6. Factura proveedor
### C7. Cuentas por pagar
### C8. Tesorería
### C9. Bancos / conciliación
### C10. Contabilidad general
### C11. Centros de costo / dimensiones
### C12. Cuentas por cobrar
### C13. Activos fijos
### C14. Presupuestos
### C15. Consolidación multiempresa
### C16. Costeo / valoración de inventario

**Evidencia:** `MKT + FUNC/POR_VALIDAR`.

Los ERP actuales separan cuentas por pagar, tesorería, bancos y libro mayor de los procesos operativos de tienda.

---

## D. Supply Chain / WMS

### D1. Almacenes y ubicaciones
### D2. Recepción
### D3. Lotes
### D4. Fechas de vencimiento
### D5. Estado de lote
### D6. Cuarentena / bloqueo
### D7. Stock disponible
### D8. Stock reservado
### D9. Stock en tránsito
### D10. Stock no vendible
### D11. Movimientos de inventario
### D12. Transferencias
### D13. Picking
### D14. Packing
### D15. Despacho
### D16. Recepción en destino
### D17. Ajustes
### D18. Inventarios físicos / conteos
### D19. Reposición automática
### D20. Stock mínimo/máximo
### D21. Pronóstico de demanda
### D22. FEFO configurable
### D23. Alertas de vencimiento
### D24. Devolución a proveedor
### D25. Retiro / recall
### D26. Trazabilidad de producto/lote

**Evidencia:** `NORM + MKT + DOM`.

La Ley 29459 prohíbe comercialización/dispensación de productos vencidos, en mal estado o de procedencia desconocida; por ello lote, vencimiento, estado y trazabilidad son capacidades críticas. FEFO se considera una política operacional soportada ampliamente por WMS/ERP modernos, pero se etiquetará `MKT/DOM` hasta precisar la regla aplicable por categoría.

---

## E. Retail / POS

### E1. Apertura de caja
### E2. Turnos
### E3. Venta
### E4. Venta mixta medicamentos + retail
### E5. Escaneo de código de barras
### E6. Pricing
### E7. Promociones
### E8. Descuentos autorizados
### E9. Medios de pago
### E10. Comprobantes electrónicos
### E11. Devoluciones
### E12. Anulaciones
### E13. Nota de crédito
### E14. Cierre y arqueo
### E15. Diferencias de caja
### E16. Consulta de stock de otras tiendas
### E17. Reserva
### E18. Suspender/recuperar transacción
### E19. Sales audit
### E20. Operación offline y sincronización

**Evidencia:** `MKT + NORM tributaria + POR_VALIDAR offline`.

SUNAT mantiene la obligación de comprobantes electrónicos para los sujetos designados/emisores obligados. El modelo POS debe integrar la emisión sin acoplar toda la venta a un proveedor único de CPE.

---

## F. Operación Farmacéutica

### F1. Recepción de receta
### F2. Validación de receta
### F3. Prescriptor
### F4. Paciente
### F5. Detalle prescrito
### F6. Vigencia / uso de receta
### F7. Condición de venta
### F8. Análisis e interpretación
### F9. Selección/preparación del producto
### F10. Dispensación
### F11. Expendio
### F12. Registro de dispensación
### F13. Información/orientación al paciente
### F14. Intervención del Químico Farmacéutico
### F15. Productos controlados
### F16. Receta especial
### F17. Registro/control de fiscalizados
### F18. Bloqueo sanitario
### F19. Alertas sanitarias / recall
### F20. Farmacovigilancia
### F21. Tecnovigilancia
### F22. Fórmulas magistrales (si aplica)
### F23. Cannabis medicinal (si aplica/licencia)

**Evidencia:** `NORM`.

La Directiva Sanitaria 105 describe la dispensación como recepción/validación, análisis/interpretación, preparación/selección, registros, entrega e información. También acepta recetas físicas, imagen digital y electrónicas que cumplan las reglas vigentes y establece confidencialidad. Productos controlados siguen normativa específica.

---

## G. Cliente / CRM

### G1. Cliente
### G2. Identificación
### G3. Consentimientos comerciales
### G4. Preferencias
### G5. Fidelización
### G6. Puntos
### G7. Cupones
### G8. Historial comercial
### G9. Segmentación
### G10. Atención/reclamos

**Evidencia:** `MKT + NORM protección datos`.

No se asumirá que un historial comercial habilita automáticamente el almacenamiento/uso de información de salud. La privacidad y finalidades de tratamiento deberán analizarse por separado.

---

## H. Omnicanalidad

### H1. Catálogo web/app
### H2. Stock disponible por local
### H3. Pedido online
### H4. Pago online
### H5. Click & Collect
### H6. Delivery
### H7. Reserva de stock
### H8. Asignación de tienda fulfillment
### H9. Seguimiento de pedido
### H10. Cancelación/devolución
### H11. Receta en canal digital
### H12. Validación farmacéutica en canal digital

**Evidencia:** `MKT + NORM por validar en detalle`.

No se habilitará venta digital de medicamentos sujetos a receta sin conservar los controles aplicables a la dispensación.

---

## I. Cumplimiento / Calidad

### I1. Autorizaciones sanitarias
### I2. Responsables profesionales
### I3. Registros de capacitación
### I4. Buenas prácticas
### I5. Alertas sanitarias
### I6. Productos robados / prohibidos
### I7. Retiro de producto
### I8. Farmacovigilancia
### I9. Tecnovigilancia
### I10. Reporte al Observatorio de Precios
### I11. Reportes de controlados
### I12. Trazabilidad de inspecciones

---

## J. Datos / BI

### J1. Ventas por tienda
### J2. Margen
### J3. Ticket promedio
### J4. Rotación
### J5. Quiebre de stock
### J6. Sobre-stock
### J7. Vencimientos y merma
### J8. Fill rate
### J9. Transferencias
### J10. Efectividad promociones
### J11. Fidelización
### J12. Rentabilidad por SKU/local
### J13. Compras/proveedor
### J14. Cash flow
### J15. Indicadores farmacéuticos/regulatorios

---

## K. Capacidades Transversales

### K1. IAM
### K2. JWT / sesiones
### K3. RBAC / ámbitos
### K4. Auditoría
### K5. Integraciones
### K6. Notificaciones
### K7. Gestión documental
### K8. Configuración
### K9. Observabilidad
### K10. Feature flags
### K11. Importación/exportación
### K12. Gestión de catálogos
