-- Clasificaciones de uso medicinal: listas del Anexo 2, DS 023-2001-SA.
-- Consultado 2026-10-14. Codigos internos LISTA_* derivados de las listas normativas.
-- Fuente de listas: https://www.digemid.minsa.gob.pe/Archivos/PortalWeb/Informativo/Catalogacion/SUSTANCIA_CONTROLADA_NOVIEMBRE_2024.pdf
-- Receta especial y vigencia: art. 23 modificado por DS 024-2020-SA.
-- https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2020/DS_024-2020-SA.pdf
-- Retencion: arts. 30 y 36; receta comun: art. 27.
-- https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2001/DecretoSupremoN023-2001-SA.pdf
-- No se incluyen IA/IB (prohibicion) ni V (investigacion).
-- NULL en vigencia significa no parametrizada; NO vigencia ilimitada.
-- Reglas generales por lista: la asignacion a cada producto y los regimenes
-- especificos (p.ej. cannabis) requieren verificar su registro y norma aplicable.
-- No asigna clasificaciones a productos ni cambia reglas del motor de dispensacion.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.clasificacion_controlada IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMP TABLE clasificaciones_carga (
 codigo varchar(40) PRIMARY KEY,
 denominacion varchar(200) NOT NULL UNIQUE,
 requiere_receta_especial boolean NOT NULL,
 retiene_receta boolean NOT NULL,
 vigencia_receta_dias integer,
 norma_fuente varchar(300) NOT NULL
) ON COMMIT DROP;

INSERT INTO clasificaciones_carga VALUES
('LISTA_IIA', 'Lista II A', true, true, 3, 'DS 023-2001-SA, arts. 23 (mod. DS 024-2020-SA) y 30 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2020/DS_024-2020-SA.pdf'),
('LISTA_IIB', 'Lista II B', false, true, NULL, 'DS 023-2001-SA, arts. 27 y 36 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2001/DecretoSupremoN023-2001-SA.pdf'),
('LISTA_IIIA', 'Lista III A', true, true, 3, 'DS 023-2001-SA, arts. 23 (mod. DS 024-2020-SA) y 30 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2020/DS_024-2020-SA.pdf'),
('LISTA_IIIB', 'Lista III B', true, true, 3, 'DS 023-2001-SA, arts. 23 (mod. DS 024-2020-SA) y 30 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2020/DS_024-2020-SA.pdf'),
('LISTA_IIIC', 'Lista III C', true, true, 3, 'DS 023-2001-SA, arts. 23 (mod. DS 024-2020-SA) y 30 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2020/DS_024-2020-SA.pdf'),
('LISTA_IVA', 'Lista IV A', false, true, NULL, 'DS 023-2001-SA, arts. 27 y 36 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2001/DecretoSupremoN023-2001-SA.pdf'),
('LISTA_IVB', 'Lista IV B', false, true, NULL, 'DS 023-2001-SA, arts. 27 y 36 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2001/DecretoSupremoN023-2001-SA.pdf'),
('LISTA_VI', 'Lista VI', false, true, NULL, 'DS 023-2001-SA, arts. 27 y 36 | https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2001/DecretoSupremoN023-2001-SA.pdf');

DO $$
BEGIN
 IF EXISTS (
  SELECT 1 FROM sch_catalogo.clasificacion_controlada c JOIN clasificaciones_carga s
    ON c.codigo = s.codigo OR lower(c.denominacion) = lower(s.denominacion)
  WHERE c.codigo <> s.codigo OR c.denominacion <> s.denominacion
     OR c.requiere_receta_especial <> s.requiere_receta_especial
     OR c.retiene_receta <> s.retiene_receta
     OR c.vigencia_receta_dias IS DISTINCT FROM s.vigencia_receta_dias
     OR c.estado <> 'ACTIVO' OR c.es_activo <> '1'
 ) THEN
  RAISE EXCEPTION 'Clasificacion existente incompatible. Revisar antes de cargar.';
 END IF;
END $$;

WITH nuevas AS (
 INSERT INTO sch_catalogo.clasificacion_controlada
 (codigo, denominacion, requiere_receta_especial, retiene_receta, vigencia_receta_dias, norma_fuente)
 SELECT codigo, denominacion, requiere_receta_especial, retiene_receta, vigencia_receta_dias, norma_fuente
 FROM clasificaciones_carga
 ON CONFLICT (codigo) DO NOTHING
 RETURNING codigo
)
SELECT count(*) AS clasificaciones_insertadas FROM nuevas;

DO $$
BEGIN
 IF (SELECT count(*) FROM sch_catalogo.clasificacion_controlada c JOIN clasificaciones_carga s USING (codigo)
     WHERE c.denominacion = s.denominacion
       AND c.requiere_receta_especial = s.requiere_receta_especial
       AND c.retiene_receta = s.retiene_receta
       AND c.vigencia_receta_dias IS NOT DISTINCT FROM s.vigencia_receta_dias
       AND c.estado = 'ACTIVO' AND c.es_activo = '1') <> 8 THEN
  RAISE EXCEPTION 'No se verificaron las ocho clasificaciones esperadas';
 END IF;
END $$;
COMMIT;
SELECT codigo, denominacion, requiere_receta_especial, retiene_receta, vigencia_receta_dias, estado
FROM sch_catalogo.clasificacion_controlada
ORDER BY codigo;

