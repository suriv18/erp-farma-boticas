import { Card } from '@boticas/ui-web';
import { SelectField, TextField } from '../../../shared/components/FormFields';
import type { EstablishmentStructure } from '../../organizacion';
import type { Proveedor } from '../api/proveedores.types';
import { MONEDAS, type CabeceraOrden, type ErroresCabecera } from '../lib/orden-cabecera';

type OrdenCabeceraFormProps = {
  valores: CabeceraOrden;
  errores: ErroresCabecera;
  proveedores: Proveedor[];
  establecimientos: EstablishmentStructure[];
  onCambiar: (campo: keyof CabeceraOrden, valor: string) => void;
};

export function OrdenCabeceraForm({
  valores,
  errores,
  proveedores,
  establecimientos,
  onCambiar
}: OrdenCabeceraFormProps) {
  return (
    <Card className="grid gap-4 p-6 sm:grid-cols-2">
      <SelectField
        id="orden-proveedor"
        label="Proveedor"
        error={errores.proveedorId}
        value={valores.proveedorId}
        onChange={(event) => onCambiar('proveedorId', event.target.value)}
      >
        <option value="">Selecciona un proveedor</option>
        {proveedores.map(({ id, razonSocial }) => (
          <option key={id} value={id}>
            {razonSocial}
          </option>
        ))}
      </SelectField>
      <SelectField
        id="orden-destino"
        label="Establecimiento de destino"
        error={errores.establecimientoDestinoId}
        value={valores.establecimientoDestinoId}
        onChange={(event) => onCambiar('establecimientoDestinoId', event.target.value)}
      >
        <option value="">Selecciona un establecimiento</option>
        {establecimientos.map(({ id, name }) => (
          <option key={id} value={id}>
            {name}
          </option>
        ))}
      </SelectField>
      <TextField
        id="orden-entrega"
        label="Fecha de entrega estimada"
        type="date"
        error={errores.fechaEntregaEstimada}
        value={valores.fechaEntregaEstimada}
        onChange={(event) => onCambiar('fechaEntregaEstimada', event.target.value)}
      />
      <SelectField
        id="orden-moneda"
        label="Moneda"
        error={errores.moneda}
        value={valores.moneda}
        onChange={(event) => onCambiar('moneda', event.target.value)}
      >
        {MONEDAS.map((moneda) => (
          <option key={moneda} value={moneda}>
            {moneda}
          </option>
        ))}
      </SelectField>
      <TextField
        id="orden-tipo-cambio"
        label="Tipo de cambio"
        inputMode="decimal"
        error={errores.tipoCambio}
        value={valores.tipoCambio}
        onChange={(event) => onCambiar('tipoCambio', event.target.value)}
      />
      <TextField
        id="orden-condicion-pago"
        label="Condición de pago"
        error={errores.condicionPago}
        value={valores.condicionPago}
        onChange={(event) => onCambiar('condicionPago', event.target.value)}
      />
      <TextField
        id="orden-dias-credito"
        label="Días de crédito"
        inputMode="numeric"
        error={errores.diasCredito}
        value={valores.diasCredito}
        onChange={(event) => onCambiar('diasCredito', event.target.value)}
      />
      <TextField
        id="orden-observacion"
        label="Observación"
        error={errores.observacion}
        value={valores.observacion}
        onChange={(event) => onCambiar('observacion', event.target.value)}
      />
      <p className="text-xs text-neutral-500 sm:col-span-2 dark:text-neutral-400">
        Al elegir un proveedor se cargan su moneda y condiciones de pago; puedes modificarlas.
      </p>
    </Card>
  );
}
