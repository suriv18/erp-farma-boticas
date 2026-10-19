import { hrefContacto, type TipoContacto } from '../lib/contacto';
import { valueOrDash } from '../lib/format';
import { DatoItem } from './DatoItem';

type DatoContactoProps = {
  label: string;
  tipo: TipoContacto;
  valor: string | null;
};

export function DatoContacto({ label, tipo, valor }: DatoContactoProps) {
  const href = valor === null || valor === '' ? null : hrefContacto(tipo, valor);
  return (
    <DatoItem label={label}>
      {href === null ? (
        valueOrDash(valor)
      ) : (
        <a
          className="text-primary-700 dark:text-primary-400 hover:underline"
          href={href}
          {...(tipo === 'web' ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
        >
          {valor}
        </a>
      )}
    </DatoItem>
  );
}
