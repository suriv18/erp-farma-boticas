# ARC-FAR-006 — Integraciones ERP, SUNAT, DIGEMID y terceros

## 1. Regla general

Ningún agregado de negocio debe llamar directamente SDKs/endpoints externos.

```text
Domain/Application
        ↓
Output Port
        ↓
Adapter / ACL
        ↓
Sistema externo
```

## 2. ERP

Se define un **ERP Boundary** independientemente de si el ERP termina siendo interno o externo.

### Operación Retail

Es propietaria de:

- venta;
- devolución;
- caja;
- stock/lote;
- dispensación;
- CPE state.

### ERP/Finanzas

Consume documentos/postings:

- venta contabilizable;
- medios de pago;
- impuestos;
- costo;
- devolución/NC;
- compras/CxP;
- cierres.

POS **no escribe directamente** tablas contables.

El posting debe poseer clave idempotente.

## 3. SUNAT / Fiscal

Se define:

```text
FiscalPort
  ├── emitirCpe()
  ├── consultarEstado()
  ├── emitirNotaCredito()
  └── recuperarConstancia()
```

El adapter concreto puede usar SEE propio, Facturador, PSE/OSE u otra alternativa permitida y aprobada.

### Offline y CPE

SUNAT documenta que el Facturador SUNAT puede **emitir sin Internet** y necesita conexión para enviar posteriormente; actualmente publica plazo de hasta tres días calendario para el envío de factura/nota en ese sistema. SUNAT también mantiene un procedimiento de comprobantes físicos de contingencia para circunstancias excepcionales y bajo requisitos específicos.

Consecuencia arquitectónica:

- no acoplar `VentaConfirmada` a una llamada sincrónica obligatoria a SUNAT;
- modelar el ciclo fiscal por separado;
- usar `FiscalOutbox`/idempotencia;
- la modalidad legal aplicable debe resolverse por configuración/adapter y no asumir que cualquier venta offline puede diferirse del mismo modo.

## 4. DIGEMID

Se definen puertos separados por finalidad, por ejemplo:

```text
RegulatoryCatalogPort
RecallSourcePort
PriceObservatoryPort
PharmacovigilanceNotificationPort
```

No se asumirá API pública donde no exista contrato verificable.

Opciones válidas según cada caso:

- carga oficial/manual;
- importación de archivos;
- API formal;
- proveedor autorizado;
- operación humana asistida.

Scraping no será dependencia crítica por defecto.

## 5. Adquirentes de pago

`PaymentPort` abstrae:

- autorización;
- captura;
- reverso;
- consulta;
- reconciliación.

Un pago aprobado y una venta confirmada son conceptos distintos; el proceso deberá definir compensaciones cuando uno falle.

## 6. Anti-Corruption Layer

El adapter traduce:

```text
Modelo externo
    ↓
ACL
    ↓
Modelo interno
```

No se introducen directamente términos/códigos del ERP/PSE/OSE dentro de los agregados salvo que sean realmente conceptos del negocio.
