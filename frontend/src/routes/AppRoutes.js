import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';

import { LoadingSpinner } from '../modules/shared';

const DocumentLibrary = lazy(() => import('../modules/document-management').then((module) => ({ default: module.DocumentLibrary })));
const ChatWorkspace = lazy(() => import('../modules/chat-system').then((module) => ({ default: module.ChatWorkspace })));
const UserProfile = lazy(() => import('../modules/user-management').then((module) => ({ default: module.UserProfile })));
const AdminPanel = lazy(() => import('../modules/user-management').then((module) => ({ default: module.AdminPanel })));

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
      </Routes>
    </Suspense>
  );
}

export default AppRoutes;
