# SEC-FAR-003 — Seguridad del Store Edge y POS

## Amenazas principales

- robo físico de terminal;
- malware/local admin;
- manipulación de reloj;
- replay de mensajes offline;
- extracción de DB local;
- credenciales cacheadas;
- sincronización con servidor falso;
- alteración de precio/catálogo local;
- abuso de privilegios durante desconexión.

## Controles candidatos

1. cifrado del dispositivo/base local cuando el entorno lo permita;
2. claves/secretos protegidos por almacén seguro del SO/TPM, no texto plano;
3. identificación única de tienda y terminal;
4. mTLS o credencial de dispositivo para sincronización;
5. firma/MAC de mensajes cuando el diseño lo justifique;
6. outbox/inbox con anti-replay (`messageId` + secuencia/cursor);
7. expiración de credenciales/catálogos y modo restringido;
8. auditoría local append-only/buffer hasta sync;
9. bloqueo de funciones administrativas offline que requieran central;
10. actualización firmada del software Store Edge/POS.

## Restricción offline

La operación offline debe tener una matriz explícita:

| Función | Offline |
|---|---|
| venta de SKU sin restricción | candidato |
| venta bajo receta | condicionado a datos/reglas/profesional disponibles |
| producto controlado | por validar estrictamente |
| cambio maestro de precio | no por defecto |
| alta de usuarios | no |
| cierre de turno | sí, con sync posterior |
| recall conocido localmente | bloqueo obligatorio |

No se asumirán permisos offline hasta cerrar la política de riesgo.
