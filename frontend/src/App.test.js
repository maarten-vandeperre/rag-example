import { render, screen } from '@testing-library/react';

import App from './App';

beforeEach(() => {
  process.env.REACT_APP_SHOW_DEV_TOOLS = 'true';
  process.env.REACT_APP_DEBUG_MODE = 'true';
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

  expect(await screen.findByText(/admin progress overview/i)).toBeInTheDocument();
  expect(await screen.findByText(/document processing progress/i)).toBeInTheDocument();
  expect(await screen.findByText(/show dev tools/i)).toBeInTheDocument();
});
