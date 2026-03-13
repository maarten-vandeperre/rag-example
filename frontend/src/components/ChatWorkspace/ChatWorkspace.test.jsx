import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import ChatWorkspace from './ChatWorkspace';
import ApiClient from '../../services/ApiClient';

jest.mock('../../services/ApiClient');

const submitChatQuery = jest.fn();

beforeEach(() => {
  ApiClient.mockImplementation(() => ({
    submitChatQuery,
    getUserDocuments: jest.fn(),
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
