import React from 'react';

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  render() {
    if (this.state.hasError) {
      return (
        <section className="module-error-boundary">
          <h1>Something went wrong.</h1>
          <p>Refresh the page to continue working with the knowledge base.</p>
        </section>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
