<template>
  <div class="auth-layout">

    <!-- ═══ Hero panel — desktop only ═══ -->
    <div class="auth-hero gt-xs">
      <div class="auth-hero__inner">
        <div class="auth-hero__brand">
          <q-icon name="flight_takeoff" size="28px" color="white" />
          <span class="auth-hero__wordmark">Gala</span>
        </div>
        <p class="auth-hero__tagline">
          Plan trips. Split expenses.<br>Make memories with your barkada.
        </p>
        <div class="auth-hero__chips">
          <div v-for="f in features" :key="f" class="auth-hero__chip">
            <q-icon name="check_circle" size="14px" color="white" />
            <span>{{ f }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ Form panel ═══ -->
    <div class="auth-form-panel">

      <!-- Mobile gradient brand strip -->
      <div class="auth-mobile-header lt-sm">
        <q-icon name="flight_takeoff" size="20px" color="white" />
        <span class="auth-mobile-header__name">Gala</span>
      </div>

      <div class="auth-form-inner">
        <div class="q-mb-lg">
          <h1 class="auth-title">Create account</h1>
          <p class="auth-subtitle">Start planning with your barkada</p>
        </div>

        <q-form @submit.prevent="handleSignup">
          <div class="auth-name-row">
            <q-input
              v-model="firstName"
              label="First Name"
              type="text"
              outlined dense
              class="auth-input"
              lazy-rules
              :rules="[(val: string) => !!val || 'Required']"
            >
              <template #prepend><q-icon name="person" color="primary" /></template>
            </q-input>
            <q-input
              v-model="lastName"
              label="Last Name"
              type="text"
              outlined dense
              class="auth-input"
            />
          </div>

          <div class="auth-name-row q-mb-md">
            <q-input
              v-model="nickname"
              label="Nickname (optional)"
              type="text"
              outlined dense
              class="auth-input"
            >
              <template #prepend><q-icon name="tag" color="primary" /></template>
            </q-input>
            <q-input
              v-model="birthday"
              label="Birthday (optional)"
              type="date"
              outlined dense
              class="auth-input"
              stack-label
            >
              <template #prepend><q-icon name="cake" color="primary" /></template>
            </q-input>
          </div>

          <q-input
            v-model="email"
            label="Email"
            type="email"
            outlined dense
            class="auth-input q-mb-md"
            lazy-rules
            :rules="[(val: string) => !!val || 'Email is required']"
          >
            <template #prepend><q-icon name="mail" color="primary" /></template>
          </q-input>

          <q-input
            v-model="password"
            label="Password"
            type="password"
            outlined dense
            class="auth-input q-mb-md"
            lazy-rules
            :rules="[(val: string) => val.length >= 6 || 'Password must be at least 6 characters']"
          >
            <template #prepend><q-icon name="lock" color="primary" /></template>
          </q-input>

          <q-input
            v-model="confirmPassword"
            label="Confirm Password"
            type="password"
            outlined dense
            class="auth-input q-mb-lg"
            lazy-rules
            :rules="[(val: string) => val === password || 'Passwords do not match']"
          >
            <template #prepend><q-icon name="lock_reset" color="primary" /></template>
          </q-input>

          <q-btn
            type="submit" color="primary" unelevated no-caps
            label="Create Account" class="full-width auth-submit"
            :loading="loading"
          />
        </q-form>

        <div class="text-center q-mt-lg">
          <span class="auth-switch-text">Already have an account? </span>
          <q-btn flat dense no-caps color="primary" label="Sign in" to="/login" />
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

const firstName = ref<string>('');
const lastName = ref<string>('');
const nickname = ref<string>('');
const birthday = ref<string>('');
const email = ref<string>('');
const password = ref<string>('');
const confirmPassword = ref<string>('');
const loading = ref<boolean>(false);

const features = [
  'Split expenses fairly with your group',
  'Track every peso, every trip',
  'Settle up in seconds',
];

async function handleSignup(): Promise<void> {
  loading.value = true;

  if (!firstName.value || !email.value || !password.value) {
    $q.notify({ type: 'warning', message: 'Please fill in all required fields.' });
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
    const fullName = [firstName.value, lastName.value].filter(Boolean).join(' ');
    const { data, error } = await supabase.auth.signUp({
      email: email.value,
      password: password.value,
      options: {
        data: { full_name: fullName },
      },
    });

    if (error) throw error;

    if (data.user) {
      await supabase.from('profiles').insert({
        id: data.user.id,
        first_name: firstName.value.trim(),
        last_name: lastName.value.trim() || null,
        nickname: nickname.value.trim() || null,
        birthday: birthday.value || null,
      });
    }

    if (data.user && !data.session) {
      $q.notify({
        type: 'info',
        message: 'Please check your email to confirm your account.',
        timeout: 5000,
        position: 'top',
      });
      setTimeout(() => {
        void router.push('/login');
      }, 2000);
    } else {
      $q.notify({
        type: 'positive',
        message: 'Account created successfully!',
        position: 'top',
      });
      void router.push('/dashboard');
    }
  } catch (error) {
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

<style scoped lang="scss">
.auth-layout {
  min-height: 100vh;
  display: flex;
  background: var(--background);
}

// ─── Hero panel ────────────────────────────────────────────────────────────────
.auth-hero {
  flex: 0 0 44%;
  background: linear-gradient(145deg, #0D9488 0%, #134E4A 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 44px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    width: 400px;
    height: 400px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
    top: -140px;
    right: -140px;
  }

  &::after {
    content: '';
    position: absolute;
    width: 280px;
    height: 280px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.04);
    bottom: -80px;
    left: -80px;
  }
}

.auth-hero__inner {
  position: relative;
  z-index: 1;
  max-width: 300px;
}

.auth-hero__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}

.auth-hero__wordmark {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2rem;
  color: #fff;
  line-height: 1;
}

.auth-hero__tagline {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 1.5rem;
  color: rgba(255, 255, 255, 0.92);
  line-height: 1.5;
  margin: 0 0 28px;
}

.auth-hero__chips {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.auth-hero__chip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.85);
  font-weight: 500;
}

// ─── Form panel ────────────────────────────────────────────────────────────────
.auth-form-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--background);
}

.auth-mobile-header {
  width: 100%;
  background: linear-gradient(135deg, #0D9488 0%, #134E4A 100%);
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 10px;

  &__name {
    font-family: 'Instrument Serif', Georgia, serif;
    font-size: 1.5rem;
    color: #fff;
    line-height: 1;
  }
}

.auth-form-inner {
  width: 100%;
  max-width: 400px;
  padding: 40px 32px;
}

// ─── Typography ────────────────────────────────────────────────────────────────
.auth-title {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2rem;
  line-height: 1.1;
  color: var(--on-background);
  margin: 0 0 6px;
}

.auth-subtitle {
  font-size: 0.9375rem;
  color: var(--muted);
  margin: 0;
}

.auth-switch-text {
  font-size: 0.875rem;
  color: var(--muted);
}

// ─── Inputs ────────────────────────────────────────────────────────────────────
.auth-name-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}

.auth-input {
  :deep(.q-field__control) {
    border-radius: var(--gala-radius-md);
  }
}

@media (max-width: 399px) {
  .auth-name-row {
    grid-template-columns: 1fr;
  }
}

// ─── Submit ────────────────────────────────────────────────────────────────────
.auth-submit {
  height: 48px;
  font-size: 1rem;
  font-weight: 600;
  border-radius: var(--gala-radius-pill) !important;
}

@media (max-width: 599px) {
  .auth-form-inner {
    padding: 28px 24px 40px;
  }
}
</style>
