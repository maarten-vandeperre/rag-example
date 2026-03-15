import { useUserProfile } from '../hooks/useUserProfile';

function UserProfile({ userId, userRole }) {
  const { profile } = useUserProfile({ userId, userRole });

  return (
    <section className="profile-card">
      <span className="workspace-layout__eyebrow">User management</span>
      <h2>User profile</h2>
      <dl>
        <div><dt>User ID</dt><dd>{profile.userId}</dd></div>
        <div><dt>Email</dt><dd>{profile.email}</dd></div>
        <div><dt>Role</dt><dd>{profile.role}</dd></div>
      </dl>
    </section>
  );
}

export default UserProfile;
