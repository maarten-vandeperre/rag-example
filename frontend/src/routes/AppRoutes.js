import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import { LoadingSpinner } from '../modules/shared';
import KnowledgeGraphAccessDenied from '../components/knowledge-graph/KnowledgeGraphAccessDenied';

const DocumentLibrary = lazy(() => import('../modules/document-management').then((module) => ({ default: module.DocumentLibrary })));
const ChatWorkspace = lazy(() => import('../modules/chat-system').then((module) => ({ default: module.ChatWorkspace })));
const UserProfile = lazy(() => import('../modules/user-management').then((module) => ({ default: module.UserProfile })));
const AdminPanel = lazy(() => import('../modules/user-management').then((module) => ({ default: module.AdminPanel })));
const KnowledgeGraphListPage = lazy(() => import('../pages/knowledge-graph/KnowledgeGraphListPage'));
const KnowledgeGraphDetailPage = lazy(() => import('../pages/knowledge-graph/KnowledgeGraphDetailPage'));
const KnowledgeGraphSearchPage = lazy(() => import('../pages/knowledge-graph/KnowledgeGraphSearchPage'));

function renderAdminRoute(userRole, element) {
  return userRole === 'ADMIN' ? element : <KnowledgeGraphAccessDenied />;
}

function AppRoutes({ apiUrl, userId, userRole }) {
  const defaultRoute = userRole === 'ADMIN' ? '/admin' : '/documents';

  return (
    <Suspense fallback={<LoadingSpinner />}>
      <Routes>
        <Route path="/" element={<Navigate to={defaultRoute} replace />} />
        <Route path="/documents" element={<DocumentLibrary apiBaseUrl={apiUrl} userId={userId} />} />
        <Route path="/chat" element={<ChatWorkspace apiBaseUrl={apiUrl} userId={userId} />} />
        <Route path="/profile" element={<UserProfile userId={userId} userRole={userRole} />} />
        <Route path="/admin" element={<AdminPanel apiUrl={apiUrl} userId={userId} userRole={userRole} />} />
        <Route path="/admin/knowledge-graph" element={renderAdminRoute(userRole, <KnowledgeGraphListPage apiUrl={apiUrl} userId={userId} />)} />
        <Route path="/admin/knowledge-graph/search" element={renderAdminRoute(userRole, <KnowledgeGraphSearchPage apiUrl={apiUrl} userId={userId} />)} />
        <Route path="/admin/knowledge-graph/:graphId" element={renderAdminRoute(userRole, <KnowledgeGraphDetailPage apiUrl={apiUrl} userId={userId} />)} />
      </Routes>
    </Suspense>
  );
}

export default AppRoutes;
