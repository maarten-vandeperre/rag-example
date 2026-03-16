import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import AppRoutes from './AppRoutes';

jest.mock('../pages/knowledge-graph/KnowledgeGraphListPage', () => () => <div>Knowledge graph list page</div>);
jest.mock('../pages/knowledge-graph/KnowledgeGraphDetailPage', () => () => <div>Knowledge graph detail page</div>);
jest.mock('../pages/knowledge-graph/KnowledgeGraphSearchPage', () => () => <div>Knowledge graph search page</div>);

describe('AppRoutes knowledge graph access', () => {
  test('renders access denied for non-admin users on knowledge graph routes', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/knowledge-graph']}>
        <AppRoutes apiUrl="/api" userId="user-1" userRole="STANDARD" />
      </MemoryRouter>
    );

    expect(await screen.findByText(/knowledge graph administration is restricted/i)).toBeInTheDocument();
  });

  test('renders knowledge graph page for admins', async () => {
    render(
      <MemoryRouter initialEntries={['/admin/knowledge-graph']}>
        <AppRoutes apiUrl="/api" userId="admin-1" userRole="ADMIN" />
      </MemoryRouter>
    );

    expect(await screen.findByText(/knowledge graph list page/i)).toBeInTheDocument();
  });
});
