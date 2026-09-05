# Modelo de Dominio DDD — Cadena de Farmacias

**Estado:** Borrador de dominio trazable  
**Fase:** posterior a BPM / RF / RN / CU / CA  
**Principio de gobierno:** ningún concepto se presenta como obligación normativa si su origen es una decisión de dominio o técnica.

## Documentos

1. [01-modelo-dominio.md](01-modelo-dominio.md) — visión global, subdominios y principios del modelo.
2. [02-bounded-contexts-context-map.md](02-bounded-contexts-context-map.md) — Bounded Contexts y relaciones entre contextos.
3. [03-agregados-entidades-value-objects.md](03-agregados-entidades-value-objects.md) — agregados candidatos, entidades y Value Objects.
4. [04-estados-transiciones.md](04-estados-transiciones.md) — máquinas de estado y transiciones relevantes.
5. [05-eventos-comandos-servicios-dominio.md](05-eventos-comandos-servicios-dominio.md) — comandos, eventos y servicios/políticas de dominio.
6. [06-invariantes-politicas.md](06-invariantes-politicas.md) — invariantes y reglas que deben preservarse.
7. [07-lenguaje-ubicuo.md](07-lenguaje-ubicuo.md) — vocabulario de negocio y términos que no deben confundirse.
8. [08-trazabilidad-dominio.md](08-trazabilidad-dominio.md) — RF/RN/CU/CA → dominio.
9. [09-matriz-origen-conceptos.md](09-matriz-origen-conceptos.md) — origen normativo, funcional, de mercado o de diseño de cada concepto.
10. [10-decisiones-abiertas.md](10-decisiones-abiertas.md) — aspectos que **no** se deben cerrar todavía por falta de evidencia o decisión del negocio.

## Regla DDD aplicada

El dominio se divide en modelos coherentes dentro de límites explícitos. Un Bounded Context no implica automáticamente un microservicio. En la siguiente fase de arquitectura se decidirá la forma física de implementación.

Las fuentes de DDD utilizadas como referencia conceptual están registradas en `docs/99-referencias.md`; la normativa sanitaria/fiscal se utiliza para definir hechos y restricciones del negocio, no para imponer una estructura de software específica.
