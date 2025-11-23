<template>
  <q-page class="bg-grey-1">
    <!-- Header -->
    <div class="q-pa-md bg-white">
      <div class="text-h5 text-weight-bold">Settings</div>
    </div>

    <!-- Profile & Team -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">PROFILE & TEAM</div>
      <q-card flat class="rounded-borders">
        <q-item clickable>
          <q-item-section avatar>
            <q-avatar color="deep-orange" text-color="white" icon="person" />
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium">{{ userName }}</q-item-label>
            <q-item-label caption>{{ userEmail }}</q-item-label>
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item clickable>
          <q-item-section avatar>
            <q-icon name="edit" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Edit Profile</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-item clickable>
          <q-item-section avatar>
            <q-icon name="groups" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Manage Team</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>
      </q-card>
    </div>

    <!-- Notifications -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">NOTIFICATIONS</div>
      <q-card flat class="rounded-borders">
        <q-item>
          <q-item-section avatar>
            <q-icon name="notifications" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Expense Updates</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.expenses" color="deep-orange" />
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section avatar>
            <q-icon name="event" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Trip Reminders</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.trips" color="deep-orange" />
          </q-item-section>
        </q-item>

        <q-item>
          <q-item-section avatar>
            <q-icon name="payment" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Settlement Alerts</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.settlements" color="deep-orange" />
          </q-item-section>
        </q-item>
      </q-card>
    </div>

    <!-- App Preferences -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">APP PREFERENCES</div>
      <q-card flat class="rounded-borders">
        <q-item>
          <q-item-section avatar>
            <q-icon name="dark_mode" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Dark Mode</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="darkMode" color="deep-orange" />
          </q-item-section>
        </q-item>

        <q-item clickable>
          <q-item-section avatar>
            <q-icon name="attach_money" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Currency</q-item-label>
            <q-item-label caption>{{ defaultCurrency }}</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>
      </q-card>
    </div>

    <!-- Legal & About -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">LEGAL & ABOUT</div>
      <q-card flat class="rounded-borders">
        <q-item clickable>
          <q-item-section avatar>
            <q-icon name="description" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Terms of Service</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-item clickable>
          <q-item-section avatar>
            <q-icon name="privacy_tip" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Privacy Policy</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-item clickable>
          <q-item-section avatar>
            <q-icon name="info" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>App Version</q-item-label>
            <q-item-label caption>1.0.0</q-item-label>
          </q-item-section>
        </q-item>
      </q-card>
    </div>

    <!-- Logout Button -->
    <div class="q-pa-md q-pb-xl">
      <q-btn
        unelevated
        rounded
        color="negative"
        text-color="white"
        label="Logout"
        icon="logout"
        class="full-width"
        @click="handleLogout"
      />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';

const router = useRouter();
const $q = useQuasar();

// State
const userName = ref('Team Gala Member');
const userEmail = ref('user@example.com');
const darkMode = ref(false);
const defaultCurrency = ref('PHP - Philippine Peso');
const notifications = ref({
  expenses: true,
  trips: false,
  settlements: true
});

// Methods
async function handleLogout() {
  const { error } = await supabase.auth.signOut();

  if (error) {
    $q.notify({ type: 'negative', message: error.message });
  } else {
    void router.push('/login');
  }
}

async function fetchUserProfile() {
  const { data: { user } } = await supabase.auth.getUser();

  if (user) {
    userEmail.value = user.email ?? 'user@example.com';
    userName.value = user.email?.split('@')[0] ?? 'User';
  }
}

// Lifecycle
onMounted(() => {
  void fetchUserProfile();
});
</script>

<style scoped>
.rounded-borders {
  border-radius: 12px;
}
</style>
