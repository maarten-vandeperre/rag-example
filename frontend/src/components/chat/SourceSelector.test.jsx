import { fireEvent, render, screen } from '@testing-library/react';

import SourceSelector from './SourceSelector';

const sources = [
  { sourceId: 'source-1', fileName: 'guide.pdf', paragraphReference: 'Paragraph 1', available: true },
  { sourceId: 'source-2', fileName: 'faq.md', paragraphReference: 'Section 4', available: true },
  { sourceId: 'source-3', fileName: 'archive.txt', paragraphReference: 'Paragraph 9', available: false }
];

test('renders selectable sources and disables unavailable items', () => {
  const onSourceSelect = jest.fn();

  render(<SourceSelector onSourceSelect={onSourceSelect} selectedSource={sources[0]} sources={sources} />);

  expect(screen.getByRole('tablist', { name: /available sources/i })).toBeInTheDocument();
  expect(screen.getByRole('tab', { name: /1\. guide\.pdf/i })).toHaveAttribute('aria-selected', 'true');
  expect(screen.getByRole('tab', { name: /3\. archive\.txt unavailable/i })).toBeDisabled();

  fireEvent.click(screen.getByRole('tab', { name: /2\. faq\.md/i }));
  expect(onSourceSelect).toHaveBeenCalledWith(sources[1]);
});

test('supports keyboard navigation and selection', () => {
  const onSourceSelect = jest.fn();

  render(<SourceSelector onSourceSelect={onSourceSelect} selectedSource={sources[0]} sources={sources} />);

  const firstTab = screen.getByRole('tab', { name: /1\. guide\.pdf/i });
  firstTab.focus();

  fireEvent.keyDown(firstTab, { key: 'ArrowRight' });
  expect(screen.getByRole('tab', { name: /2\. faq\.md/i })).toHaveFocus();

  fireEvent.keyDown(screen.getByRole('tab', { name: /2\. faq\.md/i }), { key: 'Enter' });
  expect(onSourceSelect).toHaveBeenCalledWith(sources[1]);
});
