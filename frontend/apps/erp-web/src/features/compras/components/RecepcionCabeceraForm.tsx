import { Card } from '@boticas/ui-web';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import type { OrganizationalNode } from '../../organizacion';
import {
  TIPOS_DOCUMENTO_PROVEEDOR,
  type CabeceraRecepcion,
  type ErroresCabeceraRecepcion,
  type EstadoAlmacenes
} from '../lib/recepcion-cabecera';

type RecepcionCabeceraFormProps = {
  valores: CabeceraRecepcion;
  errores: ErroresCabeceraRecepcion;
  almacenes: OrganizationalNode[];
  estadoAlmacenes: EstadoAlmacenes;
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

const ID_ESTADO_ALMACENES = 'recepcion-almacen-estado';

const MENSAJES_ALMACENES: Partial<Record<EstadoAlmacenes, string>> = {
  cargando: 'Cargando almacenes…',
  error: 'No se pudieron cargar los almacenes. Recarga la página para reintentar.',
  vacio: 'El establecimiento de destino no tiene almacenes operativos.'
};

export function RecepcionCabeceraForm({
  valores,
  errores,
  almacenes,
  estadoAlmacenes,
  onCambiar
}: RecepcionCabeceraFormProps) {
  const mensajeAlmacenes = MENSAJES_ALMACENES[estadoAlmacenes];

  return (
    <Card className="grid gap-4 p-6 sm:grid-cols-2">
      <div>
        <SelectField
          id="recepcion-almacen"
          label="Almacén"
          error={errores.almacenId}
          value={valores.almacenId}
          disabled={mensajeAlmacenes !== undefined}
          onChange={(event) => onCambiar('almacenId', event.target.value)}
          {...(mensajeAlmacenes && !errores.almacenId
            ? { 'aria-describedby': ID_ESTADO_ALMACENES }
            : {})}
        >
          <option value="">Selecciona un almacén</option>
          {almacenes.map(({ id, name }) => (
            <option key={id} value={id}>
              {name}
            </option>
          ))}
        </SelectField>
        {mensajeAlmacenes ? (
          <p
            id={ID_ESTADO_ALMACENES}
            role="status"
            className="mt-1.5 text-xs text-neutral-500 dark:text-neutral-400"
          >
            {mensajeAlmacenes}
          </p>
        ) : null}
      </div>
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
