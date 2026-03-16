import { fireEvent, render, screen } from '@testing-library/react';

import ErrorWarningDisplay from './ErrorWarningDisplay';

test('renders and dismisses no-sources warning', () => {
  const onDismissWarning = jest.fn();

  render(
    <ErrorWarningDisplay
      errors={[]}
      onDismissError={jest.fn()}
      onDismissWarning={onDismissWarning}
      onRetry={jest.fn()}
      warnings={[{ id: 'warning-no-sources', type: 'NO_SOURCES' }]}
    />
  );

  expect(screen.getByText(/no source information available/i)).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: /understood/i }));
  expect(onDismissWarning).toHaveBeenCalledWith('warning-no-sources');
});

test('renders partial failure details', () => {
  render(
    <ErrorWarningDisplay
      errors={[]}
      onDismissError={jest.fn()}
      onDismissWarning={jest.fn()}
      onRetry={jest.fn()}
      warnings={[{
        id: 'warning-partial-failure',
        type: 'PARTIAL_FAILURE',
        totalSources: 3,
        availableSources: 1,
        failedSources: [{ fileName: 'archive.txt' }, { fileName: 'history.pdf' }]
      }]}
    />
  );

  expect(screen.getByText(/1 of 3 sources can be shown right now/i)).toBeInTheDocument();
  expect(screen.getByText(/archive.txt, history.pdf could not be loaded/i)).toBeInTheDocument();
});

test('renders source error with retry action', () => {
  const onRetry = jest.fn();
  const onDismissError = jest.fn();

  render(
    <ErrorWarningDisplay
      errors={[{ id: 'error-source-load', code: 'NETWORK_ERROR', message: 'network failed', failedSources: [{ fileName: 'guide.pdf' }] }]}
      onDismissError={onDismissError}
      onDismissWarning={jest.fn()}
      onRetry={onRetry}
      warnings={[]}
    />
  );

  expect(screen.getByRole('alert')).toHaveTextContent(/service is unreachable/i);
  fireEvent.click(screen.getByRole('button', { name: /try again/i }));
  expect(onRetry).toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));
  expect(onDismissError).toHaveBeenCalledWith('error-source-load');
});
