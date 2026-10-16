import { Card } from '@boticas/ui-web';
import { SelectField } from '../../../shared/components/FormFields';
import { ESTADOS_ORDEN } from '../api/ordenes.types';
import type { Proveedor } from '../api/proveedores.types';
import { etiquetaEstadoOrden } from '../lib/estado-orden';
import type { FiltrosOrdenesUrl } from '../lib/use-ordenes-filtros';

type FiltrosOrdenesProps = {
  proveedores: Proveedor[];
  filtros: FiltrosOrdenesUrl;
  onProveedor: (proveedorId: string) => void;
  onEstado: (estado: string) => void;
};

export function FiltrosOrdenes({
  proveedores,
  filtros,
  onProveedor,
  onEstado
}: FiltrosOrdenesProps) {
  return (
    <Card className="mt-6 grid gap-4 p-4 sm:grid-cols-2" aria-label="Filtros del listado">
      <SelectField
        id="filtro-proveedor"
        label="Proveedor"
        value={filtros.proveedorId}
        onChange={(event) => onProveedor(event.target.value)}
      >
        <option value="">Todos</option>
        {proveedores.map(({ id, razonSocial }) => (
          <option key={id} value={id}>
            {razonSocial}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="filtro-estado-orden"
        label="Estado"
        value={filtros.estado}
        onChange={(event) => onEstado(event.target.value)}
      >
        <option value="">Todos</option>
        {ESTADOS_ORDEN.map((estado) => (
          <option key={estado} value={estado}>
            {etiquetaEstadoOrden(estado)}
          </option>
        ))}
      </SelectField>
    </Card>
  );
}
