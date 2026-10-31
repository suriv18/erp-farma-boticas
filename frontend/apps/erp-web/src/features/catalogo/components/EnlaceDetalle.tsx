import { Eye } from 'lucide-react';
import { Link, useLocation } from 'react-router';
import { iconButtonClassName } from '@boticas/ui-web';

export type EnlaceDetalleProps = {
  nombre: string;
  href: string;
};

export function EnlaceDetalle({ nombre, href }: EnlaceDetalleProps) {
  const { search } = useLocation();
  return (
    <Link
      to={href + search}
      aria-label={`Ver detalle de ${nombre}`}
      title={`Ver detalle de ${nombre}`}
      className={iconButtonClassName()}
    >
      <Eye className="size-4.5" aria-hidden="true" />
    </Link>
  );
}
