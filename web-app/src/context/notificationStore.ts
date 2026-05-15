import { create } from 'zustand';

type NotificationType = 'success' | 'error' | 'info';

export interface AppNotification {
  id: string;
  type: NotificationType;
  message: string;
}

interface NotificationState {
  notifications: AppNotification[];
  push: (payload: { type: NotificationType; message: string; duration?: number }) => void;
  remove: (id: string) => void;
}

const generateId = () => `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

export const useNotificationStore = create<NotificationState>((set, get) => ({
  notifications: [],
  push: ({ type, message, duration = 4000 }) => {
    const id = generateId();
    set((state) => ({
      notifications: [...state.notifications, { id, type, message }]
    }));
    if (duration > 0 && typeof window !== 'undefined') {
      window.setTimeout(() => {
        get().remove(id);
      }, duration);
    }
  },
  remove: (id) =>
    set((state) => ({
      notifications: state.notifications.filter((item) => item.id !== id)
    }))
}));

export const notifySuccess = (message: string) =>
  useNotificationStore.getState().push({ type: 'success', message });

export const notifyError = (message: string) =>
  useNotificationStore.getState().push({ type: 'error', message });

export const notifyInfo = (message: string) =>
  useNotificationStore.getState().push({ type: 'info', message });

