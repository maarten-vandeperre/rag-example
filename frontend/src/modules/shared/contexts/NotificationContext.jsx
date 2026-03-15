import { createContext, useContext, useMemo, useState } from 'react';

const NotificationContext = createContext({
  notifications: [],
  pushNotification: () => {}
});

export function NotificationProvider({ children }) {
  const [notifications, setNotifications] = useState([]);

  const value = useMemo(() => ({
    notifications,
    pushNotification(message, level = 'info') {
      setNotifications((current) => [...current, { id: current.length + 1, message, level }]);
    }
  }), [notifications]);

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>;
}

export function useNotifications() {
  return useContext(NotificationContext);
}
