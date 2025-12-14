<template>
  <q-layout view="lHh lpr lFf">
    <q-header elevated class="bg-white text-dark shadow-2" height-hint="64">
      <q-toolbar class="q-px-md">
        <!-- Mobile Menu Button -->
        <q-btn
          v-if="!sidebarVisible && $q.screen.gt.sm"
          flat
          round
          icon="menu"
          @click="sidebarVisible = true"
          aria-label="Open Sidebar"
          class="q-mr-sm"
        />

        <!-- App Logo and Branding -->
        <q-avatar size="40px" class="q-mr-sm">
          <img src="/logo.png" alt="Gala Logo" />
        </q-avatar>
        <q-toolbar-title class="text-h6 text-weight-bold text-primary q-mr-md">
          Gala
        </q-toolbar-title>

        <!-- Page Title -->
        <div class="page-title text-subtitle1 text-grey-8 q-mr-auto">
          {{ getPageTitle() }}
        </div>

        <!-- Search Button (for larger screens) -->
        <q-btn
          v-if="$q.screen.gt.md"
          flat
          round
          icon="search"
          @click="showSearch = true"
          aria-label="Search"
          class="q-mr-sm"
        >
          <q-tooltip>Search trips and activities</q-tooltip>
        </q-btn>

        <!-- Notifications -->
        <q-btn
          flat
          round
          icon="notifications"
          @click="showNotifications = true"
          aria-label="Notifications"
          class="q-mr-sm"
        >
          <q-badge
            v-if="notificationCount > 0"
            :label="notificationCount"
            color="negative"
            floating
          />
          <q-tooltip>Notifications</q-tooltip>
        </q-btn>

        <!-- User Menu -->
        <q-btn flat round aria-label="User Menu" class="q-ml-sm">
          <q-avatar size="32px">
            <img v-if="userAvatar" :src="userAvatar" alt="User Avatar" />
            <q-icon v-else name="person" />
          </q-avatar>
          <q-menu>
            <q-list dense>
              <q-item clickable @click="navigateTo('/user-profile')">
                <q-item-section avatar>
                  <q-icon name="person" />
                </q-item-section>
                <q-item-section>Profile</q-item-section>
              </q-item>
              <q-item clickable @click="navigateTo('/settings')">
                <q-item-section avatar>
                  <q-icon name="settings" />
                </q-item-section>
                <q-item-section>Settings</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable @click="handleLogout">
                <q-item-section avatar>
                  <q-icon name="logout" />
                </q-item-section>
                <q-item-section>Logout</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer
      :model-value="sidebarVisible"
      show-if-above
      :width="250"
      :breakpoint="500"
      :overlay="!$q.screen.gt.sm"
      elevated
      class="bg-white"
    >
      <q-toolbar class="bg-primary text-white">
        <q-btn flat round icon="menu" @click="toggleSidebar" aria-label="Toggle Sidebar" />
        <q-img src="/logo.png" class="logo" />
        <q-toolbar-title class="text-weight-bold">Gala</q-toolbar-title>
      </q-toolbar>
      <div class="sidebar-list">
        <q-list padding>
          <q-item clickable v-ripple @click="navigateTo('/dashboard')">
            <q-item-section avatar>
              <q-icon name="dashboard" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Dashboard</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple @click="navigateTo('/trips')">
            <q-item-section avatar>
              <q-icon name="luggage" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Trips</q-item-label>
            </q-item-section>
          </q-item>

          <q-separator />

          <q-item clickable v-ripple @click="navigateTo('/expense-analytics')">
            <q-item-section avatar>
              <q-icon name="analytics" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Expense Analytics</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple @click="navigateTo('/documents-vault')">
            <q-item-section avatar>
              <q-icon name="description" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Documents Vault</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple @click="navigateTo('/packing-list')">
            <q-item-section avatar>
              <q-icon name="checklist" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Packing List</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple @click="navigateTo('/itinerary-templates')">
            <q-item-section avatar>
              <q-icon name="event_note" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Itinerary Templates</q-item-label>
            </q-item-section>
          </q-item>

          <q-separator />

          <q-item clickable v-ripple @click="navigateTo('/user-profile')">
            <q-item-section avatar>
              <q-icon name="person" />
            </q-item-section>
            <q-item-section>
              <q-item-label>User Profile</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple @click="navigateTo('/settings')">
            <q-item-section avatar>
              <q-icon name="settings" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Settings</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </div>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';

const $q = useQuasar();
const route = useRoute();
const router = useRouter();
const drawer = ref(false);
const sidebarVisible = ref(true);
const showSearch = ref(false);
const showNotifications = ref(false);
const notificationCount = ref(3); // Mock notification count
const userAvatar = ref<string | null>(null);

watch(
  () => route.path,
  () => {
    // Close drawer on mobile when navigating
    if (!$q.screen.gt.sm) {
      drawer.value = false;
    }
  },
  { immediate: true },
);

onMounted(async () => {
  // Fetch user avatar
  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (user?.user_metadata?.avatar_url) {
      userAvatar.value = user.user_metadata.avatar_url;
    }
  } catch (error) {
    console.error('Error fetching user:', error);
  }
});

function toggleSidebar() {
  if (!$q.screen.gt.sm) {
    // Mobile: toggle drawer open/close
    drawer.value = !drawer.value;
  } else {
    // Desktop: toggle sidebar visible/hidden
    sidebarVisible.value = !sidebarVisible.value;
  }
}

function navigateTo(path: string) {
  void router.push(path);
  // Close drawer on mobile after navigation
  if (!$q.screen.gt.sm) {
    drawer.value = false;
  }
}

function getPageTitle(): string {
  const path = route.path;
  if (path === '/dashboard') return 'Dashboard';
  if (path === '/trips') return 'My Trips';
  if (path === '/expense-analytics') return 'Expense Analytics';
  if (path === '/documents-vault') return 'Documents Vault';
  if (path === '/packing-list') return 'Packing List';
  if (path === '/itinerary-templates') return 'Itinerary Templates';
  if (path === '/user-profile') return 'User Profile';
  if (path === '/settings') return 'Settings';
  return 'Gala';
}

async function handleLogout() {
  try {
    await supabase.auth.signOut();
    await router.push('/login');
  } catch (error) {
    console.error('Error logging out:', error);
    $q.notify({
      type: 'negative',
      message: 'Error logging out',
    });
  }
}
</script>

<style scoped>
.page-title {
  font-weight: 500;
  opacity: 0.8;
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

/* Header shadow and styling */
.q-header {
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

/* Responsive adjustments */
@media (max-width: 599px) {
  .page-title {
    display: none;
  }
}

/* Notification badge positioning */
.q-btn .q-badge {
  top: 8px;
  right: 8px;
}

/* User avatar styling */
.q-avatar img {
  object-fit: cover;
}

/* Toolbar spacing */
.q-toolbar {
  min-height: 64px;
}
</style>
