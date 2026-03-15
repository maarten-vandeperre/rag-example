import AdminPanel from './components/AdminPanel';
import LoginForm from './components/LoginForm';
import UserProfile from './components/UserProfile';
import { useAuth } from './hooks/useAuth';
import { useUserProfile } from './hooks/useUserProfile';

export { AdminPanel, LoginForm, UserProfile, useAuth, useUserProfile };

export const UserManagementModule = {
  name: 'user-management',
  version: '1.0.0',
  components: { AdminPanel, LoginForm, UserProfile },
  hooks: { useAuth, useUserProfile }
};
