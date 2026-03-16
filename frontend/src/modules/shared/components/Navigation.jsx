import { NavLink } from 'react-router-dom';

function Navigation({ userRole }) {
  return (
    <nav className="workspace-nav" aria-label="Primary">
      <NavLink to="/documents">Documents</NavLink>
      <NavLink to="/chat">Chat</NavLink>
      <NavLink to="/profile">Profile</NavLink>
      {userRole === 'ADMIN' ? <NavLink to="/admin">Admin</NavLink> : null}
      {userRole === 'ADMIN' ? <NavLink to="/admin/knowledge-graph">Knowledge Graphs</NavLink> : null}
    </nav>
  );
}

export default Navigation;
