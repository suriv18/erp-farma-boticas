# API-FAR-006 — Contratos con Sistemas Externos

## Principio ACL

SUNAT, DIGEMID, ERP, adquirentes y proveedores no deben introducir directamente su vocabulario/protocolo en el dominio.

```text
Dominio
  ↓
Port
  ↓
Adapter / Anti-Corruption Layer
  ↓
Sistema externo
```

## SUNAT / PSE / OSE

El dominio fiscal utiliza estados internos estables y traduce códigos externos. Venta, CPE y aceptación externa permanecen separados.

## DIGEMID

Integraciones candidatas:

- consulta/actualización de catálogo regulatorio;
- retiros/alertas;
- reporte de precios;
- farmacovigilancia.

No se asumirá que existe API pública oficial para cada caso. Si solo existe portal/archivo/proceso manual, el adaptador debe respetar el mecanismo autorizado.

## ERP

El `ERP Port` recibe hechos financieros/postings idempotentes. El Core Retail no escribe directamente tablas de un ERP externo.

## Pagos

Preferencia arquitectónica: terminal/adquirente/tokenización para minimizar manejo de datos de tarjeta. Si el sistema almacena, procesa o transmite datos de cuenta del tarjetahabiente, debe evaluarse el alcance PCI DSS vigente (v4.0.1). [REF-49]
