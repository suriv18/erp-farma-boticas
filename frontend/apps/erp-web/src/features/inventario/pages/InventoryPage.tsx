import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button, ListFilters, PageHeader } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import { posicionesQuery } from '../api/posiciones.api';
import type { Posicion } from '../api/inventario.types';
import { AjusteDialog } from '../components/AjusteDialog';
import { PosicionesTable } from '../components/PosicionesTable';
import { SkuSelect } from '../components/SkuSelect';
import { almacenesDe } from '../lib/estructura';
import { useOpcionesEstablecimientos } from '../lib/use-opciones-establecimientos';
import { usePosicionesFiltros } from '../lib/use-posiciones-filtros';

export function InventoryPage() {
  const [dialogo, setDialogo] = useState<{ posicion: Posicion | null } | null>(null);
  const [busquedaSku, setBusquedaSku] = useState('');
  const { filtros, setEstablecimiento, setAlmacen, setSku, setPage, setSize } =
    usePosicionesFiltros();
  const establecimientos = useOpcionesEstablecimientos();

  const { data, isPending, isError } = useQuery(posicionesQuery(filtros));

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        title="Inventario"
        context="Operaciones / Inventario"
        description="Stock por almacén, lote y vencimiento."
        actions={<Button onClick={() => setDialogo({ posicion: null })}>Registrar ingreso</Button>}
      />

      <ListFilters
        label="Buscar SKU"
        placeholder="Código o descripción"
        value={busquedaSku}
        onValueChange={setBusquedaSku}
      >
        <div className="w-full sm:w-56">
          <SelectField
            id="filtro-establecimiento"
            label="Establecimiento"
            value={filtros.establecimientoId}
            onChange={(event) => setEstablecimiento(event.target.value)}
          >
            <option value="">Todos</option>
            {establecimientos.map((establecimiento) => (
              <option key={establecimiento.id} value={establecimiento.id}>
                {establecimiento.nombre}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="w-full sm:w-56">
          <SelectField
            id="filtro-almacen"
            label="Almacén"
            value={filtros.almacenId}
            onChange={(event) => setAlmacen(event.target.value)}
          >
            <option value="">Todos</option>
            {almacenesDe(establecimientos, filtros.establecimientoId).map((almacen) => (
              <option key={almacen.id} value={almacen.id}>
                {almacen.nombre}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="w-full sm:w-64">
          <SkuSelect
            id="filtro-sku"
            label="SKU"
            placeholder="Todos"
            search={busquedaSku}
            value={filtros.skuId}
            onChange={setSku}
          />
        </div>
      </ListFilters>

      <div className="mt-6">
        <PosicionesTable
          rows={data?.items ?? []}
          isLoading={isPending}
          isError={isError}
          pagination={{
            page: filtros.page,
            size: filtros.size,
            totalElements: data?.totalElements ?? 0,
            onPageChange: setPage,
            onSizeChange: setSize
          }}
          onAjustar={(posicion) => setDialogo({ posicion })}
        />
      </div>

      {dialogo ? (
        <AjusteDialog posicion={dialogo.posicion} onClose={() => setDialogo(null)} />
      ) : null}
    </div>
  );
}
