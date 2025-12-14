<template>
  <div class="flex flex-center bg-white" style="min-height: 100vh">
    <q-card class="q-pa-md shadow-2 my_card" style="width: 90%; max-width: 400px">
      <q-card-section class="text-center">
        <div class="text-h5 text-weight-bold text-primary">GALA App</div>
        <div class="text-subtitle1 text-grey-7 q-mt-sm">Create Your Account</div>
      </q-card-section>

      <q-card-section>
        <q-form @submit.prevent="handleSignup">
          <q-input
            v-model="name"
            label="Full Name"
            type="text"
            outlined
            rounded
            dense
            class="q-mb-md"
            lazy-rules
            :rules="[(val: string) => !!val || 'Name is required']"
          />

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
            class="q-mb-md"
            lazy-rules
            :rules="[(val: string) => val.length >= 6 || 'Password must be at least 6 characters']"
          />

          <q-input
            v-model="confirmPassword"
            label="Confirm Password"
            type="password"
            outlined
            rounded
            dense
            class="q-mb-lg"
            lazy-rules
            :rules="[(val: string) => val === password || 'Passwords do not match']"
          />

          <q-btn
            type="submit"
            color="primary"
            rounded
            size="lg"
            label="Sign Up"
            class="full-width text-capitalize"
            :loading="loading"
          />
        </q-form>
      </q-card-section>

      <q-card-section class="text-center q-pt-none">
        <q-btn flat dense color="grey-7" label="Already have an account? Login" to="/login" />
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

const name = ref<string>('');
const email = ref<string>('');
const password = ref<string>('');
const confirmPassword = ref<string>('');
const loading = ref<boolean>(false);

async function handleSignup(): Promise<void> {
  loading.value = true;

  // Validation
  if (!name.value || !email.value || !password.value) {
    $q.notify({ type: 'warning', message: 'Please fill in all fields.' });
    loading.value = false;
    return;
  }

  if (password.value !== confirmPassword.value) {
    $q.notify({ type: 'warning', message: 'Passwords do not match.' });
    loading.value = false;
    return;
  }

  if (password.value.length < 6) {
    $q.notify({ type: 'warning', message: 'Password must be at least 6 characters.' });
    loading.value = false;
    return;
  }

  try {
    // Sign up with Supabase Auth
    const { data, error } = await supabase.auth.signUp({
      email: email.value,
      password: password.value,
      options: {
        data: {
          full_name: name.value,
        },
      },
    });

    if (error) throw error;

    // Check if email confirmation is required
    if (data.user && !data.session) {
      $q.notify({
        type: 'info',
        message: 'Please check your email to confirm your account.',
        timeout: 5000,
        position: 'top',
      });
      // Redirect to login after a delay
      setTimeout(() => {
        void router.push('/login');
      }, 2000);
    } else {
      // Auto-login successful
      $q.notify({
        type: 'positive',
        message: 'Account created successfully!',
        position: 'top',
      });
      void router.push('/dashboard');
    }
  } catch (error) {
    console.error('Signup error:', error);
    const errorMessage = error instanceof Error ? error.message : 'Failed to create account';
    $q.notify({
      type: 'negative',
      message: errorMessage,
      position: 'top',
    });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.my_card {
  border-radius: 12px;
}
</style>
