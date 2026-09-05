# SEC-FAR-007 — Threat Model Inicial

## Activos críticos

1. disponibilidad de POS;
2. integridad de precios;
3. integridad del inventario/lotes;
4. trazabilidad de dispensación/controlados;
5. CPE y postings financieros;
6. credenciales/roles;
7. datos personales/recetas;
8. sincronización tienda-central.

## Escenarios

| Amenaza | Impacto | Control candidato |
|---|---|---|
| BOLA: consultar venta de otro local | confidencialidad | object-level auth |
| elevar rol de cajero | fraude | RBAC+scope+auditoría |
| replay de venta offline | doble venta/posting | Inbox/idempotencia |
| modificar precio local | pérdida/fraude | catálogo firmado/versionado + override controlado |
| vender lote recalled | seguridad sanitaria | proyección recall + vendibilidad central/local |
| doble consumo última unidad | stock negativo | control de concurrencia |
| extracción DB Store Edge | datos/fraude | cifrado/minimización |
| compromiso API tercero | supply chain | validación, ACL, allowlist, timeouts |
| abuso de exportación | fuga masiva | permisos, límites, auditoría |
| ransomware tienda | indisponibilidad | hardening, backups, reconstrucción edge |

## Próxima fase

Realizar threat modeling formal por flujo con STRIDE u otra técnica elegida, y mapear controles a ASVS/API Security Top 10.
