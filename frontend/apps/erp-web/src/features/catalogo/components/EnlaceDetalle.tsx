import { Eye } from 'lucide-react';
import { Link } from 'react-router';
import { iconButtonClassName } from '@boticas/ui-web';

export type EnlaceDetalleProps = {
  nombre: string;
  href: string;
};

export function EnlaceDetalle({ nombre, href }: EnlaceDetalleProps) {
  return (
    <Link
      to={href}
      aria-label={`Ver detalle de ${nombre}`}
      title={`Ver detalle de ${nombre}`}
      className={iconButtonClassName()}
    >
      <Eye className="size-4.5" aria-hidden="true" />
    </Link>
  );
}
