import { Card } from '@boticas/ui-web';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import type { OrganizationalNode } from '../../organizacion';
import {
  TIPOS_DOCUMENTO_PROVEEDOR,
  type CabeceraRecepcion,
  type ErroresCabeceraRecepcion
} from '../lib/recepcion-cabecera';

type RecepcionCabeceraFormProps = {
  valores: CabeceraRecepcion;
  errores: ErroresCabeceraRecepcion;
  almacenes: OrganizationalNode[];
  onCambiar: (campo: keyof CabeceraRecepcion, valor: string) => void;
};

type CampoTexto = Exclude<keyof CabeceraRecepcion, 'almacenId' | 'documentoProveedorTipo'>;

const CAMPOS_TEXTO: readonly { campo: CampoTexto; label: string; inputMode: 'text' | 'decimal' }[] =
  [
    { campo: 'documentoProveedorSerie', label: 'Serie del documento', inputMode: 'text' },
    { campo: 'documentoProveedorNumero', label: 'Número del documento', inputMode: 'text' },
    { campo: 'guiaRemisionRemitente', label: 'Guía de remisión del remitente', inputMode: 'text' },
    {
      campo: 'guiaRemisionTransportista',
      label: 'Guía de remisión del transportista',
      inputMode: 'text'
    },
    { campo: 'temperatura', label: 'Temperatura (°C)', inputMode: 'decimal' },
    { campo: 'humedad', label: 'Humedad relativa (%)', inputMode: 'decimal' },
    { campo: 'observacion', label: 'Observación', inputMode: 'text' }
  ];

export function RecepcionCabeceraForm({
  valores,
  errores,
  almacenes,
  onCambiar
}: RecepcionCabeceraFormProps) {
  return (
    <Card className="grid gap-4 p-6 sm:grid-cols-2">
      <SelectField
        id="recepcion-almacen"
        label="Almacén"
        error={errores.almacenId}
        value={valores.almacenId}
        onChange={(event) => onCambiar('almacenId', event.target.value)}
      >
        <option value="">Selecciona un almacén</option>
        {almacenes.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="recepcion-tipo-documento"
        label="Tipo de documento"
        value={valores.documentoProveedorTipo}
        onChange={(event) => onCambiar('documentoProveedorTipo', event.target.value)}
      >
        {TIPOS_DOCUMENTO_PROVEEDOR.map(({ codigo, nombre }) => (
          <option key={codigo} value={codigo}>
            {nombre}
          </option>
        ))}
      </SelectField>
      {CAMPOS_TEXTO.map(({ campo, label, inputMode }) => (
        <TextField
          key={campo}
          id={`recepcion-${campo}`}
          label={label}
          inputMode={inputMode}
          error={errores[campo]}
          value={valores[campo]}
          onChange={(event) => onCambiar(campo, event.target.value)}
        />
      ))}
    </Card>
  );
}
