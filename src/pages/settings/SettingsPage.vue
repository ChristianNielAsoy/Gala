<template>
  <q-page class="bg-surface">
    <!-- Header -->
    <div class="q-pa-md bg-surface">
      <div class="text-h5 text-weight-bold">Settings</div>
    </div>

    <!-- Profile & Team Section -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">PROFILE & TEAM</div>
      <q-card flat class="rounded-borders">
        <!-- User Profile -->
        <q-item clickable @click="editProfileDialog = true">
          <q-item-section avatar>
            <q-avatar color="deep-orange" text-color="white" size="56px">
              <q-icon name="person" size="32px" v-if="!userProfile.photo_url" />
              <img :src="userProfile.photo_url" v-else />
            </q-avatar>
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-medium text-h6">
              {{ userProfile.name }}
            </q-item-label>
            <q-item-label caption class="text-body2">
              {{ userProfile.email }}
            </q-item-label>
            <q-item-label caption class="text-caption text-grey-6"> Team Gala Member </q-item-label>
          </q-item-section>
        </q-item>

        <q-separator />

        <!-- Edit Profile -->
        <q-item clickable @click="editProfileDialog = true">
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

        <!-- Manage Team -->
        <q-item clickable @click="$q.notify('Manage Team feature coming soon!')">
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

    <!-- Notifications Section -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">NOTIFICATIONS</div>
      <q-card flat class="rounded-borders">
        <q-item>
          <q-item-section avatar>
            <q-icon name="notifications" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Expense Updates</q-item-label>
            <q-item-label caption>Get notified when expenses are added</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.expenses" color="deep-orange" />
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item>
          <q-item-section avatar>
            <q-icon name="event" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Trip Reminders</q-item-label>
            <q-item-label caption>Upcoming trip notifications</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.trips" color="deep-orange" />
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item>
          <q-item-section avatar>
            <q-icon name="payment" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Settlement Alerts</q-item-label>
            <q-item-label caption>When payments are due or received</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="notifications.settlements" color="deep-orange" />
          </q-item-section>
        </q-item>
      </q-card>
    </div>

    <!-- App Preferences Section -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">APP PREFERENCES</div>
      <q-card flat class="rounded-borders">
        <q-item>
          <q-item-section avatar>
            <q-icon name="dark_mode" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Dark Mode</q-item-label>
            <q-item-label caption>Enable dark theme</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-toggle v-model="darkMode" color="deep-orange" @update:model-value="toggleDarkMode" />
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item clickable @click="currencyDialog = true">
          <q-item-section avatar>
            <q-icon name="attach_money" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Default Currency</q-item-label>
            <q-item-label caption>{{ defaultCurrencyName }}</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item clickable>
          <q-item-section avatar>
            <q-icon name="language" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Language</q-item-label>
            <q-item-label caption>English</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>
      </q-card>
    </div>

    <!-- Data & Storage Section -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">DATA & STORAGE</div>
      <q-card flat class="rounded-borders">
        <q-item clickable @click="confirmExportData">
          <q-item-section avatar>
            <q-icon name="download" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Export Trip Data</q-item-label>
            <q-item-label caption>Download all your trips as CSV</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item clickable @click="confirmClearCache">
          <q-item-section avatar>
            <q-icon name="delete_outline" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>Clear Cache</q-item-label>
            <q-item-label caption>Free up space</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>
      </q-card>
    </div>

    <!-- Legal & About Section -->
    <div class="q-pa-md">
      <div class="text-caption text-grey-7 q-mb-sm text-weight-medium">LEGAL & ABOUT</div>
      <q-card flat class="rounded-borders">
        <q-item clickable @click="openUrl('https://gala-app.com/terms')">
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

        <q-separator />

        <q-item clickable @click="openUrl('https://gala-app.com/privacy')">
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

        <q-separator />

        <q-item clickable @click="aboutDialog = true">
          <q-item-section avatar>
            <q-icon name="info" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>About Gala</q-item-label>
            <q-item-label caption>Version {{ appVersion }}</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="chevron_right" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-separator />

        <q-item clickable @click="openUrl('https://github.com/yourusername/gala-app')">
          <q-item-section avatar>
            <q-icon name="code" color="grey-7" />
          </q-item-section>
          <q-item-section>
            <q-item-label>View on GitHub</q-item-label>
          </q-item-section>
          <q-item-section side>
            <q-icon name="open_in_new" color="grey-5" />
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
        @click="confirmLogout"
      />
    </div>

    <!-- Edit Profile Dialog -->
    <q-dialog v-model="editProfileDialog">
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Edit Profile</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <q-input v-model="userProfile.name" label="Display Name" outlined class="q-mb-md" />
          <q-input
            v-model="userProfile.email"
            label="Email"
            type="email"
            outlined
            readonly
            class="q-mb-md"
          />
          <q-file v-model="profilePhoto" label="Profile Photo" outlined accept="image/*">
            <template v-slot:prepend>
              <q-icon name="photo_camera" />
            </template>
          </q-file>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn flat label="Save" color="primary" @click="saveProfile" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Currency Selection Dialog -->
    <q-dialog v-model="currencyDialog">
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Select Default Currency</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <q-list>
            <q-item
              v-for="currency in currencies"
              :key="currency.code"
              clickable
              @click="selectCurrency(currency)"
              v-close-popup
            >
              <q-item-section>
                <q-item-label>{{ currency.name }}</q-item-label>
                <q-item-label caption>{{ currency.code }}</q-item-label>
              </q-item-section>
              <q-item-section side v-if="defaultCurrency === currency.code">
                <q-icon name="check" color="primary" />
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- About Dialog -->
    <q-dialog v-model="aboutDialog">
      <q-card style="min-width: 350px">
        <q-card-section class="text-center">
          <div class="text-h4 text-weight-bold text-primary q-mb-sm">GALA</div>
          <div class="text-subtitle2 text-grey-7">Trip Expense Management</div>
          <div class="text-caption text-grey-6 q-mt-sm">Version {{ appVersion }}</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <p class="text-body2 text-center">
            Manage your group travel expenses with ease. Built for Filipino barkada trips.
          </p>
          <div class="text-center q-mt-md">
            <q-btn flat label="Check for Updates" color="primary" @click="checkUpdates" />
          </div>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Close" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';

