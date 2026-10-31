export type TipoContacto = 'correo' | 'telefono' | 'web';

const HREFS: Record<TipoContacto, (valor: string) => string | null> = {
  correo: (valor) => (valor.includes('@') ? `mailto:${valor}` : null),
  telefono: (valor) => {
    const numero = valor.replace(/[^\d+]/g, '');
    return numero.length >= 6 ? `tel:${numero}` : null;
  },
  web: (valor) => (/^https?:\/\//i.test(valor) ? valor : null)
};

export function hrefContacto(tipo: TipoContacto, valor: string): string | null {
  return HREFS[tipo](valor.trim());
}
