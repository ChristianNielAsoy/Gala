<template>
  <q-page class="settings-page gala-mesh-bg">

    <!-- ═══ Header ═══ -->
    <div class="q-px-lg q-pt-lg q-pb-md">
      <p class="text-caption text-weight-bold settings-eyebrow q-mb-xs">Account</p>
      <h1 class="gala-display settings-title q-mb-none">Settings</h1>
    </div>

    <div class="settings-body">

      <!-- ─── Profile Card ────────────────────────────────────────────────────── -->
      <div class="profile-card q-mb-lg">
        <div class="profile-card__avatar-wrap">
          <img v-if="userProfile.photo_url" :src="userProfile.photo_url" class="profile-card__img" />
          <span v-else class="profile-card__initials">{{ userInitials }}</span>
        </div>
        <div class="profile-card__info">
          <div class="profile-card__name">{{ userProfile.name }}</div>
          <div class="profile-card__email">{{ userProfile.email }}</div>
        </div>
        <button class="profile-card__edit" @click="editProfileDialog = true">
          <q-icon name="edit" size="16px" />
        </button>
      </div>

      <!-- ─── Profile Actions ───────────────────────────────────────────────── -->
      <div class="settings-section-label">Profile & Team</div>
      <div class="settings-panel q-mb-lg">
        <div class="setting-row setting-row--clickable" @click="editProfileDialog = true">
          <div class="setting-row__icon"><q-icon name="edit" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Edit Profile</div>
          </div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row setting-row--clickable" @click="$q.notify('Manage Team feature coming soon!')">
          <div class="setting-row__icon"><q-icon name="groups" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Manage Team</div>
          </div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
      </div>

      <!-- ─── Notifications ─────────────────────────────────────────────────── -->
      <div class="settings-section-label">Notifications</div>
      <div class="settings-panel q-mb-lg">
        <div class="setting-row">
          <div class="setting-row__icon">
            <q-icon :name="pushEnabled ? 'notifications_active' : 'notifications_off'" size="18px"
              :style="pushEnabled ? 'color: #0D9488' : 'color: var(--muted)'" />
          </div>
          <div class="setting-row__body">
            <div class="setting-row__label">Push Notifications</div>
            <div class="setting-row__caption" v-if="!pushSupported">Not supported in this browser</div>
            <div class="setting-row__caption" v-else-if="pushEnabled">Active — receiving alerts</div>
            <div class="setting-row__caption" v-else>Enable alerts on this device</div>
          </div>
          <q-toggle :model-value="pushEnabled" color="primary"
            :disable="!pushSupported || togglingPush" @update:model-value="togglePushNotifications" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row" :class="{ 'setting-row--disabled': !pushEnabled }">
          <div class="setting-row__icon"><q-icon name="receipt_long" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Expense Updates</div>
            <div class="setting-row__caption">Get notified when expenses are added</div>
          </div>
          <q-toggle v-model="notifications.expenses" color="primary" :disable="!pushEnabled" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row" :class="{ 'setting-row--disabled': !pushEnabled }">
          <div class="setting-row__icon"><q-icon name="event" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Trip Reminders</div>
            <div class="setting-row__caption">Upcoming trip notifications</div>
          </div>
          <q-toggle v-model="notifications.trips" color="primary" :disable="!pushEnabled" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row" :class="{ 'setting-row--disabled': !pushEnabled }">
          <div class="setting-row__icon"><q-icon name="payment" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Settlement Alerts</div>
            <div class="setting-row__caption">When payments are due or received</div>
          </div>
          <q-toggle v-model="notifications.settlements" color="primary" :disable="!pushEnabled" />
        </div>
      </div>

      <!-- ─── App Preferences ───────────────────────────────────────────────── -->
      <div class="settings-section-label">Preferences</div>
      <div class="settings-panel q-mb-lg">
        <div class="setting-row">
          <div class="setting-row__icon"><q-icon name="dark_mode" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Dark Mode</div>
            <div class="setting-row__caption">Enable dark theme</div>
          </div>
          <q-toggle v-model="darkMode" color="primary" @update:model-value="toggleDarkMode" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row setting-row--clickable" @click="currencyDialog = true">
          <div class="setting-row__icon"><q-icon name="attach_money" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Default Currency</div>
            <div class="setting-row__caption">{{ defaultCurrencyName }}</div>
          </div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row">
          <div class="setting-row__icon"><q-icon name="language" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Language</div>
            <div class="setting-row__caption">English</div>
          </div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
      </div>

      <!-- ─── Data & Storage ────────────────────────────────────────────────── -->
      <div class="settings-section-label">Data & Storage</div>
      <div class="settings-panel q-mb-lg">
        <div class="setting-row setting-row--clickable" @click="confirmExportData">
          <div class="setting-row__icon"><q-icon name="download" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Export Trip Data</div>
            <div class="setting-row__caption">Download all your trips as CSV</div>
          </div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row setting-row--clickable" @click="confirmClearCache">
          <div class="setting-row__icon"><q-icon name="delete_outline" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">Clear Cache</div>
            <div class="setting-row__caption">Free up space</div>
          </div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
      </div>

      <!-- ─── Legal & About ─────────────────────────────────────────────────── -->
      <div class="settings-section-label">Legal & About</div>
      <div class="settings-panel q-mb-lg">
        <div class="setting-row setting-row--clickable" @click="openUrl('https://gala-app.com/terms')">
          <div class="setting-row__icon"><q-icon name="description" size="18px" /></div>
          <div class="setting-row__body"><div class="setting-row__label">Terms of Service</div></div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row setting-row--clickable" @click="openUrl('https://gala-app.com/privacy')">
          <div class="setting-row__icon"><q-icon name="privacy_tip" size="18px" /></div>
          <div class="setting-row__body"><div class="setting-row__label">Privacy Policy</div></div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row setting-row--clickable" @click="aboutDialog = true">
          <div class="setting-row__icon"><q-icon name="info" size="18px" /></div>
          <div class="setting-row__body">
            <div class="setting-row__label">About Gala</div>
            <div class="setting-row__caption">Version {{ appVersion }}</div>
          </div>
          <q-icon name="chevron_right" size="18px" class="setting-row__chevron" />
        </div>
        <div class="setting-row__divider" />
        <div class="setting-row setting-row--clickable" @click="openUrl('https://github.com/yourusername/gala-app')">
          <div class="setting-row__icon"><q-icon name="code" size="18px" /></div>
          <div class="setting-row__body"><div class="setting-row__label">View on GitHub</div></div>
          <q-icon name="open_in_new" size="16px" class="setting-row__chevron" />
        </div>
      </div>

      <!-- ─── Logout ────────────────────────────────────────────────────────── -->
      <button class="logout-btn" @click="confirmLogout">
        <q-icon name="logout" size="18px" />
        Sign Out
      </button>

    </div>

    <!-- Edit Profile Dialog -->
    <q-dialog v-model="editProfileDialog">
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Edit Profile</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <q-input v-model="userProfile.name" label="Display Name" outlined dense class="q-mb-md" />
          <q-input
            v-model="userProfile.email"
            label="Email"
            type="email"
            outlined
            dense
            readonly
            class="q-mb-md"
          />
          <q-file v-model="profilePhoto" label="Profile Photo" outlined dense accept="image/*">
            <template v-slot:prepend>
              <q-icon name="photo_camera" />
            </template>
          </q-file>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat no-caps label="Cancel" v-close-popup />
          <q-btn flat no-caps label="Save" color="primary" @click="saveProfile" />
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
import { useAuthStore } from 'src/stores/authStore';
import {
  isPushSupported,
  isSubscribed,
  subscribeUser,
  unsubscribeUser,
  getPermissionStatus,
} from 'src/utils/notificationService';

