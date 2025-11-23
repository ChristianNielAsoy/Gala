<template>
  <q-page class="q-pa-md">
    <div class="text-h5 text-weight-bold q-mb-lg">Settings</div>

    <!-- Profile Section -->
    <q-card flat bordered class="q-mb-md">
      <q-card-section>
        <div class="text-subtitle1 text-weight-bold q-mb-md">Profile & Account</div>

        <div class="row items-center q-mb-md">
          <q-avatar size="64px" color="primary" text-color="white" class="q-mr-md">
            <q-icon name="person" size="32px" />
          </q-avatar>
          <div>
            <div class="text-body1 text-weight-medium">{{ userEmail }}</div>
            <div class="text-caption text-grey-7">Gala Member</div>
          </div>
        </div>

        <q-btn
          flat
          color="primary"
          icon="edit"
          label="Edit Profile"
          class="full-width"
          align="left"
        />
      </q-card-section>
    </q-card>

    <!-- App Preferences -->
    <q-card flat bordered class="q-mb-md">
      <q-card-section>
        <div class="text-subtitle1 text-weight-bold q-mb-md">App Preferences</div>

        <q-item tag="label" class="q-pa-none q-mb-sm">
          <q-item-section>
            <q-item-label>Dark Mode</q-item-label>
            <q-item-label caption>Enable dark theme</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="darkMode" color="primary" />
          </q-item-section>
        </q-item>

        <q-separator class="q-my-md" />

        <q-item clickable class="q-pa-none">
          <q-item-section>
            <q-item-label>Currency</q-item-label>
            <q-item-label caption>PHP - Philippine Peso</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>
      </q-card-section>
    </q-card>

    <!-- Notifications -->
    <q-card flat bordered class="q-mb-md">
      <q-card-section>
        <div class="text-subtitle1 text-weight-bold q-mb-md">Notifications</div>

        <q-item tag="label" class="q-pa-none q-mb-sm">
          <q-item-section>
            <q-item-label>Expense Updates</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.expenses" color="orange" />
          </q-item-section>
        </q-item>

        <q-item tag="label" class="q-pa-none q-mb-sm">
          <q-item-section>
            <q-item-label>Trip Reminders</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.trips" color="primary" />
          </q-item-section>
        </q-item>

        <q-item tag="label" class="q-pa-none">
          <q-item-section>
            <q-item-label>Settlement Alerts</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.settlements" color="orange" />
          </q-item-section>
        </q-item>
      </q-card-section>
    </q-card>

    <!-- Legal & About -->
    <q-card flat bordered class="q-mb-md">
      <q-card-section>
        <div class="text-subtitle1 text-weight-bold q-mb-md">Legal & About</div>

        <q-item clickable class="q-pa-none q-mb-sm">
          <q-item-section>
            <q-item-label>Terms of Service</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-item clickable class="q-pa-none q-mb-sm">
          <q-item-section>
            <q-item-label>Privacy Policy</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-item class="q-pa-none">
          <q-item-section>
            <q-item-label>App Version</q-item-label>
            <q-item-label caption>1.0.0</q-item-label>
          </q-item-section>
        </q-item>
      </q-card-section>
    </q-card>

    <!-- Logout Button -->
    <q-btn
      label="Logout"
      @click="handleLogout"
      color="negative"
      icon="logout"
      class="full-width"
      unelevated
    />
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';

const $q = useQuasar();
const router = useRouter();

const userEmail = ref('');
const darkMode = ref(false);
const notifications = ref({
  expenses: true,
  trips: false,
  settlements: true,
});

async function loadUserData(): Promise<void> {
  const { data: { user } } = await supabase.auth.getUser();
  if (user) {
    userEmail.value = user.email || 'User';
  }
}

async function handleLogout(): Promise<void> {
  const { error } = await supabase.auth.signOut();
  if (error) {
    $q.notify({ type: 'negative', message: error.message });
  } else {
    $q.notify({ type: 'positive', message: 'Logged out successfully' });
    void router.push('/login');
  }
}

onMounted(() => {
  void loadUserData();
});
</script>
