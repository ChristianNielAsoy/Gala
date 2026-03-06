  -- =============================================
  -- GALA APP - Migration v8
  -- Push notification subscriptions
  -- Safe to run multiple times
  -- =============================================

  -- Stores browser Web Push subscriptions per user.
  -- One user can have multiple subscriptions (desktop + mobile, etc.)
  CREATE TABLE IF NOT EXISTS push_subscriptions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    endpoint    TEXT        NOT NULL,
    p256dh      TEXT        NOT NULL,  -- DH public key from PushSubscription
    auth_key    TEXT        NOT NULL,  -- Auth secret from PushSubscription
    user_agent  TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW(),

    UNIQUE (user_id, endpoint)
  );

  ALTER TABLE push_subscriptions ENABLE ROW LEVEL SECURITY;

  -- Users manage only their own subscriptions
  CREATE POLICY "push_subscriptions_select" ON push_subscriptions
    FOR SELECT USING (user_id = auth.uid());

  CREATE POLICY "push_subscriptions_insert" ON push_subscriptions
    FOR INSERT WITH CHECK (user_id = auth.uid());

  CREATE POLICY "push_subscriptions_delete" ON push_subscriptions
    FOR DELETE USING (user_id = auth.uid());

  -- Service role (Edge Function) needs SELECT across all users
  -- This is handled by the service role key bypassing RLS.

  -- =============================================
  -- SUMMARY
  -- push_subscriptions : new table
  -- =============================================
