import { useNotificationStore } from '../context/notificationStore';

const NotificationCenter = () => {
  const notifications = useNotificationStore((state) => state.notifications);
  const remove = useNotificationStore((state) => state.remove);

  if (!notifications.length) {
    return null;
  }

  return (
    <div className="notification-center" role="status" aria-live="polite">
      {notifications.map((item) => (
        <div key={item.id} className={`notification notification--${item.type}`}>
          <span>{item.message}</span>
          <button type="button" aria-label="Закрыть уведомление" onClick={() => remove(item.id)}>
            ×
          </button>
        </div>
      ))}
    </div>
  );
};

export default NotificationCenter;

