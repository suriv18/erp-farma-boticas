import { useQuery } from '@tanstack/react-query';
import { SelectField } from '../../../shared/components/FormFields';
import { skusQuery } from '../../catalogo';
import { codigoCorto } from '../lib/formato';

type SkuSelectProps = {
  id: string;
  label: string;
  placeholder: string;
  search: string;
  value: string;
  onChange: (skuId: string) => void;
  error?: string | undefined;
};

export function SkuSelect({
  id,
  label,
  placeholder,
  search,
  value,
  onChange,
  error
}: SkuSelectProps) {
  const { data } = useQuery(skusQuery({ q: search, estado: 'ACTIVO', size: 50 }));
  const items = data?.items ?? [];
  const faltaSeleccionado = value !== '' && !items.some((sku) => sku.id === value);

  return (
    <SelectField
      id={id}
      label={label}
      error={error}
      value={value}
      onChange={(event) => onChange(event.target.value)}
    >
      <option value="">{placeholder}</option>
      {faltaSeleccionado ? <option value={value}>{codigoCorto(value)}</option> : null}
      {items.map((sku) => (
        <option key={sku.id} value={sku.id}>
          {sku.codigoInterno} — {sku.descripcionComercial}
        </option>
      ))}
    </SelectField>
  );
}
