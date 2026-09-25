import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post('*/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as { login?: string; password?: string };
    if (body.password !== 'Boticas2026!') {
      return HttpResponse.json({ title: 'Credenciales incorrectas o cuenta bloqueada.' }, { status: 401 });
    }
    return HttpResponse.json({
      accessToken: 'mock-access-token',
      refreshToken: 'mock-refresh-token',
      tokenType: 'Bearer',
      accessExpiresAt: '2026-08-31T12:10:00-05:00',
      refreshExpiresAt: '2026-09-07T12:00:00-05:00',
      tenantId: '11111111-1111-1111-1111-111111111111',
      userId: '22222222-2222-2222-2222-222222222222',
      sessionId: '33333333-3333-3333-3333-333333333333',
      passwordChangeRequired: false
    });
  }),
  http.get('*/api/v1/dashboard/summary', () =>
    HttpResponse.json({
      salesToday: 8420.5,
      transactionsToday: 48,
      stockUnits: 4286,
      lowStockProducts: 12,
      expiringLots: 8,
      activeCustomers: 326,
      asOf: '2026-08-31T12:00:00-05:00'
    })
  ),
  http.get('*/api/v1/estructura-corporativa', () =>
    HttpResponse.json({
      asOf: '2026-08-31T20:00:00-05:00',
      companies: [
        {
          id: '8a9773f5-8b95-4d2a-956f-252f433634f4',
          legalName: 'Boticas del Pacífico S.A.C.',
          tradeName: 'Boticas Pacífico',
          status: 'ACTIVE',
          establishments: [
            {
              id: '7a74eca6-1e32-4f90-b18a-d4f7e75358f2',
              code: 'LIM-001',
              name: 'Botica Miraflores',
              status: 'ACTIVE',
              timeZone: 'America/Lima',
              warehouses: [
                {
                  id: 'ef4a6317-d7dd-4da2-a4a1-04b333607d2d',
                  code: 'ALM-01',
                  name: 'Almacén principal',
                  status: 'ACTIVE'
                },
                {
                  id: '6ee07658-b411-45a7-8b10-89e81ed44d23',
                  code: 'CUA-01',
                  name: 'Zona de cuarentena',
                  status: 'ACTIVE'
                }
              ],
              cashRegisters: [
                {
                  id: 'bd304cd4-252e-457e-b8fb-02e54f885b25',
                  code: 'CAJ-01',
                  name: 'Caja principal',
                  status: 'ACTIVE'
                }
              ]
            }
          ]
        }
      ]
    })
  ),
  http.get('*/api/v1/catalogo/marcas', () =>
    HttpResponse.json({
      items: [
        { id: 'marca-1', tenantId: '11111111-1111-1111-1111-111111111111', codigo: 'BAYER', nombre: 'Bayer', descripcion: null, estado: 'ACTIVO' }
      ],
      page: 0,
      size: 20,
      totalElements: 1
    })
  ),
  http.get('*/api/v1/catalogo/categorias', () =>
    HttpResponse.json({
      items: [
        {
          id: 'categoria-1',
          tenantId: '11111111-1111-1111-1111-111111111111',
          categoriaPadreId: null,
          codigo: 'ANALGESICOS',
          nombre: 'Analgésicos',
          descripcion: null,
          nivel: 1,
          orden: 1,
          estado: 'ACTIVO'
        }
      ],
      page: 0,
      size: 20,
      totalElements: 1
    })
  ),
  http.get('*/api/v1/catalogo/rubros-comerciales', () =>
    HttpResponse.json({
      items: [
        {
          id: 'rubro-1',
          tenantId: '11111111-1111-1111-1111-111111111111',
          codigo: 'FARMA',
          nombre: 'Farmacéutico',
          descripcion: null,
          esFarmaceutico: true,
          orden: 1,
          estado: 'ACTIVO'
        }
      ],
      page: 0,
      size: 20,
      totalElements: 1
    })
  ),
  http.get('*/api/v1/catalogo/condiciones-venta', () =>
    HttpResponse.json([
      {
        codigo: 'VL',
        denominacion: 'Venta libre',
        requiereReceta: false,
        requiereRetencion: false,
        fuente: 'DIGEMID',
        versionFuente: '2026',
        vigenteDesde: null,
        vigenteHasta: null,
        estado: 'ACTIVO'
      },
      {
        codigo: 'RM',
        denominacion: 'Con receta médica',
        requiereReceta: true,
        requiereRetencion: false,
        fuente: 'DIGEMID',
        versionFuente: '2026',
        vigenteDesde: null,
        vigenteHasta: null,
        estado: 'ACTIVO'
      }
    ])
  ),
  http.get('*/api/v1/catalogo/formas-farmaceuticas', () =>
    HttpResponse.json([
      { codigo: 'TAB', denominacion: 'Tableta', fuente: 'DIGEMID', estado: 'ACTIVO' },
      { codigo: 'JBE', denominacion: 'Jarabe', fuente: 'DIGEMID', estado: 'ACTIVO' }
    ])
  ),
  http.get('*/api/v1/catalogo/vias-administracion', () =>
    HttpResponse.json([
      { codigo: 'ORAL', denominacion: 'Vía oral', fuente: 'DIGEMID', estado: 'ACTIVO' },
      { codigo: 'IV', denominacion: 'Vía intravenosa', fuente: 'DIGEMID', estado: 'ACTIVO' }
    ])
  ),
  http.get('*/api/v1/catalogo/unidades-medida', () =>
    HttpResponse.json([
      { codigo: 'UND', denominacion: 'Unidad', simbolo: 'u', permiteDecimal: false, fuente: 'DIGEMID', estado: 'ACTIVO' },
      { codigo: 'MG', denominacion: 'Miligramo', simbolo: 'mg', permiteDecimal: true, fuente: 'DIGEMID', estado: 'ACTIVO' }
    ])
  ),
  http.get('*/api/v1/catalogo/clasificaciones-controladas', () =>
    HttpResponse.json([
      {
        codigo: 'IIA',
        denominacion: 'Lista II-A',
        normaFuente: 'DS 023-2001-SA',
        requiereRecetaEspecial: true,
        retieneReceta: true,
        vigenciaRecetaDias: 30,
        estado: 'ACTIVO'
      },
      {
        codigo: 'IIIA',
        denominacion: 'Lista III-A',
        normaFuente: 'DS 023-2001-SA',
        requiereRecetaEspecial: false,
        retieneReceta: false,
        vigenciaRecetaDias: null,
        estado: 'ACTIVO'
      }
    ])
  )
];
