import { fireEvent, render, screen, waitFor } from '@testing-library/react';

import AdminProgress from './AdminProgress';

const progressPayload = {
  statistics: {
    totalDocuments: 10,
    uploadedCount: 2,
    processingCount: 2,
    readyCount: 5,
    failedCount: 1,
  },
  failedDocuments: [
    {
      documentId: 'failed-1',
      fileName: 'invoice.pdf',
      uploadedBy: 'alice',
      uploadedAt: '2026-03-13T12:00:00Z',
      failureReason: 'Embedding provider timed out',
      fileSize: 2048,
    },
  ],
  processingDocuments: [
    {
      documentId: 'processing-1',
      fileName: 'contract.pdf',
      uploadedBy: 'bob',
      uploadedAt: '2026-03-13T12:10:00Z',
      processingStartedAt: '2026-03-13T12:15:00Z',
    },
  ],
};

describe('AdminProgress', () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2026-03-13T12:16:05Z'));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('renders statistics and document tables for admins', async () => {
    const fetchImpl = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => progressPayload,
    });

    render(
      <AdminProgress
        apiUrl="/api"
        userId="11111111-1111-1111-1111-111111111111"
        userRole="ADMIN"
        fetchImpl={fetchImpl}
      />
    );

    expect(await screen.findByText('invoice.pdf')).toBeInTheDocument();
    expect(screen.getByText('contract.pdf')).toBeInTheDocument();
    expect(screen.getByText('1m 5s')).toBeInTheDocument();
    expect(fetchImpl).toHaveBeenCalledWith('/api/admin/documents/progress', expect.objectContaining({
      headers: expect.objectContaining({ 'X-User-Id': '11111111-1111-1111-1111-111111111111' }),
    }));
  });

  it('shows access denied for non-admin users', () => {
    const fetchImpl = jest.fn();

    render(<AdminProgress apiUrl="/api" userId="user-1" userRole="STANDARD" fetchImpl={fetchImpl} />);

    expect(screen.getByText(/access denied/i)).toBeInTheDocument();
    expect(fetchImpl).not.toHaveBeenCalled();
  });

  it('refreshes data when the refresh button is clicked', async () => {
    const fetchImpl = jest.fn().mockResolvedValue({
      ok: true,
      json: async () => progressPayload,
    });

    render(<AdminProgress apiUrl="/api" userId="admin-1" userRole="ADMIN" fetchImpl={fetchImpl} />);

    await screen.findByText('invoice.pdf');
    fireEvent.click(screen.getByRole('button', { name: /refresh data/i }));

    await waitFor(() => expect(fetchImpl).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(screen.queryByText(/loading admin progress/i)).not.toBeInTheDocument());
  });
});
