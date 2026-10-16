import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Plus, Search } from 'lucide-react';
import { Button, Card } from '@boticas/ui-web';
import { TextField } from '../../../shared/components/FormFields';
import { skusQuery, type SkuResumen } from '../../catalogo';

type BuscadorSkuProps = { onElegir: (sku: SkuResumen) => void };

export function BuscadorSku({ onElegir }: BuscadorSkuProps) {
  const [texto, setTexto] = useState('');
  const [termino, setTermino] = useState('');
  const { data, isError, isFetching } = useQuery({
    ...skusQuery({ q: termino, estado: 'ACTIVO', size: 10 }),
    enabled: termino !== ''
  });
  const resultados = data?.items ?? [];
  const buscando = termino !== '';
  const sinResultados = buscando && !isError && !isFetching && resultados.length === 0;

  return (
    <Card className="space-y-4 p-5">
      <form
        className="flex items-end gap-3"
        onSubmit={(evento) => {
          evento.preventDefault();
          setTermino(texto.trim());
        }}
      >
        <div className="min-w-0 flex-1">
          <TextField
            id="orden-buscar-sku"
            label="Buscar producto"
            icon={Search}
            placeholder="Código o descripción"
            value={texto}
            onChange={(evento) => setTexto(evento.target.value)}
          />
        </div>
        <Button type="submit">Buscar</Button>
      </form>
      {buscando && isError ? (
        <p className="text-danger-600 dark:text-danger-400 text-sm">No se pudo buscar productos.</p>
      ) : null}
      {sinResultados ? (
        <p className="text-sm text-neutral-500 dark:text-neutral-400">
          No se encontraron productos.
        </p>
      ) : null}
      {resultados.length > 0 ? (
        <ul className="divide-y divide-neutral-100 dark:divide-neutral-800">
          {resultados.map((sku) => (
            <li key={sku.id} className="flex items-center justify-between gap-4 py-3">
              <span className="min-w-0 text-sm font-medium text-neutral-900 dark:text-neutral-100">
                {sku.codigoInterno} — {sku.descripcionComercial}
              </span>
              <Button
                size="sm"
                aria-label={`Agregar ${sku.codigoInterno}`}
                onClick={() => onElegir(sku)}
              >
                <Plus className="size-4" aria-hidden="true" />
                Agregar
              </Button>
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}
