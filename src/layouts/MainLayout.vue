<template>
  <q-layout view="hHh lpr fFf">
    <!-- ═══ DESKTOP: Minimal top bar ═══ -->
    <q-header v-if="isDesktop" class="layout-header" height-hint="56">
      <q-toolbar class="q-px-lg" style="min-height: 56px">
        <q-avatar size="30px" class="q-mr-xs">
          <img src="/logo.png" alt="Gala Logo" />
        </q-avatar>
        <span class="header-brand gala-display text-primary">Gala</span>

        <q-space />

        <!-- Notification Bell -->
        <q-btn flat round dense class="q-mr-xs header-icon-btn" aria-label="Notifications">
          <q-icon name="notifications_outlined" size="20px" />
          <q-badge
            v-if="unreadCount > 0"
            color="negative"
            :label="unreadCount > 9 ? '9+' : String(unreadCount)"
            floating rounded
          />
          <q-menu
            anchor="bottom right"
            self="top right"
            class="notif-menu"
            @before-show="markNotificationsRead"
          >
            <div class="row items-center justify-between q-px-md q-pt-sm q-pb-xs">
              <div class="text-subtitle2 text-weight-bold">Notifications</div>
              <q-btn flat dense no-caps size="sm" label="See all" color="primary" :to="'/trips'" v-close-popup />
            </div>
            <q-separator />
            <div v-if="loadingNotifs" class="text-center q-pa-md">
              <q-spinner size="sm" color="primary" />
            </div>
            <div v-else-if="notifications.length === 0" class="text-center q-pa-xl" style="color: var(--muted)">
              <q-icon name="notifications_none" size="lg" class="q-mb-sm" style="color: var(--border)" />
              <div class="text-caption">No recent activity</div>
            </div>
            <q-list v-else class="gala-scroll" style="max-height: 360px; overflow-y: auto">
              <q-item
                v-for="n in notifications"
                :key="n.id"
                clickable
                v-close-popup
                :class="isUnread(n) ? 'notif-unread' : ''"
                @click="goToTripFromNotif(n.trip_id)"
                class="q-py-sm"
              >
                <q-item-section avatar>
                  <q-avatar
                    size="34px"
                    :color="isUnread(n) ? 'primary' : 'grey-3'"
                    :text-color="isUnread(n) ? 'white' : 'grey-7'"
                  >
                    <q-icon :name="getNotifIcon(n.action_type)" size="16px" />
                  </q-avatar>
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-caption text-weight-medium">{{ n.description }}</q-item-label>
                  <q-item-label caption style="color: var(--muted)">{{ formatRelativeTime(n.created_at) }}</q-item-label>
                </q-item-section>
                <q-item-section side v-if="isUnread(n)">
                  <div class="notif-dot" />
                </q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>

        <!-- User Avatar + Menu -->
        <q-btn flat round dense aria-label="User Menu" class="q-ml-xs">
          <q-avatar size="32px" color="primary" text-color="white">
            <img v-if="authStore.userAvatar" :src="authStore.userAvatar" alt="User Avatar" />
            <span v-else class="text-caption text-weight-bold">{{ authStore.userInitials }}</span>
          </q-avatar>
          <q-menu anchor="bottom right" self="top right" style="min-width: 160px">
            <q-list dense>
              <q-item clickable :to="'/user-profile'" v-close-popup>
                <q-item-section avatar><q-icon name="person_outline" size="18px" /></q-item-section>
                <q-item-section>Profile</q-item-section>
              </q-item>
              <q-item clickable :to="'/settings'" v-close-popup>
                <q-item-section avatar><q-icon name="settings" size="18px" /></q-item-section>
                <q-item-section>Settings</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable @click="handleLogout" v-close-popup>
                <q-item-section avatar><q-icon name="logout" size="18px" color="negative" /></q-item-section>
                <q-item-section class="text-negative">Logout</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <!-- ═══ MOBILE: Compact top bar ═══ -->
    <q-header v-else class="layout-header" height-hint="52">
      <q-toolbar style="min-height: 52px">
        <q-avatar size="28px" class="q-mr-xs">
          <img src="/logo.png" alt="Gala Logo" />
        </q-avatar>
        <span class="header-brand gala-display text-primary">Gala</span>

        <q-space />

        <q-btn flat round dense class="header-icon-btn" aria-label="Notifications">
          <q-icon name="notifications_outlined" size="20px" />
          <q-badge
            v-if="unreadCount > 0"
            color="negative"
            :label="unreadCount > 9 ? '9+' : String(unreadCount)"
            floating rounded
          />
          <q-menu
            anchor="bottom right"
            self="top right"
            class="notif-menu"
            @before-show="markNotificationsRead"
          >
            <div class="row items-center justify-between q-px-md q-pt-sm q-pb-xs">
              <div class="text-subtitle2 text-weight-bold">Notifications</div>
              <q-btn flat dense no-caps size="sm" label="See all" color="primary" :to="'/trips'" v-close-popup />
            </div>
            <q-separator />
            <div v-if="loadingNotifs" class="text-center q-pa-md">
              <q-spinner size="sm" color="primary" />
            </div>
            <div v-else-if="notifications.length === 0" class="text-center q-pa-xl" style="color: var(--muted)">
              <q-icon name="notifications_none" size="lg" class="q-mb-sm" style="color: var(--border)" />
              <div class="text-caption">No recent activity</div>
            </div>
            <q-list v-else class="gala-scroll" style="max-height: 320px; overflow-y: auto">
              <q-item
                v-for="n in notifications"
                :key="n.id"
                clickable
                v-close-popup
                :class="isUnread(n) ? 'notif-unread' : ''"
                @click="goToTripFromNotif(n.trip_id)"
                class="q-py-sm"
              >
                <q-item-section avatar>
                  <q-avatar
                    size="32px"
                    :color="isUnread(n) ? 'primary' : 'grey-3'"
                    :text-color="isUnread(n) ? 'white' : 'grey-7'"
                  >
                    <q-icon :name="getNotifIcon(n.action_type)" size="14px" />
                  </q-avatar>
                </q-item-section>
                <q-item-section>
                  <q-item-label class="text-caption text-weight-medium">{{ n.description }}</q-item-label>
                  <q-item-label caption style="color: var(--muted)">{{ formatRelativeTime(n.created_at) }}</q-item-label>
                </q-item-section>
                <q-item-section side v-if="isUnread(n)">
                  <div class="notif-dot" />
                </q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>

        <q-btn flat round dense aria-label="User Menu" class="q-ml-xs">
          <q-avatar size="28px" color="primary" text-color="white">
            <img v-if="authStore.userAvatar" :src="authStore.userAvatar" alt="User Avatar" />
            <span v-else style="font-size: 0.65rem" class="text-weight-bold">{{ authStore.userInitials }}</span>
          </q-avatar>
          <q-menu anchor="bottom right" self="top right" style="min-width: 160px">
            <q-list dense>
              <q-item clickable :to="'/user-profile'" v-close-popup>
                <q-item-section avatar><q-icon name="person_outline" size="18px" /></q-item-section>
                <q-item-section>Profile</q-item-section>
              </q-item>
              <q-item clickable :to="'/settings'" v-close-popup>
                <q-item-section avatar><q-icon name="settings" size="18px" /></q-item-section>
                <q-item-section>Settings</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable @click="handleLogout" v-close-popup>
                <q-item-section avatar><q-icon name="logout" size="18px" color="negative" /></q-item-section>
                <q-item-section class="text-negative">Logout</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>
      </q-toolbar>
    </q-header>

    <!-- ═══ DESKTOP: Icon Rail Sidebar ═══ -->
    <div v-if="isDesktop" class="gala-rail" :class="{ 'gala-rail--expanded': railExpanded }" @mouseenter="railExpanded = true" @mouseleave="railExpanded = false">
      <div class="gala-rail__spacer" style="height: 56px" />

      <router-link
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="gala-rail__item"
        :class="{ 'gala-rail__item--active': isNavActive(item.to) }"
      >
        <q-icon :name="isNavActive(item.to) ? item.iconActive : item.icon" />
        <span class="gala-rail__label">{{ item.label }}</span>
      </router-link>

      <div class="gala-rail__divider" />

      <router-link
        v-for="item in navSecondary"
        :key="item.to"
        :to="item.to"
        class="gala-rail__item"
        :class="{ 'gala-rail__item--active': isNavActive(item.to) }"
      >
        <q-icon :name="isNavActive(item.to) ? item.iconActive : item.icon" />
        <span class="gala-rail__label">{{ item.label }}</span>
      </router-link>

      <div class="gala-rail__spacer" />

      <router-link to="/settings" class="gala-rail__item" :class="{ 'gala-rail__item--active': isNavActive('/settings') }">
        <q-icon :name="isNavActive('/settings') ? 'settings' : 'settings'" />
        <span class="gala-rail__label">Settings</span>
      </router-link>

      <div style="height: var(--space-md)" />
    </div>

    <!-- ═══ Page Content ═══ -->
    <q-page-container :style="isDesktop ? 'padding-left: 64px' : ''">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>

      <!-- Mobile bottom nav spacer -->
      <div v-if="!isDesktop" class="gala-bottom-nav-spacer" />
    </q-page-container>

    <!-- ═══ MOBILE: Bottom Tab Bar ═══ -->
    <div v-if="!isDesktop" class="gala-bottom-nav">
      <router-link
        v-for="item in bottomNavItems"
        :key="item.to"
        :to="item.to"
        class="gala-bottom-nav__item"
        :class="{ 'gala-bottom-nav__item--active': isNavActive(item.to) }"
      >
        <q-icon :name="isNavActive(item.to) ? item.iconActive : item.icon" />
        <span>{{ item.label }}</span>
      </router-link>

      <!-- Center FAB -->
      <div class="gala-bottom-nav__fab" @click="showQuickActions = true">
        <q-icon name="add" />
      </div>

      <router-link
        v-for="item in bottomNavItemsRight"
        :key="item.to"
        :to="item.to"
        class="gala-bottom-nav__item"
        :class="{ 'gala-bottom-nav__item--active': isNavActive(item.to) }"
      >
        <q-icon :name="isNavActive(item.to) ? item.iconActive : item.icon" />
        <span>{{ item.label }}</span>
      </router-link>
    </div>

    <!-- ═══ Quick Actions Dialog ═══ -->
    <q-dialog v-model="showQuickActions" position="bottom">
      <q-card class="quick-actions-card">
        <div class="quick-actions-handle" />
        <q-card-section class="q-pb-none">
          <div class="text-subtitle1 text-weight-bold">Quick Actions</div>
        </q-card-section>
        <q-card-section class="row q-gutter-md justify-center q-pb-lg">
          <div class="quick-action-item" @click="quickAction('/trips')">
            <div class="quick-action-icon gala-gradient-primary">
              <q-icon name="flight_takeoff" color="white" size="24px" />
            </div>
            <span class="text-caption text-weight-medium">New Trip</span>
          </div>
          <div class="quick-action-item" @click="quickAction('/expense-analytics')">
            <div class="quick-action-icon gala-gradient-accent">
              <q-icon name="receipt_long" color="white" size="24px" />
            </div>
            <span class="text-caption text-weight-medium">Add Expense</span>
          </div>
          <div class="quick-action-item" @click="quickAction('/documents-vault')">
            <div class="quick-action-icon" style="background: linear-gradient(135deg, #3B82F6, #6366F1)">
              <q-icon name="description" color="white" size="24px" />
            </div>
            <span class="text-caption text-weight-medium">Documents</span>
          </div>
          <div class="quick-action-item" @click="quickAction('/packing-list')">
            <div class="quick-action-icon" style="background: linear-gradient(135deg, #10B981, #059669)">
              <q-icon name="checklist" color="white" size="24px" />
            </div>
            <span class="text-caption text-weight-medium">Packing List</span>
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
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

