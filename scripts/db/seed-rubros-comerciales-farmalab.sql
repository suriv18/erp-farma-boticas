-- Rubros comerciales aprobados para FARMALAB el 2026-10-14.
-- Referencia comercial: https://www.boticasperu.pe/cuidado_personal/corporal
-- Nombres adaptados al ERP; codigos y descripciones internos, no oficiales.
-- es_farmaceutico identifica la linea comercial Medicamentos; no determina
-- la clasificacion sanitaria, permisos ni requisitos de los productos.
-- Insercion transaccional y repetible, sin actualizar registros existentes.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.rubro_comercial IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE rubros_carga (
    codigo varchar(50) PRIMARY KEY,
    nombre varchar(120) NOT NULL,
    descripcion varchar(300) NOT NULL,
    es_farmaceutico boolean NOT NULL,
    orden integer NOT NULL
) ON COMMIT DROP;

INSERT INTO rubros_carga VALUES
('FARMA', 'Medicamentos', 'Línea comercial de medicamentos del establecimiento.', true, 10),
('DISMED', 'Dispositivos médicos', 'Línea comercial de equipos, instrumentos e insumos médicos.', false, 20),
('DERMO', 'Dermocosmética', 'Línea comercial de productos dermocosméticos para el cuidado de la piel.', false, 30),
('CUIDADO', 'Cuidado personal', 'Línea comercial de higiene y cuidado personal cotidiano.', false, 40),
('BEBE', 'Mamá y bebé', 'Línea comercial de alimentación, higiene y accesorios materno infantiles.', false, 50),
('NUTRI', 'Nutrición', 'Línea comercial de productos nutricionales y complementos alimenticios.', false, 60),
('ADULTO', 'Cuidado del adulto mayor', 'Línea comercial de productos para el cuidado y asistencia del adulto mayor.', false, 70),
('SEXUAL', 'Bienestar sexual', 'Línea comercial de productos para el bienestar y cuidado sexual.', false, 80);

CREATE FUNCTION pg_temp.clave_rubro(text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT regexp_replace(translate(lower($1), 'áéíóúüñ', 'aeiouun'), '[^a-z0-9]', '', 'g')
$$;
CREATE TEMP TABLE rubros_insertados (codigo text, nombre text) ON COMMIT DROP;

DO $$
DECLARE
    destino bigint;
BEGIN
    SELECT id INTO STRICT destino FROM sch_admin.tenant
    WHERE codigo = 'FARMALAB' AND estado = 'ACTIVO' AND es_activo = '1';

    IF EXISTS (
        SELECT 1 FROM sch_catalogo.rubro_comercial r JOIN rubros_carga s
          ON r.codigo = s.codigo OR pg_temp.clave_rubro(r.nombre) = pg_temp.clave_rubro(s.nombre)
        WHERE r.tenant_id = destino AND (
            r.codigo <> s.codigo
            OR pg_temp.clave_rubro(r.nombre) <> pg_temp.clave_rubro(s.nombre)
            OR r.es_farmaceutico <> s.es_farmaceutico
            OR r.orden <> s.orden OR r.es_activo <> '1'
        )
    ) THEN
        RAISE EXCEPTION 'Existe un rubro incompatible con la carga. Revisar antes de continuar.';
    END IF;

    WITH nuevos AS (
        INSERT INTO sch_catalogo.rubro_comercial
            (tenant_id, codigo, nombre, descripcion, es_farmaceutico, orden, created_by)
        SELECT destino, s.codigo, s.nombre, s.descripcion, s.es_farmaceutico, s.orden,
               'SEED_RUBROS_20261014'
        FROM rubros_carga s
        WHERE NOT EXISTS (
            SELECT 1 FROM sch_catalogo.rubro_comercial r
            WHERE r.tenant_id = destino AND r.codigo = s.codigo AND r.es_activo = '1'
        )
        RETURNING codigo, nombre
    )
    INSERT INTO rubros_insertados SELECT codigo, nombre FROM nuevos;

    IF (SELECT count(*) FROM sch_catalogo.rubro_comercial r
        JOIN rubros_carga s USING (codigo)
        WHERE r.tenant_id = destino AND r.es_activo = '1'
          AND r.es_farmaceutico = s.es_farmaceutico AND r.orden = s.orden) <> 8 THEN
        RAISE EXCEPTION 'No se verificaron los ocho rubros esperados';
    END IF;
END $$;

SELECT count(*) AS rubros_insertados FROM rubros_insertados;
COMMIT;

-- Lectura persistida posterior al commit.
SELECT r.codigo, r.nombre, r.es_farmaceutico, r.orden, r.es_activo
FROM sch_catalogo.rubro_comercial r JOIN sch_admin.tenant t ON t.id = r.tenant_id
WHERE t.codigo = 'FARMALAB'
ORDER BY r.orden;
