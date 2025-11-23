<template>
  <q-layout view="lHh lpr lFf">
    <q-page-container>
      <!-- Main view content loads here -->
      <router-view />
    </q-page-container>

    <!-- Persistent Bottom Navigation Bar -->
    <q-footer class="bg-white q-py-xs" bordered>
      <q-tabs
        v-model="activeTab"
        align="justify"
        indicator-color="primary"
        no-caps
        active-color="primary"
        class="text-grey-7"
      >
        <q-route-tab
          name="dashboard"
          icon="dashboard"
          label="Dashboard"
          to="/dashboard"
          exact
        />

        <q-route-tab
          name="trips"
          icon="flight_takeoff"
          label="Trips"
          to="/trips"
          exact
        />

        <q-route-tab
          name="people"
          icon="people"
          label="People"
          to="/people"
          exact
        />

        <!-- Add a route for profile/settings -->
        <q-route-tab
          name="settings"
          icon="settings"
          label="Settings"
          to="/settings"
          exact
        />
      </q-tabs>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

// The active tab is synchronized with the current route path
const activeTab = ref(route.path.split('/')[1] || 'dashboard');

// Watch for route changes to update the active tab
watch(() => route.path, (newPath) => {
  // Sets the active tab name based on the first segment of the path (e.g., '/trips' -> 'trips')
  activeTab.value = newPath.split('/')[1] || 'dashboard';
});
</script>

<style scoped>
/* Ensure footer sits above content on mobile */
.q-footer {
  box-shadow: 0 -2px 5px rgba(0, 0, 0, 0.05);
}
</style>