const railExpanded = ref(false);
const showQuickActions = ref(false);

const isDesktop = computed(() => $q.screen.gt.sm);

// ─── Navigation items ─────────────────────────────────────────────────────
const navItems = [
  { to: '/dashboard', icon: 'dashboard_outlined', iconActive: 'dashboard', label: 'Dashboard' },
  { to: '/trips', icon: 'luggage_outlined', iconActive: 'luggage', label: 'Trips' },
  { to: '/expense-analytics', icon: 'analytics_outlined', iconActive: 'analytics', label: 'Analytics' },
];

const navSecondary = [
  { to: '/documents-vault', icon: 'description_outlined', iconActive: 'description', label: 'Documents' },
  { to: '/packing-list', icon: 'checklist_outlined', iconActive: 'checklist', label: 'Packing' },
  { to: '/user-profile', icon: 'person_outline', iconActive: 'person', label: 'Profile' },
];

// Mobile bottom nav — split left/right around FAB
const bottomNavItems = [
  { to: '/dashboard', icon: 'dashboard_outlined', iconActive: 'dashboard', label: 'Home' },
  { to: '/trips', icon: 'luggage_outlined', iconActive: 'luggage', label: 'Trips' },
];

const bottomNavItemsRight = [
  { to: '/expense-analytics', icon: 'analytics_outlined', iconActive: 'analytics', label: 'Analytics' },
  { to: '/user-profile', icon: 'person_outline', iconActive: 'person', label: 'Profile' },
];

