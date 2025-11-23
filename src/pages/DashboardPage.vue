<template>
  <q-page class="q-pa-md">
    <div class="text-h5 q-mb-lg text-primary">Welcome Back!</div>

    <q-card class="q-mb-md shadow-2">
      <q-card-section>
        <div class="text-h6">Dashboard Overview</div>
        <div class="text-subtitle2 text-grey-7">Upcoming trips, quick balances, and recent activities will go here.</div>
      </q-card-section>
      <q-separator />
      <q-card-section class="flex flex-center" style="min-height: 200px;">
        <q-icon name="trending_up" size="xl" color="grey-4" />
        <p class="text-grey-6 q-mt-md">Analytics and quick stats coming soon...</p>
      </q-card-section>
    </q-card>

    <!-- Logout button for easy testing -->
    <q-btn
      label="Logout"
      @click="handleLogout"
      color="negative"
      flat
      class="full-width q-mt-xl"
    />
  </q-page>
</template>

<script setup lang="ts">
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';

const $q = useQuasar();
const router = useRouter();

async function handleLogout() {
  const { error } = await supabase.auth.signOut();

  if (error) {
    $q.notify({ type: 'negative', message: error.message });
  } else {
    // Redirect to login page after successful logout
    router.push('/login');
  }
}
</script>
