# DEVOPS-FAR-007 — Releases, migraciones y rollback

Las migraciones son forward-only por defecto, versionadas y probadas en PostgreSQL 18. Cambios incompatibles seguirán expand/contract cuando sea necesario. Todo release deberá declarar compatibilidad Core ↔ Store Edge y plan de rollback/forward-fix.
