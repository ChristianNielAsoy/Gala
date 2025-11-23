<template>
  <q-layout view="lHh lpr lFf">
    <q-page-container>
      <router-view />
    </q-page-container>

    <!-- Persistent Bottom Navigation Bar -->
    <q-footer class="bg-white q-py-xs" bordered elevated>
      <q-tabs
        v-model="activeTab"
        align="justify"
        indicator-color="transparent"
        no-caps
        active-color="primary"
        class="text-grey-7"
      >
        <q-route-tab
          name="dashboard"
          icon="home"
          label="Home"
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

const activeTab = ref(route.path.split('/')[1] || 'dashboard');

watch(() => route.path, (newPath) => {
  activeTab.value = newPath.split('/')[1] || 'dashboard';
});
</script>

<style scoped>
.q-footer {
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
}
</style>
