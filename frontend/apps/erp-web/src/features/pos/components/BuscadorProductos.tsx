import { useState, type SubmitEvent } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
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
    <Card>
      <form onSubmit={buscar}>
        <TextField
          id="pos-buscar"
          label="Buscar producto"
          placeholder="Código, descripción o código de barras"
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
        />
        <Button type="submit">Buscar</Button>
      </form>
      {buscando && isError ? <p>No se pudo buscar productos.</p> : null}
      {sinResultados ? <p>No se encontraron productos.</p> : null}
      {resultados.length > 0 ? (
        <ul>
          {resultados.map((sku) => (
            <li key={sku.id}>
              <span>
                {sku.codigoInterno} — {sku.descripcionComercial}
              </span>
              <span>
                {sku.precioVentaReferencia === null
                  ? 'Sin precio'
                  : formatoMoneda(sku.precioVentaReferencia)}
              </span>
              <StockDisponible almacenId={almacenId} skuId={sku.id} />
              <Button
                aria-label={`Agregar ${sku.codigoInterno}`}
                disabled={deshabilitado}
                onClick={() => onElegir(sku)}
              >
                Agregar {sku.codigoInterno}
              </Button>
            </li>
          ))}
        </ul>
      ) : null}
    </Card>
  );
}
