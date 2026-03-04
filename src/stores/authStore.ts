import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { Session, User } from '@supabase/supabase-js';
import { supabase } from 'boot/supabase';

export const useAuthStore = defineStore('auth', () => {
  const session = ref<Session | null>(null);
  const user = ref<User | null>(null);
  const isInitialized = ref(false);

  const isAuthenticated = computed(() => !!session.value);

  const userName = computed(() => {
    if (!user.value) return '';
    return (user.value.user_metadata?.full_name as string) ?? user.value.email ?? '';
  });

  const userAvatar = computed<string | null>(() => {
    return (user.value?.user_metadata?.avatar_url as string) ?? null;
  });

  const userInitials = computed(() => {
    if (!userName.value) return '?';
    return userName.value
      .split(' ')
      .map((n) => n[0])
      .slice(0, 2)
      .join('')
      .toUpperCase();
  });

  async function initialize() {
    if (isInitialized.value) return;

    const {
      data: { session: currentSession },
    } = await supabase.auth.getSession();

    session.value = currentSession;
    user.value = currentSession?.user ?? null;
    isInitialized.value = true;

    supabase.auth.onAuthStateChange((_event, newSession) => {
      session.value = newSession;
      user.value = newSession?.user ?? null;
    });
  }

  async function signOut() {
    await supabase.auth.signOut();
    session.value = null;
    user.value = null;
  }

  return {
    session,
    user,
    isAuthenticated,
    isInitialized,
    userName,
    userAvatar,
    userInitials,
    initialize,
    signOut,
  };
});
