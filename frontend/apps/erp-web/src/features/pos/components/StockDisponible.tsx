import { useQuery } from '@tanstack/react-query';
import { posicionesQuery } from '../../inventario';

const ESTILO = 'text-xs text-neutral-500 tabular-nums dark:text-neutral-400';

type StockDisponibleProps = { almacenId: string; skuId: string };

export function StockDisponible({ almacenId, skuId }: StockDisponibleProps) {
  const { data, isPending, isError } = useQuery({
    ...posicionesQuery({ almacenId, skuId, size: 100 }),
    enabled: almacenId !== ''
  });

  if (almacenId === '')
    return <span className={ESTILO}>Selecciona un almacén para ver el stock.</span>;
  if (isError) return <span className={ESTILO}>Stock: —</span>;
  if (isPending) return <span className={ESTILO}>Stock: …</span>;
  const total = data.items
    .filter(({ vendible }) => vendible)
    .reduce((acumulado, { cantidadDisponible }) => acumulado + cantidadDisponible, 0);
  return <span className={ESTILO}>Stock: {total}</span>;
}
