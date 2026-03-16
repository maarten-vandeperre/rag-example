import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import ChatWorkspace from './ChatWorkspace';
import ApiClient from '../../services/ApiClient';

jest.mock('../../services/ApiClient');

const submitChatQuery = jest.fn();
const getAnswerSources = jest.fn();
const getDocumentContent = jest.fn();

beforeEach(() => {
  ApiClient.mockImplementation(() => ({
    submitChatQuery,
    getAnswerSources,
    getUserDocuments: jest.fn(),
    getDocumentContent,
    uploadDocument: jest.fn()
  }));
});

afterEach(() => {
  jest.resetAllMocks();
});

test('renders empty chat interface with input field', () => {
  render(<ChatWorkspace />);

  expect(screen.getByText(/chat with the documents your team trusts/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/ask about your uploaded documents/i)).toBeInTheDocument();
});

test('adds question and renders referenced answer', async () => {
  submitChatQuery.mockResolvedValue({
    success: true,
    answer: 'Your onboarding guide explains that PDFs, markdown files, and text notes are processed after upload and become searchable once indexing finishes.',
    documentReferences: [
      {
        documentName: 'onboarding-guide.pdf',
        paragraphReference: 'Paragraph 4',
        relevanceScore: 0.96,
        href: '#onboarding-guide'
      }
    ]
  });

  render(<ChatWorkspace />);

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'What does the onboarding guide say about upload?' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  expect(screen.getByText(/searching your documents/i)).toBeInTheDocument();
  expect(screen.getByText(/what does the onboarding guide say about upload/i)).toBeInTheDocument();

  await waitFor(() => {
    expect(screen.getByText(/pdfs, markdown files, and text notes are processed/i)).toBeInTheDocument();
  });
  expect(screen.getByText(/onboarding-guide.pdf/i)).toBeInTheDocument();
});