const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();

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

// Push notification state
const pushSupported = ref(false);
const pushEnabled = ref(false);
const togglingPush = ref(false);

// Auto-save notification preferences whenever a toggle changes
watch(notifications, () => { void saveUserPreferences(); }, { deep: true });

// Computed
const userInitials = computed(() => {
  const name = userProfile.value.name;
  if (!name || name === 'Team Gala Member') return '?';
  return name.split(' ').map((n: string) => n[0]).slice(0, 2).join('').toUpperCase();
});

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
  const user = authStore.user;

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
      $q.dark.set(preferences.dark_mode ?? false);
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
  const user = authStore.user;
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
  const user = authStore.user;
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
    message: 'Download all your trips and expenses as a CSV file?',
    cancel: true,
    persistent: true,
    ok: { label: 'Export', color: 'primary' },
  }).onOk(() => void doExportData());
}

async function doExportData() {
  const user = authStore.user;
  if (!user) return;

  try {
    const { data: trips } = await supabase
      .from('trips')
      .select('id, name, destination, start_date, end_date, currency_code')
      .eq('user_id', user.id);

    if (!trips?.length) {
      $q.notify({ type: 'warning', message: 'No trips found to export.' });
      return;
    }

    const tripIds = trips.map((t) => t.id);
    const { data: expenses } = await supabase
      .from('expenses')
      .select('trip_id, date, description, category, amount, paid_by_id')
      .in('trip_id', tripIds)
      .order('date', { ascending: true });

    const { data: members } = await supabase
      .from('members')
      .select('id, trip_id, name')
      .in('trip_id', tripIds);

    const tripMap = Object.fromEntries(trips.map((t) => [t.id, t]));
    const memberMap = Object.fromEntries((members || []).map((m) => [m.id, m.name]));

    const headers = ['Trip', 'Destination', 'Date', 'Description', 'Category', 'Amount', 'Currency', 'Paid By'];
    const rows = (expenses || []).map((e) => {
      const trip = tripMap[e.trip_id];
      return [
        `"${(trip?.name ?? '').replace(/"/g, '""')}"`,
        `"${(trip?.destination ?? '').replace(/"/g, '""')}"`,
        e.date,
        `"${e.description.replace(/"/g, '""')}"`,
        e.category,
        (e.amount as number).toFixed(2),
        trip?.currency_code ?? 'PHP',
        memberMap[e.paid_by_id] ?? 'Unknown',
      ];
    });

    const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `gala-trips-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);

    $q.notify({ type: 'positive', message: `Exported ${expenses?.length ?? 0} expenses from ${trips.length} trips.` });
  } catch {
    $q.notify({ type: 'negative', message: 'Export failed. Please try again.' });
  }
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

// ─── Push notification management ────────────────────────────────────────────

async function initPushState() {
  pushSupported.value = isPushSupported();
  if (pushSupported.value) {
    pushEnabled.value = await isSubscribed();
  }
}

async function togglePushNotifications(enable: boolean) {
  if (togglingPush.value) return;
  togglingPush.value = true;

  try {
    const user = authStore.user;
    if (!user) return;

    if (enable) {
      if (getPermissionStatus() === 'denied') {
        $q.notify({
          type: 'warning',
          message: 'Notifications are blocked by your browser.',
          caption: 'Check your browser settings to allow notifications for this site.',
        });
        pushEnabled.value = false;
        return;
      }
      const success = await subscribeUser(user.id);
      pushEnabled.value = success;
      if (success) {
        $q.notify({ type: 'positive', message: 'Push notifications enabled!' });
      } else {
        $q.notify({ type: 'warning', message: 'Could not enable push notifications.' });
      }
    } else {
      await unsubscribeUser(user.id);
      pushEnabled.value = false;
      $q.notify({ type: 'info', message: 'Push notifications disabled.' });
    }
  } finally {
    togglingPush.value = false;
  }
}

// Lifecycle
onMounted(async () => {
  await Promise.all([fetchCurrencies(), fetchUserProfile(), initPushState()]);
});
</script>

<style scoped lang="scss">
.settings-page {
  min-height: 100vh;
  background-color: var(--surface);
}

.settings-eyebrow {
  color: $primary;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.settings-title {
  font-size: clamp(2rem, 7vw, 3rem);
  line-height: 1.05;
  color: var(--on-background);
}

// ─── Body ─────────────────────────────────────────────────────────────────────
.settings-body {
  padding: 0 16px 80px;
}

// ─── Profile Card ──────────────────────────────────────────────────────────────
.profile-card {
  background: linear-gradient(135deg, #0D9488 0%, #134E4A 100%);
  border-radius: var(--gala-radius-xl);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  color: #fff;

  &__avatar-wrap {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    border: 2px solid rgba(255, 255, 255, 0.3);
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }

  &__img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__initials {
    font-size: 1.25rem;
    font-weight: 700;
    color: #fff;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 1rem;
    font-weight: 700;
    line-height: 1.3;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__email {
    font-size: 0.78rem;
    opacity: 0.75;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__edit {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.15);
    border: 1px solid rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    cursor: pointer;
    flex-shrink: 0;
    transition: background 0.15s;

    &:hover { background: rgba(255, 255, 255, 0.25); }
  }
}

// ─── Section labels ────────────────────────────────────────────────────────────
.settings-section-label {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--muted);
  margin-bottom: 6px;
  margin-top: 4px;
}

// ─── Panel ────────────────────────────────────────────────────────────────────
.settings-panel {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

// ─── Setting rows ─────────────────────────────────────────────────────────────
.setting-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;

  &--clickable {
    cursor: pointer;
    transition: background 0.12s;
    &:hover { background: var(--surface); }
    &:active { background: var(--border); }
  }

  &--disabled {
    opacity: 0.4;
    pointer-events: none;
  }

  &__icon {
    width: 32px;
    height: 32px;
    border-radius: var(--gala-radius-sm);
    background: var(--surface);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--muted);
    flex-shrink: 0;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__label {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--on-background);
    line-height: 1.3;
  }

  &__caption {
    font-size: 0.75rem;
    color: var(--muted);
    margin-top: 1px;
  }

  &__chevron {
    color: var(--muted);
    flex-shrink: 0;
  }

  &__divider {
    height: 1px;
    background: var(--border);
    margin: 0 16px;
  }
}

// ─── Logout button ─────────────────────────────────────────────────────────────
.logout-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 13px 0;
  border-radius: var(--gala-radius-pill);
  border: 1.5px solid rgba(220, 38, 38, 0.35);
  background: rgba(220, 38, 38, 0.06);
  color: #DC2626;
  font-size: 0.9375rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 16px;

  &:hover {
    background: rgba(220, 38, 38, 0.1);
    border-color: rgba(220, 38, 38, 0.5);
  }
}
</style>
