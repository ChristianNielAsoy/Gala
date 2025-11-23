<template>
  <q-page class="flex flex-center bg-grey-2">
    <q-card class="q-pa-md shadow-2 my_card" style="width: 90%; max-width: 400px;">
      <q-card-section class="text-center">
        <div class="text-h5 text-weight-bold text-primary">GALA App</div>
        <div class="text-subtitle1 text-grey-7 q-mt-sm">Trip Expense Management</div>
      </q-card-section>

      <q-card-section>
        <q-form @submit.prevent="handleLogin">
          <q-input
            v-model="email"
            label="Email"
            type="email"
            outlined
            rounded
            dense
            class="q-mb-md"
            lazy-rules
            :rules="[val => !!val || 'Email is required']"
          />
          <q-input
            v-model="password"
            label="Password"
            type="password"
            outlined
            rounded
            dense
            class="q-mb-lg"
            lazy-rules
            :rules="[val => !!val || 'Password is required']"
          />

          <q-btn
            type="submit"
            color="primary"
            rounded
            size="lg"
            label="Login"
            class="full-width text-capitalize"
            :loading="loading"
          />
        </q-form>
      </q-card-section>

      <q-card-section class="text-center q-pt-none">
        <q-btn flat dense color="grey-7" label="Don't have an account? Sign Up" to="/signup" />
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
// Import the direct export of the Supabase client
import { supabase } from 'boot/supabase';

const $q = useQuasar();
const router = useRouter();

const email = ref<string>('');
const password = ref<string>('');
const loading = ref<boolean>(false);

async function handleLogin() {
  loading.value = true;

  // Basic Input Validation
  if (!email.value || !password.value) {
    $q.notify({ type: 'warning', message: 'Please fill in both fields.' });
    loading.value = false;
    return;
  }

  // Call Supabase Auth
  const { error } = await supabase.auth.signInWithPassword({
    email: email.value,
    password: password.value,
  });

  loading.value = false;

  // Handle result
  if (error) {
    // Show error message from Supabase (e.g., "Invalid login credentials")
    $q.notify({
      type: 'negative',
      message: error.message,
      position: 'top',
    });
  } else {
    // Success: The router guard automatically redirects to /dashboard
    router.push('/dashboard');
  }
}
</script>

<style scoped>
.my_card {
  border-radius: 12px;
}
</style>
