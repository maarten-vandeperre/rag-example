import { fireEvent, render, screen } from '@testing-library/react';

import SourceSnippetDisplay from './SourceSnippetDisplay';

const source = {
  fileName: 'guide.pdf',
  fileType: 'PDF',
  relevanceScore: 0.92,
  available: true,
  metadata: {
    title: 'Onboarding Guide',
    pageNumber: 5,
    chunkIndex: 2
  },
  snippet: {
    content: 'Uploads are indexed after processing.',
    context: 'The workflow highlights indexing after upload validation.'
  }
};

test('renders source content and actions', () => {
  const onViewFullDocument = jest.fn();

  render(<SourceSnippetDisplay onViewFullDocument={onViewFullDocument} source={source} />);

  expect(screen.getByRole('heading', { name: /onboarding guide/i })).toBeInTheDocument();
  expect(screen.getByText(/uploads are indexed after processing/i)).toBeInTheDocument();
  expect(screen.getByText(/workflow highlights indexing after upload validation/i)).toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: /view full document/i }));
  expect(onViewFullDocument).toHaveBeenCalledWith(source);
});

test('renders error and retry controls', () => {
  const onRetry = jest.fn();

  render(<SourceSnippetDisplay error="Source request failed." onRetry={onRetry} onViewFullDocument={jest.fn()} source={source} />);

  expect(screen.getByRole('alert')).toHaveTextContent(/source request failed/i);
  fireEvent.click(screen.getByRole('button', { name: /try again/i }));
  expect(onRetry).toHaveBeenCalled();
});

test('renders unavailable source state', () => {
  render(<SourceSnippetDisplay onViewFullDocument={jest.fn()} source={{ ...source, available: false }} />);

  expect(screen.getByText(/source unavailable/i)).toBeInTheDocument();
});
