import { register } from 'register-service-worker';

// Register the service worker in production
register(process.env.SERVICE_WORKER_FILE, {
  ready() {
    // Service worker is active
  },
  registered() {
    // Service worker has been registered
  },
  cached() {
    // Content has been cached for offline use
  },
  updatefound() {
    // New content is downloading
  },
  updated() {
    // New content is available; please refresh.
    window.location.reload();
  },
  offline() {
    console.warn('Gala is running offline.');
  },
  error(err) {
    console.error('Service worker registration error:', err);
  },
});
