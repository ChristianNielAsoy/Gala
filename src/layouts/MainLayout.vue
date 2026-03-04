<template>
  <q-layout view="lHh lpr lFf">
    <q-header class="bg-white text-dark" height-hint="64">
      <q-toolbar class="q-px-md">
        <!-- Hamburger — always visible (closes/opens sidebar) -->
        <q-btn
          flat
          round
          dense
          icon="menu"
          @click="toggleSidebar"
          aria-label="Toggle Sidebar"
          class="q-mr-sm text-grey-7"
        />

        <!-- App Brand -->
        <q-avatar size="34px" class="q-mr-xs">
          <img src="/logo.png" alt="Gala Logo" />
        </q-avatar>
        <span class="header-brand text-weight-bold text-primary q-mr-md">Gala</span>

        <!-- Page Title -->
        <span class="page-title text-body2 text-grey-6 gt-xs">{{ getPageTitle() }}</span>

        <q-space />

        <!-- User Avatar + Menu -->
        <q-btn flat round dense aria-label="User Menu" class="q-ml-sm">
          <q-avatar size="32px" color="primary" text-color="white">
            <img v-if="authStore.userAvatar" :src="authStore.userAvatar" alt="User Avatar" />
            <span v-else class="text-caption text-weight-bold">{{ authStore.userInitials }}</span>
          </q-avatar>
          <q-menu anchor="bottom right" self="top right" style="min-width: 160px">
            <q-list dense>
              <q-item clickable :to="'/user-profile'" v-close-popup>
                <q-item-section avatar>
                  <q-icon name="person" size="18px" />
                </q-item-section>
                <q-item-section>Profile</q-item-section>
              </q-item>
              <q-item clickable :to="'/settings'" v-close-popup>
                <q-item-section avatar>
                  <q-icon name="settings" size="18px" />
                </q-item-section>
                <q-item-section>Settings</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable @click="handleLogout" v-close-popup>
                <q-item-section avatar>
                  <q-icon name="logout" size="18px" color="negative" />
                </q-item-section>
                <q-item-section class="text-negative">Logout</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <q-drawer
      v-model="sidebarVisible"
      show-if-above
      :width="250"
      :breakpoint="500"
      :overlay="!$q.screen.gt.sm"
      class="bg-white"
    >
      <!-- Sidebar brand header -->
      <div class="sidebar-header">
        <q-icon name="flight_takeoff" size="20px" color="white" />
        <span class="text-subtitle1 text-white text-weight-bold q-ml-sm">Gala</span>
        <q-space />
        <q-btn
          flat round dense
          icon="chevron_left"
          color="white"
          size="sm"
          @click="sidebarVisible = false"
          aria-label="Close Sidebar"
        />
      </div>

      <!-- Navigation -->
      <div class="sidebar-list q-pt-sm">
        <q-list padding>
          <q-item clickable v-ripple :to="'/dashboard'" exact>
            <q-item-section avatar>
              <q-icon name="dashboard" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Dashboard</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple :to="'/trips'">
            <q-item-section avatar>
              <q-icon name="luggage" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Trips</q-item-label>
            </q-item-section>
          </q-item>

          <q-separator class="q-my-sm" />

          <q-item clickable v-ripple :to="'/expense-analytics'">
            <q-item-section avatar>
              <q-icon name="analytics" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Expense Analytics</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple :to="'/documents-vault'">
            <q-item-section avatar>
              <q-icon name="description" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Documents Vault</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple :to="'/packing-list'">
            <q-item-section avatar>
              <q-icon name="checklist" />
            </q-item-section>
            <q-item-section>
              <q-item-label>Packing List</q-item-label>
            </q-item-section>
          </q-item>

          <q-separator class="q-my-sm" />

          <q-item clickable v-ripple :to="'/user-profile'">
            <q-item-section avatar>
              <q-icon name="person" />
            </q-item-section>
            <q-item-section>
              <q-item-label>User Profile</q-item-label>
            </q-item-section>
          </q-item>

          <q-item clickable v-ripple :to="'/settings'">
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
import { ref, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import { useTripStore } from 'src/stores/tripStore';

const $q = useQuasar();
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const tripStore = useTripStore();

const sidebarVisible = ref(true);

watch(
  () => route.path,
  () => {
    if (!$q.screen.gt.sm) {
      sidebarVisible.value = false;
    }
  },
);

function toggleSidebar() {
  sidebarVisible.value = !sidebarVisible.value;
}

function getPageTitle(): string {
  return (route.meta.title as string) || '';
}

async function handleLogout() {
  try {
    tripStore.reset();
    await authStore.signOut();
    await router.push('/login');
  } catch {
    $q.notify({ type: 'negative', message: 'Error logging out' });
  }
}

onMounted(async () => {
  const user = authStore.user;
  if (!user) return;
  const { data } = await supabase
    .from('user_preferences')
    .select('dark_mode')
    .eq('user_id', user.id)
    .single();
  if (data?.dark_mode != null) {
    $q.dark.set(data.dark_mode as boolean);
  }
});
</script>

<style scoped lang="scss">
.q-header {
  border-bottom: 1px solid $border;
  box-shadow: none;
}

.header-brand {
  font-size: 1.0625rem;
  letter-spacing: -0.01em;
}

.page-title {
  font-weight: 500;
}

.sidebar-header {
  display: flex;
  align-items: center;
  height: 64px;
  padding: 0 16px;
  background: linear-gradient(135deg, #0d9488 0%, #065f52 100%);
  flex-shrink: 0;
}
</style>
