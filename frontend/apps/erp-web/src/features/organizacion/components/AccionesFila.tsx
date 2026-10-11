import { Eye, Pencil } from 'lucide-react';
import { Link } from 'react-router';
import { IconButton, iconButtonClassName } from '@boticas/ui-web';

type AccionesFilaProps = {
  nombre: string;
  detalleHref: string;
  onEditar: () => void;
};

export function AccionesFila({ nombre, detalleHref, onEditar }: AccionesFilaProps) {
  return (
    <div className="flex items-center gap-1">
      <Link
        to={detalleHref}
        aria-label={`Ver detalle de ${nombre}`}
        title={`Ver detalle de ${nombre}`}
        className={iconButtonClassName()}
      >
        <Eye className="size-4.5" aria-hidden="true" />
      </Link>
      <IconButton icon={Pencil} label={`Editar ${nombre}`} onClick={onEditar} />
    </div>
  );
}
