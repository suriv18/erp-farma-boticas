-- Seleccion inicial de 100 principios activos, no catalogo exhaustivo.
-- Fuente: DIGEMID, LISTADO_PNUME_2023.xlsx, RM 633-2023/MINSA.
-- https://repositorio-digemid.minsa.gob.pe/bitstreams/a5925c7f-7f2f-409d-b8a5-1bc91f44763d/download
-- Consultado 2026-10-14. Cada nombre contrastado con columna B del XLSX.
-- Se preservan sales y denominaciones de origen. Sin concentraciones/formas ni combinaciones.
-- codigo_fuente NULL: la fuente no identifica sustancias con un codigo unico.
-- nombre_normalizado: misma denominacion en minusculas sin tildes para busqueda.
-- Catalogo global, sin asignacion a productos ni alteracion de datos existentes.
\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.principio_activo IN SHARE ROW EXCLUSIVE MODE;
CREATE TEMP TABLE principios_carga (
 denominacion varchar(300) PRIMARY KEY
) ON COMMIT DROP;
INSERT INTO principios_carga VALUES
('Acetazolamida'),
('Acetilcisteína'),
('Aciclovir'),
('Ácido acetilsalicílico'),
('Ácido ascórbico'),
('Ácido fólico'),
('Ácido fusídico'),
('Ácido tranexámico'),
('Albendazol'),
('Alopurinol'),
('Alprazolam'),
('Amikacina (como sulfato)'),
('Aminofilina'),
('Amiodarona clorhidrato'),
('Amitriptilina clorhidrato'),
('Amlodipino (como besilato)'),
('Amoxicilina'),
('Ampicilina (como sal sódica)'),
('Atenolol'),
('Atorvastatina (como sal cálcica)'),
('Azitromicina'),
('Baclofeno'),
('Beclometasona dipropionato'),
('Bencilo benzoato'),
('Bencilpenicilina benzatina'),
('Benzoilo peróxido'),
('Bisacodilo'),
('Bismuto subsalicilato'),
('Bisoprolol fumarato'),
('Budesonida'),
('Calcio carbonato'),
('Calcitriol'),
('Captopril'),
('Carbamazepina'),
('Carvedilol'),
('Cefalexina'),
('Cefazolina (como sal sódica)'),
('Ceftriaxona (como sal sódica)'),
('Cefuroxima (como axetil)'),
('Ciprofloxacino'),
('Claritromicina'),
('Clindamicina (como clorhidrato)'),
('Clobetasol propionato'),
('Clonazepam'),
('Clopidogrel (como bisulfato)'),
('Cloranfenicol'),
('Clorfenamina maleato'),
('Clotrimazol'),
('Colchicina'),
('Dexametasona'),
('Dextrometorfano bromhidrato'),
('Diazepam'),
('Diclofenaco sódico'),
('Dicloxacilina (como sal sódica)'),
('Dimenhidrinato'),
('Doxiciclina (como clorhidrato o hiclato)'),
('Enalapril maleato'),
('Escopolamina butilbromuro'),
('Espironolactona'),
('Fluconazol'),
('Fluoxetina (como clorhidrato)'),
('Fosfomicina (como trometamol)'),
('Furosemida'),
('Gabapentina'),
('Gemfibrozilo'),
('Gentamicina (como sulfato)'),
('Glibenclamida'),
('Gliclazida'),
('Hidroclorotiazida'),
('Hidrocortisona'),
('Ibuprofeno'),
('Ipratropio bromuro'),
('Itraconazol'),
('Ivermectina'),
('Lactulosa'),
('Levofloxacino'),
('Levotiroxina sódica'),
('Lidocaína'),
('Loperamida'),
('Loratadina'),
('Losartán potásico'),
('Mebendazol'),
('Metamizol sódico'),
('Metformina clorhidrato'),
('Metoclopramida clorhidrato'),
('Metronidazol'),
('Mupirocina'),
('Naproxeno (como base o sal sódica)'),
('Nifedipino'),
('Nistatina'),
('Nitrofurantoina'),
('Omeprazol'),
('Paracetamol'),
('Permetrina'),
('Prednisona'),
('Salbutamol (como sulfato)'),
('Sertralina (como clorhidrato)'),
('Simeticona'),
('Tramadol clorhidrato'),
('Zinc sulfato');

CREATE FUNCTION pg_temp.clave_principio(text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
 SELECT regexp_replace(translate(lower(trim($1)), 'áéíóúüñ', 'aeiouun'), '\s+', ' ', 'g')
$$;

DO $$
BEGIN
 IF EXISTS (
  SELECT 1 FROM sch_catalogo.principio_activo p JOIN principios_carga s
    ON pg_temp.clave_principio(p.denominacion) = pg_temp.clave_principio(s.denominacion)
  WHERE p.estado <> 'ACTIVO' OR p.es_activo <> '1'
 ) THEN
  RAISE EXCEPTION 'Existe un principio inactivo coincidente; revisar antes de cargar.';
 END IF;
END $$;

WITH nuevos AS (
 INSERT INTO sch_catalogo.principio_activo (denominacion, nombre_normalizado, codigo_fuente, fuente)
 SELECT s.denominacion, pg_temp.clave_principio(s.denominacion), NULL,
        'DIGEMID PNUME 2023, RM 633-2023/MINSA | https://repositorio-digemid.minsa.gob.pe/bitstreams/a5925c7f-7f2f-409d-b8a5-1bc91f44763d/download'
 FROM principios_carga s
 WHERE NOT EXISTS (
  SELECT 1 FROM sch_catalogo.principio_activo p
  WHERE pg_temp.clave_principio(p.denominacion) = pg_temp.clave_principio(s.denominacion)
 )
 RETURNING id
)
SELECT count(*) AS principios_insertados FROM nuevos;

DO $$
BEGIN
 IF (SELECT count(*) FROM principios_carga s
     WHERE EXISTS (
      SELECT 1 FROM sch_catalogo.principio_activo p
      WHERE pg_temp.clave_principio(p.denominacion) = pg_temp.clave_principio(s.denominacion)
        AND p.estado = 'ACTIVO' AND p.es_activo = '1'
     )) <> 100 THEN
  RAISE EXCEPTION 'No se verificaron los 100 principios esperados';
 END IF;
END $$;
COMMIT;
SELECT count(*) AS total_principios,
 count(*) FILTER (WHERE estado = 'ACTIVO' AND es_activo = '1') AS activos,
 count(*) FILTER (WHERE fuente LIKE 'DIGEMID PNUME 2023%') AS con_fuente_pnume
FROM sch_catalogo.principio_activo;

