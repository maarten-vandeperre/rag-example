import AuthGuard from '../../../components/AuthGuard';
import AdminProgress from '../../../components/AdminProgress/AdminProgress';

function AdminPanel({ apiUrl, userId, userRole }) {
  return (
    <AuthGuard requireAdmin={true}>
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
        <AdminProgress apiUrl={apiUrl} userId={userId} userRole={userRole} />
      </main>
    </AuthGuard>
  );
}

export default AdminPanel;
