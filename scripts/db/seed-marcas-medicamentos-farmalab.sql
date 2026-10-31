-- Marcas comerciales de medicamentos verificadas en fuentes peruanas el 2026-10-14.
-- Fuente por marca en marcas_carga. Codigos internos propios; descripcion NULL.
-- Una marca agrupa sus dosis/presentaciones. No se cargan laboratorios ni principios activos.
-- La presencia comercial no constituye validacion del registro sanitario de cada producto.
-- Insercion transaccional acotada a FARMALAB, sin modificar las marcas existentes.

\set ON_ERROR_STOP on
SET client_encoding = 'UTF8';
BEGIN;
SET LOCAL lock_timeout = '5s';
LOCK TABLE sch_catalogo.marca IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE marcas_carga (
    codigo varchar(50) PRIMARY KEY,
    nombre varchar(180) NOT NULL,
    fuente text NOT NULL
) ON COMMIT DROP;

INSERT INTO marcas_carga VALUES
('MAR-APRONAX', 'Apronax', 'https://www.apronax.com.pe/'),
('MAR-BUSCAPINA', 'Buscapina', 'https://www.buscapina.com/es-pe'),
('MAR-NEUROBION', 'Neurobion', 'https://www.boticasperu.pe/nutricion/vitaminas_y_minerales/vitamina_b/neurobion_-__blister_4_un/05562.html'),
('MAR-DOLO-NEUROBION', 'Dolo Neurobion', 'https://boticaharika.com/producto/319'),
('MAR-VOLTAREN', 'Voltaren', 'https://botifarma.pe/producto/voltaren-1-emulgel-tubo-100g/'),
('MAR-CANESTEN', 'Canesten', 'https://inkafarma.pe/lista/bepanthen-canesten-gynocanesten'),
('MAR-GYNOCANESTEN', 'Gynocanesten', 'https://inkafarma.pe/lista/bepanthen-canesten-gynocanesten'),
('MAR-GRAVOL', 'Gravol', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-BISMUTOL', 'Bismutol', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-BISMUCAR', 'Bismucar', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-ANDREWS', 'Andrews', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-EFETAMOL', 'Efetamol', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-GRAVAMIN', 'Gravamin', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-SIMSOROCH', 'Simsoroch', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-GRAVAX', 'Gravax', 'https://www.boticasperu.pe/on/demandware.store/Sites-BoticasPeru-Site/es_PE/Search-UpdateGrid?cgid=botencas-kitviajero&srule=best-matches&start=0&sz=94'),
('MAR-GASEOVET', 'Gaseovet', 'https://www.medifarma.com.pe/wp-content/uploads/2023/10/SALUD-DIGESTIVA_Vademecum_2.pdf'),
('MAR-FUROXONA', 'Furoxona', 'https://www.medifarma.com.pe/wp-content/uploads/2023/10/SALUD-DIGESTIVA_Vademecum_2.pdf'),
('MAR-EVACUOL', 'Evacuol', 'https://www.medifarma.com.pe/wp-content/uploads/2023/10/SALUD-DIGESTIVA_Vademecum_2.pdf'),
('MAR-ENTEROGERMINA', 'Enterogermina', 'https://www.enterogermina.com/es-pe/productos'),
('MAR-AERO-OM', 'Aero-OM', 'https://www.aeroomadultos.com/');


CREATE FUNCTION pg_temp.clave_marca(text) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT regexp_replace(
        translate(lower($1), 'áéíóúüñ', 'aeiouun'),
        '[^a-z0-9]', '', 'g')
$$;

CREATE TEMP TABLE marcas_insertadas (id bigint, codigo text, nombre text) ON COMMIT DROP;
DO $$
DECLARE
    destino bigint;
    fila record;
    coincidencias integer;
    existente record;
    nuevo_id bigint;
BEGIN
    SELECT id INTO STRICT destino FROM sch_admin.tenant
    WHERE codigo = 'FARMALAB' AND estado = 'ACTIVO' AND es_activo = '1';

    FOR fila IN SELECT * FROM marcas_carga ORDER BY nombre LOOP
        SELECT count(*) INTO coincidencias FROM sch_catalogo.marca m
        WHERE m.tenant_id = destino AND m.es_activo = '1'
          AND (m.codigo = fila.codigo OR pg_temp.clave_marca(m.nombre) = pg_temp.clave_marca(fila.nombre));
        IF coincidencias > 1 THEN
            RAISE EXCEPTION 'Multiples coincidencias para la marca %', fila.nombre;
        ELSIF coincidencias = 1 THEN
            SELECT m.* INTO STRICT existente FROM sch_catalogo.marca m
            WHERE m.tenant_id = destino AND m.es_activo = '1'
              AND (m.codigo = fila.codigo OR pg_temp.clave_marca(m.nombre) = pg_temp.clave_marca(fila.nombre));
            IF pg_temp.clave_marca(existente.nombre) <> pg_temp.clave_marca(fila.nombre)
               OR existente.estado <> 'ACTIVO' THEN
                RAISE EXCEPTION 'Conflicto con marca existente %', fila.nombre;
            END IF;
            CONTINUE;
        END IF;

        INSERT INTO sch_catalogo.marca (tenant_id, codigo, nombre, descripcion, created_by)
        VALUES (destino, fila.codigo, fila.nombre, NULL, 'SEED_MARCAS_MED_20261014')
        RETURNING id INTO nuevo_id;
        INSERT INTO marcas_insertadas VALUES (nuevo_id, fila.codigo, fila.nombre);
    END LOOP;

    IF (SELECT count(*) FROM marcas_carga s WHERE EXISTS (
        SELECT 1 FROM sch_catalogo.marca m
        WHERE m.tenant_id = destino AND m.estado = 'ACTIVO' AND m.es_activo = '1'
          AND pg_temp.clave_marca(m.nombre) = pg_temp.clave_marca(s.nombre)
    )) <> 20 THEN
        RAISE EXCEPTION 'No se verificaron las 20 marcas esperadas';
    END IF;
END $$;

SELECT count(*) AS marcas_insertadas FROM marcas_insertadas;
SELECT codigo, nombre FROM marcas_insertadas ORDER BY nombre;
COMMIT;

-- Verificacion persistida posterior al commit.
SELECT count(*) AS total_marcas,
       count(*) FILTER (WHERE m.estado = 'ACTIVO' AND m.es_activo = '1') AS activas,
       count(DISTINCT pg_temp.clave_marca(m.nombre)) AS nombres_distintos
FROM sch_catalogo.marca m JOIN sch_admin.tenant t ON t.id = m.tenant_id
WHERE t.codigo = 'FARMALAB';

