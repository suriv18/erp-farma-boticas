import { Eye, SlidersHorizontal } from 'lucide-react';
import { Link } from 'react-router';
import { IconButton, iconButtonClassName } from '@boticas/ui-web';

type AccionesPosicionProps = {
  loteId: string;
  numeroLote: string;
  onAjustar: () => void;
};

export function AccionesPosicion({ loteId, numeroLote, onAjustar }: AccionesPosicionProps) {
  return (
    <div className="flex items-center gap-1">
      <Link
        to={`/inventario/lotes/${loteId}`}
        aria-label={`Ver detalle del lote ${numeroLote}`}
        title={`Ver detalle del lote ${numeroLote}`}
        className={iconButtonClassName()}
      >
        <Eye className="size-4.5" aria-hidden="true" />
      </Link>
      <IconButton
        icon={SlidersHorizontal}
        label={`Ajustar stock del lote ${numeroLote}`}
        onClick={onAjustar}
      />
    </div>
  );
}