test('shows no answer found guidance when query has no match', async () => {
  submitChatQuery.mockResolvedValue({
    success: true,
    answer: "I couldn't find relevant information in your documents",
    documentReferences: []
  });

  render(<ChatWorkspace />);

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'Tell me about quarterly budgets' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  await waitFor(() => {
    expect(screen.getByText(/i couldn't find relevant information in your documents/i)).toBeInTheDocument();
  });
});

test('shows timeout error message for slow query keywords', async () => {
  submitChatQuery.mockRejectedValue(new Error('The query took too long to process. Please try again.'));

  render(<ChatWorkspace />);

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'This timeout looks slow' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  await waitFor(() => {
    expect(screen.getByText(/the query took too long to process/i)).toBeInTheDocument();
  });
});

test('opens answer details when an answer is clicked and closes it again', async () => {
  submitChatQuery.mockResolvedValueOnce({
    success: true,
    answer: 'Open the onboarding guide to review the upload checklist.',
    documentReferences: [
      {
        documentName: 'onboarding-guide.pdf',
        paragraphReference: 'Paragraph 4',
        relevanceScore: 0.96,
        href: '#onboarding-guide',
        snippet: 'The upload checklist explains how new files become searchable after indexing.'
      }
    ]
  });

  render(<ChatWorkspace />);

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'How do uploads become searchable?' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  await waitFor(() => {
    expect(screen.getByText(/open the onboarding guide to review the upload checklist/i)).toBeInTheDocument();
  });

  fireEvent.click(screen.getByRole('button', { name: /view answer details/i }));

  expect(screen.getByRole('dialog', { name: /review answer and sources/i })).toBeInTheDocument();
  expect(screen.getByText(/the upload checklist explains how new files become searchable/i)).toBeInTheDocument();

  fireEvent.click(screen.getByRole('button', { name: /close answer details/i }));

  expect(screen.queryByRole('dialog', { name: /review answer and sources/i })).not.toBeInTheDocument();
});

test('supports keyboard answer selection and updates details for a different answer', async () => {
  submitChatQuery
    .mockResolvedValueOnce({
      success: true,
      answer: 'The onboarding guide covers PDF indexing.',
      documentReferences: [
        {
          documentName: 'onboarding-guide.pdf',
          paragraphReference: 'Paragraph 2',
          relevanceScore: 0.9,
          snippet: 'PDF uploads are indexed once processing finishes.'
        }
      ]
    })
    .mockResolvedValueOnce({
      success: true,
      answer: 'The FAQ describes markdown note retrieval.',
      documentReferences: [
        {
          documentName: 'faq.md',
          paragraphReference: 'Section 4',
          relevanceScore: 0.88,
          snippet: 'Markdown notes follow the same retrieval flow as PDFs.'
        }
      ]
    });

  render(<ChatWorkspace />);

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'Tell me about PDFs' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  await waitFor(() => {
    expect(screen.getByText(/the onboarding guide covers pdf indexing/i)).toBeInTheDocument();
  });

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'Tell me about markdown' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  await waitFor(() => {
    expect(screen.getByText(/the faq describes markdown note retrieval/i)).toBeInTheDocument();
  });

  const answerButtons = screen.getAllByRole('button', { name: /view answer details/i });

  fireEvent.keyDown(answerButtons[0], { key: 'Enter' });
  expect(screen.getByRole('dialog')).toHaveTextContent(/the onboarding guide covers pdf indexing/i);

  fireEvent.keyDown(answerButtons[1], { key: ' ' });
  expect(screen.getByRole('dialog')).toHaveTextContent(/the faq describes markdown note retrieval/i);
  expect(screen.getByRole('dialog')).toHaveTextContent(/markdown notes follow the same retrieval flow as pdfs/i);
});

test('loads switchable source details and disables unavailable sources', async () => {
  submitChatQuery.mockResolvedValueOnce({
    answerId: 'answer-123',
    success: true,
    answer: 'The answer references multiple sources.',
    documentReferences: [
      { documentName: 'summary.pdf', paragraphReference: 'Paragraph 1', relevanceScore: 0.91 }
    ]
  });
  getAnswerSources.mockResolvedValueOnce({
    sources: [
      {
        sourceId: 'source-1',
        fileName: 'summary.pdf',
        fileType: 'PDF',
        paragraphReference: 'Paragraph 1',
        relevanceScore: 0.91,
        available: true,
        snippet: {
          content: 'Summary source snippet.',
          context: 'Summary context.'
        },
        metadata: {
          title: 'Executive Summary',
          chunkIndex: 1
        }
      },
      {
        sourceId: 'source-2',
        fileName: 'appendix.md',
        fileType: 'MARKDOWN',
        paragraphReference: 'Section 3',
        relevanceScore: 0.85,
        available: true,
        snippet: {
          content: 'Appendix source snippet.',
          context: 'Appendix context.'
        },
        metadata: {
          title: 'Appendix',
          chunkIndex: 3
        }
      },
      {
        sourceId: 'source-3',
        fileName: 'archived.txt',
        fileType: 'PLAIN_TEXT',
        paragraphReference: 'Section 8',
        relevanceScore: 0.4,
        available: false,
        snippet: {
          content: 'Archived snippet.',
          context: ''
        },
        metadata: {
          title: 'Archived source',
          chunkIndex: 8
        }
      }
    ]
  });

  render(<ChatWorkspace />);

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'Show me all sources' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  await waitFor(() => {
    expect(screen.getByText(/the answer references multiple sources/i)).toBeInTheDocument();
  });

  fireEvent.click(screen.getByRole('button', { name: /view answer details/i }));

  await waitFor(() => {
    expect(getAnswerSources).toHaveBeenCalledWith('answer-123');
  });

  await waitFor(() => {
    expect(screen.getByRole('heading', { name: /executive summary/i })).toBeInTheDocument();
  });

  fireEvent.click(screen.getByRole('tab', { name: /2\. appendix\.md/i }));

  expect(screen.getByRole('heading', { name: /appendix/i })).toBeInTheDocument();
  expect(screen.getByText(/appendix source snippet/i)).toBeInTheDocument();
  expect(screen.getByRole('tab', { name: /3\. archived\.txt unavailable/i })).toBeDisabled();
});

test('opens the full document viewer from a source', async () => {
  submitChatQuery.mockResolvedValueOnce({
    answerId: 'answer-456',
    success: true,
    answer: 'The answer includes a document you can open.',
    documentReferences: [
      { documentName: 'summary.pdf', paragraphReference: 'Paragraph 1', relevanceScore: 0.91 }
    ]
  });
  getAnswerSources.mockResolvedValueOnce({
    sources: [
      {
        sourceId: 'source-1',
        documentId: 'doc-7',
        fileName: 'summary.pdf',
        fileType: 'PDF',
        paragraphReference: 'Paragraph 1',
        relevanceScore: 0.91,
        available: true,
        snippet: {
          content: 'Summary source snippet.',
          context: 'Summary context.'
        },
        metadata: {
          title: 'Executive Summary',
          chunkIndex: 1
        }
      }
    ]
  });
  getDocumentContent.mockResolvedValueOnce({
    documentId: 'doc-7',
    fileName: 'summary.pdf',
    fileType: 'PDF',
    content: 'Full summary document content for review.',
    metadata: {
      pageCount: 1
    }
  });

  render(<ChatWorkspace />);

  fireEvent.change(screen.getByLabelText(/ask about your uploaded documents/i), {
    target: { value: 'Open the full document' }
  });
  fireEvent.click(screen.getByRole('button', { name: /ask/i }));

  await waitFor(() => {
    expect(screen.getByText(/the answer includes a document you can open/i)).toBeInTheDocument();
  });

  fireEvent.click(screen.getByRole('button', { name: /view answer details/i }));

  await waitFor(() => {
    expect(screen.getByRole('heading', { name: /executive summary/i })).toBeInTheDocument();
  });

  fireEvent.click(screen.getAllByRole('button', { name: /view full document/i })[0]);

  expect(getDocumentContent).toHaveBeenCalledWith('doc-7');
  expect(await screen.findByText(/full summary document content for review/i)).toBeInTheDocument();
});
