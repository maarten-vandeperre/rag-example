import './App.css';
import AdminProgress from './components/AdminProgress/AdminProgress';
import DocumentLibrary from './components/DocumentLibrary/DocumentLibrary';

function App() {
  const apiUrl = process.env.REACT_APP_API_URL || '/api';
  const currentUserRole = process.env.REACT_APP_USER_ROLE || 'ADMIN';
  const currentUserId = process.env.REACT_APP_USER_ID || '11111111-1111-1111-1111-111111111111';

  if (currentUserRole === 'ADMIN') {
    return (
      <main className="admin-shell">
        <section className="admin-shell__hero">
          <div>
            <span className="admin-shell__eyebrow">Private knowledge base</span>
            <h1>Admin progress overview</h1>
            <p>
              Monitor ingestion health, investigate failures, and watch active document processing across the platform.
            </p>
          </div>
        </section>
        <AdminProgress apiUrl={apiUrl} userId={currentUserId} userRole={currentUserRole} />
      </main>
    );
  }

  return <DocumentLibrary apiBaseUrl={apiUrl} userId={currentUserId} />;
}

export default App;
