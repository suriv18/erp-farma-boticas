# DEVOPS-FAR-003 — CI/CD y Quality Gates

Pipeline mínimo: build → unit tests → architecture tests → integration/contract tests → SAST/dependency scan → SBOM → package/sign/provenance → deploy por ambiente. Migraciones PostgreSQL deben ejecutarse en BD efímera de PostgreSQL 18 antes de promoción.
