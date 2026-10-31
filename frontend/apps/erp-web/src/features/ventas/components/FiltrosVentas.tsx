import { Card } from '@boticas/ui-web';
import type { EstablishmentStructure } from '../../organizacion';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import type { FiltrosVentas as Filtros } from '../lib/use-ventas-filtros';

type FiltrosVentasProps = {
  establecimientos: EstablishmentStructure[];
  filtros: Filtros;
  onEstablecimiento: (id: string) => void;
  onDesde: (fecha: string) => void;
  onHasta: (fecha: string) => void;
};

export function FiltrosVentas({
  establecimientos,
  filtros,
  onEstablecimiento,
  onDesde,
  onHasta
}: FiltrosVentasProps) {
  return (
    <Card className="mt-6 grid gap-4 p-4 sm:grid-cols-3" aria-label="Filtros del listado">
      <SelectField
        id="filtro-establecimiento"
        label="Establecimiento"
        value={filtros.establecimientoId}
        onChange={(event) => onEstablecimiento(event.target.value)}
      >
        <option value="">Todos</option>
        {establecimientos.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <TextField
        id="filtro-desde"
        label="Desde"
        type="date"
        value={filtros.desde}
        onChange={(event) => onDesde(event.target.value)}
      />
      <TextField
        id="filtro-hasta"
        label="Hasta"
        type="date"
        value={filtros.hasta}
        onChange={(event) => onHasta(event.target.value)}
      />
    </Card>
  );
}
