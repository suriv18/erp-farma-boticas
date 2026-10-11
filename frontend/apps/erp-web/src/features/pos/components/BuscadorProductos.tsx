import { useState, type SubmitEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Search } from 'lucide-react';
import { Button, Card } from '@boticas/ui-web';
import { TextField } from '../../../shared/components/FormFields';
import { formatoMoneda } from '../../../shared/lib/format';
import { skusQuery, type SkuResumen } from '../../catalogo';
import { esCodigoBarras } from '../lib/codigo-barras';
import { StockDisponible } from './StockDisponible';

type BuscadorProductosProps = {
  almacenId: string;
  deshabilitado: boolean;
  onElegir: (sku: SkuResumen) => void;
};

export function BuscadorProductos({ almacenId, deshabilitado, onElegir }: BuscadorProductosProps) {
  const queryClient = useQueryClient();
  const [texto, setTexto] = useState('');
  const [termino, setTermino] = useState('');
  const { data, isError, isFetching } = useQuery({
    ...skusQuery({ q: termino, estado: 'ACTIVO', size: 10 }),
    enabled: termino !== ''
  });
  const buscarUnico = async (buscado: string) => {
    const pagina = await queryClient
      .fetchQuery(skusQuery({ q: buscado, estado: 'ACTIVO', size: 10 }))
      .catch(() => undefined);
    return pagina?.items.length === 1 ? pagina.items[0] : undefined;
  };

  const resultados = data?.items ?? [];

  const buscar = async (evento: SubmitEvent<HTMLFormElement>) => {
    evento.preventDefault();
    const buscado = texto.trim();
    const unico = esCodigoBarras(buscado) ? await buscarUnico(buscado) : undefined;
    if (unico === undefined) {
      setTermino(buscado);
      return;
    }
    onElegir(unico);
    setTexto('');
    setTermino('');
  };

  const buscando = termino !== '';
  const sinResultados = buscando && !isError && !isFetching && resultados.length === 0;

  return (
    <Card className="space-y-4 p-5">
      <form
        className="flex items-end gap-3"
        onSubmit={(evento) => {
          void buscar(evento);
        }}
      >
        <div className="min-w-0 flex-1">
          <TextField
            id="pos-buscar"
            label="Buscar producto"
            icon={Search}
            autoFocus
            placeholder="Código, descripción o código de barras"
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
            <li key={sku.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 py-3">
              <span className="min-w-0 flex-1 basis-56 text-sm font-medium text-neutral-900 dark:text-neutral-100">
                {sku.codigoInterno} — {sku.descripcionComercial}
              </span>
              <span className="flex flex-col items-end">
                <span className="text-sm font-semibold text-neutral-900 tabular-nums dark:text-neutral-100">
                  {sku.precioVentaReferencia === null
                    ? 'Sin precio'
                    : formatoMoneda(sku.precioVentaReferencia)}
                </span>
                <StockDisponible almacenId={almacenId} skuId={sku.id} />
              </span>
              <Button
                size="sm"
                aria-label={`Agregar ${sku.codigoInterno}`}
                disabled={deshabilitado}
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
