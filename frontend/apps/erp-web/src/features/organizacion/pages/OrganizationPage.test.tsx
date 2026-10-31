import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '../../../test/mocks/server';
import { renderRoute } from '../../../test/render-route';
import { OrganizationPage } from './OrganizationPage';

const structureUrl = '*/api/v1/estructura-corporativa';

const structure = {
  asOf: '2026-08-31T20:00:00-05:00',
  companies: [
    {
      id: 'empresa-1',
      legalName: 'Boticas del Pacífico S.A.C.',
      tradeName: 'Boticas Pacífico',
      status: 'ACTIVE',
      establishments: [
        {
          id: 'est-1',
          code: 'LIM-001',
          name: 'Botica Miraflores',
          status: 'ACTIVE',
          timeZone: 'America/Lima',
          warehouses: [
            { id: 'alm-1', code: 'ALM-01', name: 'Almacén principal', status: 'ACTIVE' }
          ],
          cashRegisters: [{ id: 'caj-1', code: 'CAJ-01', name: 'Caja principal', status: 'ACTIVE' }]
        },
        {
          id: 'est-2',
          code: 'LIM-002',
          name: 'Botica Surco',
          status: 'SUSPENDED',
          timeZone: 'America/Lima',
          warehouses: [],
          cashRegisters: []
        }
      ]
    },
    {
      id: 'empresa-2',
      legalName: 'Inversiones Andinas S.A.C.',
      tradeName: null,
      status: 'INACTIVE',
      establishments: [
        {
          id: 'est-3',
          code: 'CUS-001',
          name: 'Botica Cusco',
          status: 'INACTIVE',
          timeZone: 'America/Lima',
          warehouses: [],
          cashRegisters: []
        }
      ]
    }
  ]
};

function renderPage() {
  return renderRoute('/organizacion', OrganizationPage, '/organizacion');
}

describe('OrganizationPage', () => {
  it('muestra la estructura corporativa con enlaces a los detalles', async () => {
    server.use(http.get(structureUrl, () => HttpResponse.json(structure)));

    renderPage();

    expect(screen.getByRole('heading', { name: 'Organización' })).toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'Boticas Pacífico' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-1'
    );
    expect(screen.getByRole('link', { name: 'Inversiones Andinas S.A.C.' })).toHaveAttribute(
      'href',
      '/organizacion/empresas/empresa-2'
    );
    expect(screen.getByRole('link', { name: 'Botica Miraflores' })).toHaveAttribute(
      'href',
      '/organizacion/establecimientos/est-1'
    );
    expect(screen.getByRole('link', { name: 'Gestionar empresas' })).toHaveAttribute(
      'href',
      '/organizacion/empresas'
    );
    expect(screen.getByText(/Almacén principal/u)).toBeInTheDocument();
    expect(screen.getByText('Caja principal')).toBeInTheDocument();
    expect(screen.getAllByText('Sin almacenes visibles').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Sin cajas visibles').length).toBeGreaterThan(0);
    expect(screen.getByText(/Fecha de corte/u)).toBeInTheDocument();
  });

  it('traduce los estados activo, suspendido e inactivo', async () => {
    server.use(http.get(structureUrl, () => HttpResponse.json(structure)));

    renderPage();

    await screen.findByRole('link', { name: 'Boticas Pacífico' });
    expect(screen.getAllByText('Activo').length).toBe(2);
    expect(screen.getAllByText('Suspendido').length).toBe(1);
    expect(screen.getAllByText('Inactivo').length).toBe(2);
  });

  it('muestra los contadores de la estructura', async () => {
    server.use(http.get(structureUrl, () => HttpResponse.json(structure)));

    renderPage();

    expect(screen.getAllByText('—').length).toBe(4);
    await screen.findByRole('link', { name: 'Boticas Pacífico' });
    expect(screen.queryByText('—')).not.toBeInTheDocument();
    const summary = within(screen.getByRole('region', { name: 'Resumen de estructura' }));
    expect(summary.getByText('Empresas').nextElementSibling).toHaveTextContent('2');
    expect(summary.getByText('Establecimientos').nextElementSibling).toHaveTextContent('3');
    expect(summary.getByText('Almacenes').nextElementSibling).toHaveTextContent('1');
    expect(summary.getByText('Cajas').nextElementSibling).toHaveTextContent('1');
  });

  it('informa cuando no se puede obtener la estructura', async () => {
    server.use(
      http.get(structureUrl, () => HttpResponse.json({ title: 'Error' }, { status: 500 }))
    );

    renderPage();

    expect(
      await screen.findByText(/No fue posible obtener la estructura corporativa/u)
    ).toBeInTheDocument();
  });
});