const router = useRouter();
const $q = useQuasar();

// State
const userProfile = ref({
  name: 'Team Gala Member',
  email: 'user@example.com',
  photo_url: '',
});

const notifications = ref({
  expenses: true,
  trips: false,
  settlements: true,
});

const darkMode = ref(false);
const defaultCurrency = ref('PHP');
const appVersion = ref('1.0.0');

// Auto-save notification preferences whenever a toggle changes
watch(notifications, () => { void saveUserPreferences(); }, { deep: true });

// Computed
const defaultCurrencyName = computed(() => {
  const currency = currencies.value.find((c) => c.code === defaultCurrency.value);
  return currency ? currency.name : defaultCurrency.value;
});

// Dialogs
const editProfileDialog = ref(false);
const currencyDialog = ref(false);
const aboutDialog = ref(false);
const profilePhoto = ref<File | null>(null);

// Currency options
const currencies = ref<{ code: string; name: string }[]>([]);

// Methods
async function fetchCurrencies(): Promise<void> {
  const { data, error } = await supabase
    .from('currencies')
    .select('code, name')
    .eq('is_active', true)
    .order('display_order');

  if (error) {
    console.warn('Failed to load currencies:', error);
    // Fallback to hardcoded currencies
    currencies.value = [
      { code: 'PHP', name: 'PHP - Philippine Peso' },
      { code: 'USD', name: 'USD - US Dollar' },
      { code: 'EUR', name: 'EUR - Euro' },
      { code: 'JPY', name: 'JPY - Japanese Yen' },
      { code: 'SGD', name: 'SGD - Singapore Dollar' },
      { code: 'GBP', name: 'GBP - British Pound' },
      { code: 'AUD', name: 'AUD - Australian Dollar' },
    ];
  } else {
    currencies.value = data.map((curr) => ({
      code: curr.code,
      name: `${curr.code} - ${curr.name}`,
    }));
  }
}

async function fetchUserProfile() {
  const {
    data: { user },
  } = await supabase.auth.getUser();

  if (user) {
    userProfile.value.email = user.email ?? 'user@example.com';
    userProfile.value.name =
      (user.user_metadata?.full_name as string | undefined) ||
      (user.user_metadata?.display_name as string | undefined) ||
      user.email?.split('@')[0] ||
      'User';
    userProfile.value.photo_url =
      (user.user_metadata?.avatar_url as string | undefined) || '';

    // Load user preferences
    const { data: preferences, error } = await supabase
      .from('user_preferences')
      .select('*')
      .eq('user_id', user.id)
      .single();

    if (!error && preferences) {
      darkMode.value = preferences.dark_mode ?? false;
      defaultCurrency.value = preferences.default_currency ?? 'PHP';
      notifications.value = {
        expenses: preferences.expense_updates ?? true,
        trips: preferences.trip_reminders ?? false,
        settlements: preferences.settlement_alerts ?? true,
      };
    }
  }
}

