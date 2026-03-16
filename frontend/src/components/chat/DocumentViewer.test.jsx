import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import { clearDocumentCache } from '../../utils/documentCache';
import DocumentViewer from './DocumentViewer';

const apiClient = {
  getDocumentContent: jest.fn()
};

const documentSummary = {
  documentId: 'doc-1',
  fileName: 'guide.pdf',
  fileType: 'PDF'
};

beforeEach(() => {
  clearDocumentCache();
  apiClient.getDocumentContent.mockReset();
});

test('loads and displays full document content with search controls', async () => {
  apiClient.getDocumentContent.mockResolvedValue({
    documentId: 'doc-1',
    fileName: 'guide.pdf',
    fileType: 'PDF',
    content: 'Uploads are indexed after processing. Uploads remain searchable for answers.',
    metadata: {
      pageCount: 2
    }
  });

  render(
    <DocumentViewer
      apiClient={apiClient}
      document={documentSummary}
      highlightSnippet={{ content: 'indexed after processing' }}
      onClose={jest.fn()}
    />
  );

  await waitFor(() => {
    expect(screen.getByText(/showing extracted text from the uploaded pdf/i)).toBeInTheDocument();
  });

  fireEvent.change(screen.getByLabelText(/search in document/i), {
    target: { value: 'uploads' }
  });

  expect(screen.getByText(/1 of 2/i)).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /next/i }));
  expect(screen.getByText(/2 of 2/i)).toBeInTheDocument();
});

test('shows retry state when document loading fails', async () => {
  const onClose = jest.fn();
  apiClient.getDocumentContent.mockRejectedValueOnce(new Error('Document request failed.'));

  render(
    <DocumentViewer
      apiClient={apiClient}
      document={documentSummary}
      highlightSnippet={null}
      onClose={onClose}
    />
  );

  await waitFor(() => {
    expect(screen.getByRole('alert')).toHaveTextContent(/document request failed/i);
  });

  fireEvent.click(screen.getByRole('button', { name: /close/i }));
  expect(onClose).toHaveBeenCalled();
});
