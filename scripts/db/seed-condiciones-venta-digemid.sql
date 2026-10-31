-- Tres condiciones del estandar DIGEMID de marzo 2026 V.01.
-- Consultado el 2026-10-14; codigos y denominaciones del documento oficial:
-- https://www.digemid.minsa.gob.pe/Archivos/PortalWeb/Informativo/Catalogacion/3_Marzo_2026_ST_CONDICION%20DE%20VENTA_V.01.docx
-- Catalogo global. No modifica productos ni condiciones existentes.
-- La fecha del documento no se usa como fecha legal de inicio de vigencia.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.condicion_venta IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE condiciones_carga (
 codigo varchar(30) PRIMARY KEY,
 denominacion varchar(200) NOT NULL UNIQUE,
 requiere_receta boolean NOT NULL,
 requiere_retencion boolean NOT NULL
) ON COMMIT DROP;
INSERT INTO condiciones_carga VALUES
('CRM-1', 'CON RECETA MEDICA', true, false),
('CRMR-2', 'CON RECETA MEDICA RETENIDA', true, true),
('SRM-3', 'SIN RECETA MEDICA', false, false);

CREATE FUNCTION pg_temp.clave_condicion(text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
 SELECT regexp_replace(translate(lower($1), 'áéíóúüñ', 'aeiouun'), '[^a-z0-9]', '', 'g')
$$;
DO $$
BEGIN
 IF EXISTS (
  SELECT 1 FROM sch_catalogo.condicion_venta c JOIN condiciones_carga s
    ON c.codigo = s.codigo OR pg_temp.clave_condicion(c.denominacion) = pg_temp.clave_condicion(s.denominacion)
  WHERE c.codigo <> s.codigo
     OR pg_temp.clave_condicion(c.denominacion) <> pg_temp.clave_condicion(s.denominacion)
     OR c.requiere_receta <> s.requiere_receta
     OR c.requiere_retencion <> s.requiere_retencion
     OR c.estado <> 'ACTIVO' OR c.es_activo <> '1'
 ) THEN
  RAISE EXCEPTION 'Condicion existente incompatible. Revisar antes de cargar.';
 END IF;
END $$;

WITH nuevas AS (
 INSERT INTO sch_catalogo.condicion_venta
 (codigo, denominacion, requiere_receta, requiere_retencion, fuente, version_fuente)
 SELECT codigo, denominacion, requiere_receta, requiere_retencion,
        'DIGEMID | https://www.digemid.minsa.gob.pe/Archivos/PortalWeb/Informativo/Catalogacion/3_Marzo_2026_ST_CONDICION%20DE%20VENTA_V.01.docx',
        'Marzo 2026 ST_CONDICION DE VENTA V.01'
 FROM condiciones_carga
 ON CONFLICT (codigo) DO NOTHING
 RETURNING codigo
)
SELECT count(*) AS condiciones_insertadas FROM nuevas;

DO $$
BEGIN
 IF (SELECT count(*) FROM sch_catalogo.condicion_venta c JOIN condiciones_carga s USING (codigo)
     WHERE c.denominacion = s.denominacion
       AND c.requiere_receta = s.requiere_receta
       AND c.requiere_retencion = s.requiere_retencion
       AND c.estado = 'ACTIVO' AND c.es_activo = '1') <> 3 THEN
  RAISE EXCEPTION 'No se verificaron las tres condiciones esperadas';
 END IF;
END $$;
COMMIT;
SELECT codigo, denominacion, requiere_receta, requiere_retencion, version_fuente, estado
FROM sch_catalogo.condicion_venta ORDER BY codigo;

