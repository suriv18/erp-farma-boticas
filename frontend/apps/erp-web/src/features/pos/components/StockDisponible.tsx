import { useQuery } from '@tanstack/react-query';
import { posicionesQuery } from '../../inventario';

type StockDisponibleProps = { almacenId: string; skuId: string };

export function StockDisponible({ almacenId, skuId }: StockDisponibleProps) {
  const { data, isPending, isError } = useQuery({
    ...posicionesQuery({ almacenId, skuId, size: 100 }),
    enabled: almacenId !== ''
  });

  if (almacenId === '') return <span>Selecciona un almacén para ver el stock.</span>;
  if (isError) return <span>Stock: —</span>;
  if (isPending) return <span>Stock: …</span>;
  const total = data.items
    .filter(({ vendible }) => vendible)
    .reduce((acumulado, { cantidadDisponible }) => acumulado + cantidadDisponible, 0);
  return <span>Stock: {total}</span>;
}
