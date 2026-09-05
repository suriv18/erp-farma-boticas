# API-FAR-005 — OpenAPI Contract-First

OpenAPI define una interfaz estándar independiente del lenguaje para describir APIs HTTP. La última versión publicada es OAS 3.2.0 (19-09-2025). [REF-45]

## Decisión

Se adopta **contract-first** como principio. La versión OAS efectiva (`3.1.x` o `3.2.0`) se fijará después de validar compatibilidad de generadores, gateways, validators y documentación.

## Flujo

```text
RF/CU/CA
   ↓
Command / Query
   ↓
Contrato OpenAPI
   ↓
Revisión negocio + seguridad
   ↓
Implementación
   ↓
Contract tests
```

## Reglas

1. Schemas de request y response son contratos, no entidades JPA.
2. `readOnly`/`writeOnly` se usa cuando corresponda.
3. Enumeraciones externas deben gestionarse con compatibilidad; no romper consumidores por agregar valores sin estrategia.
4. Errores referencian componentes RFC 9457.
5. Seguridad se documenta por operación.
6. Endpoints offline/locales se documentan aparte si no son parte de la API corporativa pública.
7. Webhooks/eventos externos tendrán contratos/versionado propios.