function isNavActive(path: string): boolean {
  return route.path === path || route.path.startsWith(path + '/');
}

function quickAction(path: string) {
  showQuickActions.value = false;
  void router.push(path);
}

// ─── Notification centre ───────────────────────────────────────────────────
interface NotifEntry {
  id: string;
  trip_id: string;
  action_type: string;
  description: string;
  created_at: string;
}

const NOTIF_KEY = 'gala_notif_last_read';
const notifications = ref<NotifEntry[]>([]);
const loadingNotifs = ref(false);
const lastReadAt = ref<string>(localStorage.getItem(NOTIF_KEY) ?? '');

const unreadCount = computed(() =>
  notifications.value.filter((n) => isUnread(n)).length
);

function isUnread(n: NotifEntry): boolean {
  if (!lastReadAt.value) return true;
  return n.created_at > lastReadAt.value;
}

function markNotificationsRead() {
  const now = new Date().toISOString();
  lastReadAt.value = now;
  localStorage.setItem(NOTIF_KEY, now);
}

function getNotifIcon(actionType: string): string {
  if (actionType.startsWith('expense')) return 'receipt_long';
  if (actionType.startsWith('settlement')) return 'account_balance_wallet';
  if (actionType.startsWith('member')) return 'people';
  if (actionType.startsWith('trip')) return 'flight_takeoff';
  return 'notifications';
}

