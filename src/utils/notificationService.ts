/**
 * Gala Push Notification Service
 *
 * Setup required:
 *  1. Generate VAPID keys:  npx web-push generate-vapid-keys
 *  2. Add to .env:          VITE_VAPID_PUBLIC_KEY=<your-public-key>
 *  3. Add to Supabase Edge Function env:
 *       VAPID_PUBLIC_KEY, VAPID_PRIVATE_KEY, VAPID_CONTACT_EMAIL
 *  4. Deploy the Edge Function:  supabase functions deploy send-push
 */

import { supabase } from 'boot/supabase';

const VAPID_PUBLIC_KEY = import.meta.env.VITE_VAPID_PUBLIC_KEY as string | undefined;

// ─── Capability checks ────────────────────────────────────────────────────────

export function isPushSupported(): boolean {
  return (
    'Notification' in window &&
    'serviceWorker' in navigator &&
    'PushManager' in window
  );
}

export function getPermissionStatus(): NotificationPermission {
  if (!isPushSupported()) return 'denied';
  return Notification.permission;
}

// ─── Subscription management ──────────────────────────────────────────────────

function urlBase64ToUint8Array(base64: string): Uint8Array<ArrayBuffer> {
  const padding = '='.repeat((4 - (base64.length % 4)) % 4);
  const b64 = (base64 + padding).replace(/-/g, '+').replace(/_/g, '/');
  const raw = atob(b64);
  const arr = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) {
    arr[i] = raw.charCodeAt(i);
  }
  return arr;
}

export async function requestPermission(): Promise<NotificationPermission> {
  if (!isPushSupported()) return 'denied';
  return Notification.requestPermission();
}

export async function subscribeUser(userId: string): Promise<boolean> {
  if (!isPushSupported() || !VAPID_PUBLIC_KEY) {
    console.warn('[push] VITE_VAPID_PUBLIC_KEY is not set or push not supported');
    return false;
  }

  try {
    const permission = await requestPermission();
    if (permission !== 'granted') return false;

    const reg = await navigator.serviceWorker.ready;
    const subscription = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY),
    });

    const json = subscription.toJSON() as {
      endpoint: string;
      keys: { p256dh: string; auth: string };
    };

    const { error } = await supabase.from('push_subscriptions').upsert(
      {
        user_id: userId,
        endpoint: json.endpoint,
        p256dh: json.keys.p256dh,
        auth_key: json.keys.auth,
      },
      { onConflict: 'user_id,endpoint' },
    );

    if (error) throw error;
    return true;
  } catch (err) {
    console.error('[push] Subscribe failed:', err);
    return false;
  }
}

export async function unsubscribeUser(userId: string): Promise<void> {
  if (!isPushSupported()) return;

  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();

    if (sub) {
      const endpoint = sub.endpoint;
      await sub.unsubscribe();
      await supabase
        .from('push_subscriptions')
        .delete()
        .eq('user_id', userId)
        .eq('endpoint', endpoint);
    }
  } catch (err) {
    console.error('[push] Unsubscribe failed:', err);
  }
}

export async function isSubscribed(): Promise<boolean> {
  if (!isPushSupported()) return false;
  try {
    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.getSubscription();
    return sub !== null;
  } catch {
    return false;
  }
}

// ─── Send helpers (calls Supabase Edge Function) ──────────────────────────────

interface PushNotificationPayload {
  trip_id: string;
  exclude_user_id?: string | undefined; // don't notify the actor themselves
  title: string;
  body: string;
  url?: string | undefined;
}

export async function sendPushToTripMembers(payload: PushNotificationPayload): Promise<void> {
  try {
    await supabase.functions.invoke('send-push', { body: payload });
  } catch (err) {
    // Non-fatal — push is best-effort
    console.warn('[push] send-push invoke failed:', err);
  }
}
