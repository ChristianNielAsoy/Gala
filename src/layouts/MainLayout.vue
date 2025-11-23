<template>
  <q-layout view="lHh lpr lFf">
    <q-page-container>
      <router-view />
    </q-page-container>

    <!-- Bottom Navigation Bar -->
    <q-footer elevated class="bg-white">
      <q-tabs
        v-model="activeTab"
        align="justify"
        indicator-color="transparent"
        active-color="deep-orange"
        class="text-grey-6 bottom-nav"
      >
        <q-route-tab
          name="home"
          icon="home"
          label="Home"
          to="/home"
          exact
          class="bottom-nav-tab"
        />

        <q-route-tab
          name="trips"
          icon="luggage"
          label="Trips"
          to="/trips"
          exact
          class="bottom-nav-tab"
        />

        <q-route-tab
          name="settings"
          icon="settings"
          label="Settings"
          to="/settings"
          exact
          class="bottom-nav-tab"
        />
      </q-tabs>
    </q-footer>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const activeTab = ref('home');

watch(() => route.path, (newPath) => {
  const segment = newPath.split('/')[1] || 'home';
  if (['home', 'trips', 'settings'].includes(segment)) {
    activeTab.value = segment;
  }
}, { immediate: true });
</script>

<style scoped>
.bottom-nav {
  border-top: 1px solid #e0e0e0;
  padding: 4px 0;
}

.bottom-nav-tab {
  min-height: 56px;
}
</style>
