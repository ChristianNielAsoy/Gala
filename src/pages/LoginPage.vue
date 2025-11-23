<template>
  <div class="flex flex-center bg-grey-2" style="min-height: 100vh;">
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
            :rules="[(val: string) => !!val || 'Email is required']"
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
            :rules="[(val: string) => !!val || 'Password is required']"
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';

const $q = useQuasar();
const router = useRouter();

const email = ref<string>('');
const password = ref<string>('');
const loading = ref<boolean>(false);

async function handleLogin(): Promise<void> {
  loading.value = true;

  if (!email.value || !password.value) {
    $q.notify({ type: 'warning', message: 'Please fill in both fields.' });
    loading.value = false;
    return;
  }

  const { error } = await supabase.auth.signInWithPassword({
    email: email.value,
    password: password.value,
  });

  loading.value = false;

  if (error) {
    $q.notify({
      type: 'negative',
      message: error.message,
      position: 'top',
    });
  } else {
    void router.push('/dashboard');
  }
}
</script>

<style scoped>
.my_card {
  border-radius: 12px;
}
</style>
