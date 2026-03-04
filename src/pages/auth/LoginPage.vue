<template>
  <div class="auth-layout">
    <!-- Hero panel — desktop only -->
    <div class="auth-hero gt-xs">
      <div class="auth-hero__inner">
        <div class="auth-hero__brand">
          <q-icon name="flight_takeoff" size="32px" color="white" class="q-mr-sm" />
          <span class="text-h4 text-white text-weight-bold">Gala</span>
        </div>
        <p class="auth-hero__tagline text-white">
          Plan trips. Split expenses.<br />Make memories with your barkada.
        </p>
        <div class="auth-hero__features">
          <div v-for="f in features" :key="f" class="auth-hero__feature">
            <q-icon name="check_circle" color="white" size="18px" />
            <span class="text-white q-ml-xs">{{ f }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Form panel -->
    <div class="auth-form-panel">
      <!-- Mobile-only brand header -->
      <div class="auth-mobile-brand lt-sm q-mb-xl text-center">
        <q-icon name="flight_takeoff" size="24px" color="primary" />
        <span class="text-h5 text-primary text-weight-bold q-ml-xs">Gala</span>
      </div>

      <div class="auth-form-inner">
        <div class="q-mb-xl">
          <div class="text-h5 text-weight-bold" style="color: #1e293b">Welcome back</div>
          <div class="text-body2 text-grey-6 q-mt-xs">Sign in to your Gala account</div>
        </div>

        <q-form @submit.prevent="handleLogin">
          <q-input
            v-model="email"
            label="Email"
            type="email"
            outlined
            dense
            class="q-mb-md"
            lazy-rules
            :rules="[(val: string) => !!val || 'Email is required']"
          >
            <template #prepend>
              <q-icon name="mail" />
            </template>
          </q-input>

          <q-input
            v-model="password"
            label="Password"
            type="password"
            outlined
            dense
            class="q-mb-sm"
            lazy-rules
            :rules="[(val: string) => !!val || 'Password is required']"
          >
            <template #prepend>
              <q-icon name="lock" />
            </template>
          </q-input>

          <div class="text-right q-mb-lg">
            <q-btn flat dense no-caps color="primary" label="Forgot password?" size="sm" />
          </div>

          <q-btn
            type="submit"
            color="primary"
            unelevated
            size="lg"
            label="Sign In"
            class="full-width"
            :loading="loading"
          />
        </q-form>

        <div class="text-center q-mt-lg">
          <span class="text-body2 text-grey-6">New to Gala? </span>
          <q-btn flat dense no-caps color="primary" label="Sign up" to="/signup" />
        </div>
      </div>
    </div>
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

const features = [
  'Split expenses fairly with your group',
  'Track every peso, every trip',
  'Settle up in seconds',
];

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

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  display: flex;
}

.auth-hero {
  flex: 0 0 45%;
  background: linear-gradient(145deg, #0d9488 0%, #065f52 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    width: 350px;
    height: 350px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
    top: -120px;
    right: -120px;
  }

  &::after {
    content: '';
    position: absolute;
    width: 250px;
    height: 250px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.04);
    bottom: -80px;
    left: -60px;
  }
}

.auth-hero__inner {
  position: relative;
  z-index: 1;
  max-width: 320px;
}

.auth-hero__brand {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.auth-hero__tagline {
  font-size: 1.25rem;
  font-weight: 500;
  line-height: 1.6;
  opacity: 0.92;
  margin: 0 0 32px 0;
}

.auth-hero__features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.auth-hero__feature {
  display: flex;
  align-items: center;
  opacity: 0.88;
  font-size: 0.9375rem;
}

.auth-form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  background: #ffffff;
}

.auth-mobile-brand {
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-form-inner {
  width: 100%;
  max-width: 400px;
}

@media (max-width: 599px) {
  .auth-form-panel {
    align-items: flex-start;
    padding: 48px 24px 32px;
  }
}
</style>
