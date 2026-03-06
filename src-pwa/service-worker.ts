/*
 * Gala PWA Service Worker
 * Handles push notifications and offline caching.
 *
 * Built only when running: quasar build -m pwa
 */

/// <reference lib="webworker" />
declare const self: ServiceWorkerGlobalScope;

import { clientsClaim } from 'workbox-core';
import {
  precacheAndRoute,
  cleanupOutdatedCaches,
  createHandlerBoundToURL,
} from 'workbox-precaching';
import { registerRoute, NavigationRoute } from 'workbox-routing';

void self.skipWaiting();
clientsClaim();

// Workbox injects precache manifest here
precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();

// SPA fallback to index.html
if (process.env.MODE !== 'ssr' || process.env.PROD) {
  registerRoute(
    new NavigationRoute(
      createHandlerBoundToURL(process.env.PWA_FALLBACK_HTML),
      {
        denylist: [
          new RegExp(process.env.PWA_SERVICE_WORKER_REGEX),
          /workbox-(.)*\.js$/,
        ],
      },
    ),
  );
}

// ─── Push Notifications ───────────────────────────────────────────────────────

interface PushPayload {
  title: string;
  body: string;
  url?: string;
  icon?: string;
}

self.addEventListener('push', (event: PushEvent) => {
  if (!event.data) return;

  let payload: PushPayload = { title: 'Gala', body: 'You have a new update.' };

  try {
    payload = event.data.json() as PushPayload;
  } catch {
    payload.body = event.data.text();
  }

  event.waitUntil(
    self.registration.showNotification(payload.title, {
      body: payload.body,
      icon: payload.icon ?? '/icons/icon-192x192.png',
      badge: '/icons/icon-128x128.png',
      data: { url: payload.url ?? '/' },
    } as NotificationOptions),
  );
});

self.addEventListener('notificationclick', (event: NotificationEvent) => {
  event.notification.close();
  const url = (event.notification.data as { url?: string })?.url ?? '/';

  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      const existing = clients.find((c) => 'focus' in c);
      if (existing) {
        void existing.navigate(url);
        return existing.focus();
      }
      return self.clients.openWindow(url);
    }),
  );
});