function formatRelativeTime(dateStr: string): string {
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

function goToTripFromNotif(tripId: string) {
  void router.push(`/trips/${tripId}`);
}

async function fetchNotifications() {
  const ids = tripStore.tripIds;
  if (!ids.length) return;
  loadingNotifs.value = true;
  try {
    const { data } = await supabase
      .from('activity_log')
      .select('id, trip_id, action_type, description, created_at')
      .in('trip_id', ids)
      .order('created_at', { ascending: false })
      .limit(20);
    notifications.value = (data || []) as NotifEntry[];
  } finally {
    loadingNotifs.value = false;
  }
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
  await tripStore.fetchTrips();
  void fetchNotifications();
});
</script>

<style scoped lang="scss">
// ─── Header ──────────────────────────────────────────────────────────────────
.layout-header {
  background: var(--gala-glass-bg) !important;
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid var(--gala-glass-border);
  box-shadow: none;
  color: var(--on-background) !important;
}

.header-brand {
  font-size: 1.25rem;
  letter-spacing: -0.03em;
}

.header-icon-btn {
  color: var(--muted);

  &:hover {
    color: var(--on-background);
  }
}

// ─── Notification menu ───────────────────────────────────────────────────────
.notif-menu {
  min-width: 310px;
  max-width: 360px;
  border-radius: var(--gala-radius-lg) !important;
  box-shadow: var(--gala-elevation-float) !important;
  border: 1px solid var(--gala-glass-border);
}

.notif-unread {
  background: rgba(13, 148, 136, 0.06);
}

.notif-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--q-primary);
}

// ─── Quick Actions Dialog ────────────────────────────────────────────────────
.quick-actions-card {
  width: 100%;
  max-width: 400px;
  border-radius: var(--gala-radius-xl) var(--gala-radius-xl) 0 0 !important;
  background: var(--background);
  border: none;
  box-shadow: var(--gala-elevation-float);
}

.quick-actions-handle {
  width: 36px;
  height: 4px;
  border-radius: 2px;
  background: var(--border);
  margin: 12px auto 0;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;

  &:active .quick-action-icon {
    transform: scale(0.92);
  }
}

.quick-action-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 150ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
</style>