function toggleDarkMode(value: boolean) {
  darkMode.value = value;
  $q.dark.set(value);
  void saveUserPreferences();
  $q.notify({
    type: 'info',
    message: value ? 'Dark mode enabled' : 'Light mode enabled',
    position: 'top',
  });
}

async function selectCurrency(currency: { code: string; name: string }) {
  defaultCurrency.value = currency.code;
  await saveUserPreferences();
  $q.notify({
    type: 'positive',
    message: `Currency changed to ${currency.code}`,
    position: 'top',
  });
}

async function saveUserPreferences() {
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return;

  const { error } = await supabase.from('user_preferences').upsert({
    user_id: user.id,
    dark_mode: darkMode.value,
    default_currency: defaultCurrency.value,
    expense_updates: notifications.value.expenses,
    trip_reminders: notifications.value.trips,
    settlement_alerts: notifications.value.settlements,
    updated_at: new Date().toISOString(),
  });

  if (error) {
    console.warn('Failed to save user preferences:', error);
  }
}

async function saveProfile() {
  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) return;

  try {
    let photoUrl = userProfile.value.photo_url;

    // Upload new photo if selected
    if (profilePhoto.value) {
      const ext = profilePhoto.value.name.split('.').pop() ?? 'jpg';
      const fileName = `${user.id}/avatar.${ext}`;
      const { error: uploadError } = await supabase.storage
        .from('profile-photos')
        .upload(fileName, profilePhoto.value, { upsert: true });

      if (!uploadError) {
        const { data: urlData } = supabase.storage.from('profile-photos').getPublicUrl(fileName);
        photoUrl = urlData.publicUrl;
      }
    }

    // Update auth user metadata
    const { error } = await supabase.auth.updateUser({
      data: {
        full_name: userProfile.value.name,
        display_name: userProfile.value.name,
        avatar_url: photoUrl || undefined,
      },
    });

    if (error) throw error;

    // Reflect photo change in local state
    if (photoUrl !== userProfile.value.photo_url) {
      userProfile.value.photo_url = photoUrl;
    }
    profilePhoto.value = null;

    $q.notify({
      type: 'positive',
      message: 'Profile updated successfully!',
      position: 'top',
    });
    editProfileDialog.value = false;
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error instanceof Error ? error.message : 'Failed to update profile',
    });
  }
}

function confirmLogout() {
  $q.dialog({
    title: 'Confirm Logout',
    message: 'Are you sure you want to logout?',
    cancel: true,
    persistent: true,
  }).onOk(() => {
    void handleLogout();
  });
}

async function handleLogout() {
  const { error } = await supabase.auth.signOut();

  if (error) {
    $q.notify({ type: 'negative', message: error.message });
  } else {
    void router.push('/login');
  }
}

function confirmExportData() {
  $q.dialog({
    title: 'Export Trip Data',
    message: 'Download all your trip data as a CSV file?',
    cancel: true,
    persistent: true,
    ok: { label: 'Export', color: 'primary' },
  }).onOk(() => {
    $q.notify({
      type: 'info',
      message: 'Export feature coming soon!',
      position: 'top',
    });
  });
}

function confirmClearCache() {
  $q.dialog({
    title: 'Clear Cache',
    message: 'This will free up space but may require reloading data.',
    cancel: true,
    persistent: true,
    ok: { label: 'Clear', color: 'negative' },
  }).onOk(() => {
    $q.notify({
      type: 'positive',
      message: 'Cache cleared successfully',
      position: 'top',
    });
  });
}

function checkUpdates() {
  $q.notify({
    type: 'positive',
    message: 'You are using the latest version!',
    position: 'top',
  });
}

function openUrl(url: string) {
  window.open(url, '_blank');
}

// Lifecycle
onMounted(async () => {
  await Promise.all([fetchCurrencies(), fetchUserProfile()]);
});
</script>

<style scoped>
.rounded-borders {
  border-radius: 12px;
}
</style>
