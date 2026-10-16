import { terminalSchema } from './terminal.schema';

const valid = {
  codigo: 'POS001',
  nombre: 'Caja 1',
  serieBoletaDefecto: '',
  serieFacturaDefecto: '',
  numeroSerieEquipo: '',
  hostname: '',
  ipEquipo: '',
  impresoraCodigo: '',
  storeEdgeHabilitado: false,
  estado: 'ACTIVO'
};

function messages(overrides: Record<string, unknown>) {
  const result = terminalSchema.safeParse({ ...valid, ...overrides });
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('terminalSchema', () => {
  it('acepta un terminal válido con los opcionales vacíos', () => {
    expect(terminalSchema.safeParse(valid).success).toBe(true);
  });

  it('acepta series, IPv4 e IPv6 válidas', () => {
    expect(
      messages({ serieBoletaDefecto: 'B001', serieFacturaDefecto: 'F0A1', ipEquipo: '10.0.0.15' })
    ).toEqual([]);
    expect(messages({ ipEquipo: '2001:db8::1' })).toEqual([]);
  });

  it.each([
    [{ codigo: '' }, 'El código es obligatorio.'],
    [{ codigo: 'x'.repeat(41) }, 'El código no debe exceder 40 caracteres.'],
    [{ nombre: 'A' }, 'El nombre debe tener al menos 2 caracteres.'],
    [{ nombre: 'x'.repeat(121) }, 'El nombre no debe exceder 120 caracteres.'],
    [{ serieBoletaDefecto: 'F001' }, 'La serie de boleta debe iniciar con B y tener 4 caracteres.'],
    [
      { serieFacturaDefecto: 'B001' },
      'La serie de factura debe iniciar con F y tener 4 caracteres.'
    ],
    [{ numeroSerieEquipo: 'x'.repeat(121) }, 'El número de serie no debe exceder 120 caracteres.'],
    [{ hostname: 'x'.repeat(151) }, 'El hostname no debe exceder 150 caracteres.'],
    [{ ipEquipo: '999.1.1.1' }, 'La dirección IP no es válida.'],
    [
      { impresoraCodigo: 'x'.repeat(101) },
      'El código de impresora no debe exceder 100 caracteres.'
    ],
    [{ estado: 'OFFLINE' }, 'Selecciona un estado.']
  ])('rechaza %j con "%s"', (overrides, message) => {
    expect(messages(overrides)).toContain(message);
  });
});
