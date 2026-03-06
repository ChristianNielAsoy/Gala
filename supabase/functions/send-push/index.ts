// @ts-nocheck — Deno runtime; not compiled by vue-tsc
/**
 * Gala - send-push Edge Function
 *
 * Sends Web Push notifications to all members of a trip.
 * Called from the Gala frontend via supabase.functions.invoke('send-push').
 *
 * Required Supabase Edge Function environment variables:
 *   VAPID_PUBLIC_KEY     — from: npx web-push generate-vapid-keys
 *   VAPID_PRIVATE_KEY    — from: npx web-push generate-vapid-keys
 *   VAPID_CONTACT_EMAIL  — e.g. mailto:you@example.com
 *
 * Deploy: supabase functions deploy send-push
 */

import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
// @ts-ignore — web-push Deno-compatible fork
import webpush from 'npm:web-push';

interface RequestPayload {
  trip_id: string;
  exclude_user_id?: string;
  title: string;
  body: string;
  url?: string;
}

Deno.serve(async (req: Request) => {
  // CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response(null, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'authorization, content-type',
      },
    });
  }

  if (req.method !== 'POST') {
    return new Response('Method not allowed', { status: 405 });
  }

  // Verify caller is an authenticated Supabase user
  const authHeader = req.headers.get('Authorization');
  if (!authHeader) {
    return new Response('Unauthorized', { status: 401 });
  }

  const supabaseUrl = Deno.env.get('SUPABASE_URL') ?? '';
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '';
  const vapidPublic = Deno.env.get('VAPID_PUBLIC_KEY') ?? '';
  const vapidPrivate = Deno.env.get('VAPID_PRIVATE_KEY') ?? '';
  const vapidEmail = Deno.env.get('VAPID_CONTACT_EMAIL') ?? 'mailto:admin@example.com';

  // Verify JWT with the anon client
  const anonClient = createClient(supabaseUrl, Deno.env.get('SUPABASE_ANON_KEY') ?? '', {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: { user }, error: authError } = await anonClient.auth.getUser();
  if (authError || !user) {
    return new Response('Unauthorized', { status: 401 });
  }

  const payload = await req.json() as RequestPayload;
  const { trip_id, exclude_user_id, title, body, url = '/' } = payload;

  // Use service-role client to read subscriptions (bypasses RLS)
  const admin = createClient(supabaseUrl, serviceRoleKey);

  // Get all members of the trip who have push subscriptions
  const { data: members } = await admin
    .from('members')
    .select('user_id')
    .eq('trip_id', trip_id)
    .not('user_id', 'is', null);

  const userIds = (members ?? [])
    .map((m: { user_id: string }) => m.user_id)
    .filter((id: string) => id && id !== exclude_user_id);

  if (!userIds.length) {
    return new Response(JSON.stringify({ sent: 0 }), {
      headers: { 'Content-Type': 'application/json' },
    });
  }

  const { data: subscriptions } = await admin
    .from('push_subscriptions')
    .select('endpoint, p256dh, auth_key')
    .in('user_id', userIds);

  webpush.setVapidDetails(vapidEmail, vapidPublic, vapidPrivate);

  const notification = JSON.stringify({ title, body, url });

  const results = await Promise.allSettled(
    (subscriptions ?? []).map((sub: { endpoint: string; p256dh: string; auth_key: string }) =>
      webpush.sendNotification(
        { endpoint: sub.endpoint, keys: { p256dh: sub.p256dh, auth: sub.auth_key } },
        notification,
      ),
    ),
  );

  const sent = results.filter((r) => r.status === 'fulfilled').length;
  const failed = results.length - sent;

  return new Response(JSON.stringify({ sent, failed }), {
    headers: {
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
    },
  });
});
