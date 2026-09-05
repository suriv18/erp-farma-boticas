# 11 — DevOps, DevSecOps y Operación

**Estado:** Línea base propuesta.  
**Objetivo:** definir cómo construir, verificar, empaquetar, desplegar, observar, recuperar y operar la plataforma sin asumir todavía proveedor cloud, orquestador, CI/CD ni gestor de secretos.

## Principio rector

La operación de una cadena de farmacias combina dos perfiles distintos:

```text
Core Central
  └─ servicio corporativo de alta disponibilidad

Store Edge (cuando aplique)
  └─ continuidad local + sincronización posterior
```

Por ello DevOps debe contemplar tanto servicios centrales como despliegues distribuidos en establecimientos.

## Documentos

1. [Principios DevOps/DevSecOps](01-principios-devops-devsecops.md)
2. [Entornos, configuración y secretos](02-entornos-configuracion-secretos.md)
3. [CI/CD y quality gates](03-cicd-quality-gates.md)
4. [Observabilidad, SLI/SLO y alertas](04-observabilidad-slo-alertas.md)
5. [Despliegue Core Central / Store Edge](05-despliegue-core-store-edge.md)
6. [Backup, DR y continuidad](06-backup-dr-continuidad.md)
7. [Releases, migraciones y rollback](07-release-migraciones-rollback.md)
8. [Seguridad de la cadena de suministro](08-supply-chain-security.md)
9. [Runbooks operacionales](09-runbooks-operacion.md)

## Fuentes base

- NIST SSDF 1.1 como referencia estable para integrar seguridad al SDLC. [REF-51]
- SLSA 1.2 para controles progresivos de integridad/provenance de artefactos. [REF-52]
- OpenTelemetry como estándar vendor-neutral para traces, metrics y logs. [REF-53]
- Twelve-Factor como referencia de portabilidad/configuración, sin adoptarlo como dogma. [REF-54]
- CycloneDX/SPDX para SBOM. [REF-55] [REF-56]
