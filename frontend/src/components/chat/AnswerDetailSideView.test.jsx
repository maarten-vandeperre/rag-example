import { fireEvent, render, screen } from '@testing-library/react';
import { waitFor } from '@testing-library/react';

import { clearDocumentCache } from '../../utils/documentCache';
import AnswerDetailSideView from './AnswerDetailSideView';

const answer = {
  content: 'The workspace stores uploaded PDFs and markdown files for later retrieval.\n\nIndexed answers stay tied to their source material.',
  timestamp: '09:41'
};

beforeEach(() => {
  clearDocumentCache();
});

const sources = [
  {
    sourceId: 'source-1',
    documentId: 'doc-1',
    fileName: 'knowledge-base.pdf',
    fileType: 'PDF',
    paragraphReference: 'Paragraph 2',
    relevanceScore: 0.93,
    snippet: {
      content: 'Uploaded PDFs are parsed and indexed before they become searchable.',
      context: 'The ingestion flow makes uploaded files searchable after indexing.'
    },
    metadata: {
      title: 'Knowledge Base',
      chunkIndex: 2
    },
    available: true
  },
  {
    sourceId: 'source-2',
    documentId: 'doc-2',
    fileName: 'faq.md',
    fileType: 'MARKDOWN',
    paragraphReference: 'Section 4',
    relevanceScore: 0.74,
    snippet: {
      content: 'Markdown notes can also be retrieved through the same answer flow.',
      context: 'Markdown retrieval follows the same answer detail experience.'
    },
    metadata: {
      title: 'FAQ',
      chunkIndex: 4
    },
    available: true
  }
];

function renderComponent(overrides = {}) {
  const onClose = jest.fn();
  const onSourceSelect = jest.fn();
  const onViewFullDocument = jest.fn();
  const apiClient = {
    getDocumentContent: jest.fn().mockResolvedValue({
      documentId: 'doc-1',
      fileName: 'knowledge-base.pdf',
      fileType: 'PDF',
      content: 'Full document content that includes the selected source.',
      metadata: {
        pageCount: 2
      }
    })
  };

  render(
    <AnswerDetailSideView
      answer={answer}
      error=""
      isOpen
      loading={false}
      apiClient={apiClient}
      onClose={onClose}
      onSourceSelect={onSourceSelect}
      onViewFullDocument={onViewFullDocument}
      selectedSource={sources[0]}
      sources={sources}
      {...overrides}
    />
  );

  return { apiClient, onClose, onSourceSelect, onViewFullDocument };
}

test('renders answer details and source actions', () => {
  const { onSourceSelect, onViewFullDocument } = renderComponent();

  expect(screen.getByRole('dialog', { name: /review answer and sources/i })).toBeInTheDocument();
  expect(screen.getByText(/the workspace stores uploaded pdfs and markdown files/i)).toBeInTheDocument();
  expect(screen.getByText(/indexed answers stay tied to their source material/i)).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: /knowledge base/i })).toBeInTheDocument();
  expect(screen.getByText(/uploaded pdfs are parsed and indexed/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/available sources/i)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /close answer details/i })).toHaveFocus();

  fireEvent.click(screen.getByRole('tab', { name: /2\. faq\.md/i }));
  expect(onSourceSelect).toHaveBeenLastCalledWith(expect.objectContaining({
    sourceId: 'source-2',
    fileName: 'faq.md'
  }));

  fireEvent.click(screen.getByRole('button', { name: /view full document/i }));
  expect(onViewFullDocument).toHaveBeenCalledWith(expect.objectContaining({
    sourceId: 'source-1',
    fileName: 'knowledge-base.pdf'
  }));
});

test('renders selector retry and unavailable state', () => {
  renderComponent({
    selectedSource: { ...sources[0], available: false },
    sources: [{ ...sources[0], available: false }],
    error: ''
  });

  expect(screen.getByText(/source unavailable/i)).toBeInTheDocument();
});

test('shows a no-sources warning when answer references are missing', () => {
  renderComponent({ answer: { ...answer, answerId: 'answer-empty' }, selectedSource: null, sources: [] });

  expect(screen.getByText(/no source information available/i)).toBeInTheDocument();
});

test('shows partial failure warning for unavailable sources', () => {
  renderComponent({
    answer: { ...answer, answerId: 'answer-partial' },
    selectedSource: sources[0],
    sources: [...sources, { sourceId: 'source-3', fileName: 'archive.txt', paragraphReference: 'Paragraph 8', available: false }]
  });

  expect(screen.getByText(/some sources are unavailable/i)).toBeInTheDocument();
  expect(screen.getByText(/archive.txt could not be loaded/i)).toBeInTheDocument();
});

test('shows retryable source load error message', async () => {
  renderComponent({
    answer: { ...answer, answerId: 'answer-network' },
    apiClient: {
      getAnswerSources: jest.fn().mockRejectedValue({ message: 'Network failed', code: 'NETWORK_ERROR' }),
      getDocumentContent: jest.fn()
    },
    selectedSource: null,
    sources: []
  });

  expect(await screen.findByRole('alert')).toHaveTextContent(/service is unreachable/i);
  expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
});

test('opens full document viewer from the selected source', async () => {
  const { apiClient, onViewFullDocument } = renderComponent();

  fireEvent.click(screen.getByRole('button', { name: /view full document/i }));

  expect(onViewFullDocument).toHaveBeenCalled();
  await waitFor(() => {
    expect(apiClient.getDocumentContent).toHaveBeenCalled();
  });
  expect(await screen.findByText(/full document content that includes the selected source/i)).toBeInTheDocument();
});

test('supports overlay clicks and escape to close the panel', () => {
  const { onClose } = renderComponent();

  fireEvent.click(screen.getByTestId('answer-detail-backdrop'));
  expect(onClose).toHaveBeenCalledTimes(1);

  fireEvent.keyDown(document, { key: 'Escape' });
  expect(onClose).toHaveBeenCalledTimes(2);
});

test('renders loading state without answer content', () => {
  renderComponent({ loading: true });

  expect(screen.getByRole('status')).toHaveTextContent(/loading details/i);
  expect(screen.queryByText(/the workspace stores uploaded pdfs/i)).not.toBeInTheDocument();
});

test('renders a friendly error state', () => {
  renderComponent({ error: 'Source metadata could not be loaded.' });

  expect(screen.getByRole('alert')).toHaveTextContent(/unable to load answer details/i);
  expect(screen.getByRole('alert')).toHaveTextContent(/source metadata could not be loaded/i);
  expect(screen.getByRole('alert')).toHaveTextContent(/try another source or return to the chat/i);
});

test('does not render when the panel is closed', () => {
  renderComponent({ isOpen: false });

  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
