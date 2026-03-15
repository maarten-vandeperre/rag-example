import Navigation from './Navigation';

function Layout({ userRole, children }) {
  return (
    <div className="workspace-layout">
      <header className="workspace-layout__header">
        <div>
          <span className="workspace-layout__eyebrow">Modular frontend</span>
          <h1>RAG workspace</h1>
        </div>
        <Navigation userRole={userRole} />
      </header>
      <div className="workspace-layout__content">{children}</div>
    </div>
  );
}

export default Layout;
