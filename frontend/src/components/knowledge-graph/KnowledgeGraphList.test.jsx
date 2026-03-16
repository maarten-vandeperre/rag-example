import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

import KnowledgeGraphList from './KnowledgeGraphList';

describe('KnowledgeGraphList', () => {
  test('renders graph cards and pagination actions', () => {
    const onPreviousPage = jest.fn();
    const onNextPage = jest.fn();

    render(
      <MemoryRouter>
        <KnowledgeGraphList
          graphs={[{ graphId: 'graph-1', name: 'Main Graph', totalNodes: 12, totalRelationships: 6 }]}
          statistics={{ totalGraphs: 1, totalNodes: 12, totalRelationships: 6 }}
          filter=""
          onFilterChange={jest.fn()}
          page={0}
          pageSize={1}
          onPreviousPage={onPreviousPage}
          onNextPage={onNextPage}
          loading={false}
          error={null}
        />
      </MemoryRouter>
    );

    expect(screen.getByText(/main graph/i)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /next/i }));
    expect(onNextPage).toHaveBeenCalled();
  });
});
