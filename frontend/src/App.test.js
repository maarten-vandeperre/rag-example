import { render, screen } from '@testing-library/react';

import App from './App';

beforeEach(() => {
  global.fetch = jest.fn().mockResolvedValue({
    ok: true,
    json: async () => ({
      statistics: {
        totalDocuments: 8,
        uploadedCount: 2,
        processingCount: 1,
        readyCount: 4,
        failedCount: 1,
      },
      failedDocuments: [],
      processingDocuments: [],
    }),
  });
});

afterEach(() => {
  jest.resetAllMocks();
});

test('renders admin progress overview shell', async () => {
  render(<App />);

  expect(screen.getByText(/admin progress overview/i)).toBeInTheDocument();
  expect(await screen.findByText(/document processing progress/i)).toBeInTheDocument();
});
