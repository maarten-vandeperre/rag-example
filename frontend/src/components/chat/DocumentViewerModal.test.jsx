import { fireEvent, render, screen } from '@testing-library/react';

import DocumentViewerModal from './DocumentViewerModal';

test('renders modal and closes on backdrop or escape', () => {
  const onClose = jest.fn();

  render(
    <DocumentViewerModal isOpen onClose={onClose} title="Document viewer">
      <div>Viewer content</div>
    </DocumentViewerModal>
  );

  expect(screen.getByRole('dialog')).toBeInTheDocument();
  fireEvent.keyDown(document, { key: 'Escape' });
  expect(onClose).toHaveBeenCalledTimes(1);

  fireEvent.click(screen.getByTestId('document-viewer-backdrop'));
  expect(onClose).toHaveBeenCalledTimes(2);
});
