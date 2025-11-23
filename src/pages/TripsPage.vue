<template>
  <q-page class="q-pb-xl">
    <!-- Header with Floating Action Button for Adding Trip -->
    <div class="q-pa-md row items-center justify-between sticky-header">
      <div class="text-h4 text-weight-bold">Your Trips</div>
      <q-btn
        round
        color="primary"
        icon="add"
        size="lg"
        @click="openNewTripModal"
        class="shadow-2"
        aria-label="Add new trip"
      />
    </div>

    <div class="q-px-md q-pt-sm">
      <q-list separator class="q-mt-md">
        <!-- Placeholder Trip Items (Replace with v-for="trip in trips") -->
        <q-item clickable v-ripple class="q-py-md shadow-1 q-mb-sm rounded-borders">
          <q-item-section avatar>
            <q-avatar
              icon="directions_bus"
              color="blue-1"
              text-color="primary"
              size="lg"
            />
          </q-item-section>

          <q-item-section>
            <q-item-label class="text-weight-bold">Palawan Adventure 2025</q-item-label>
            <q-item-label caption>Start: May 15, 2025 | 4 Members</q-item-label>
          </q-item-section>

          <q-item-section side>
            <q-icon name="arrow_forward_ios" color="grey-5" />
          </q-item-section>
        </q-item>

        <q-item clickable v-ripple class="q-py-md shadow-1 q-mb-sm rounded-borders">
          <q-item-section avatar>
            <q-avatar
              icon="local_airport"
              color="yellow-1"
              text-color="orange-8"
              size="lg"
            />
          </q-item-section>

          <q-item-section>
            <q-item-label class="text-weight-bold">Japan Food Tour</q-item-label>
            <q-item-label caption>Start: October 20, 2025 | 2 Members</q-item-label>
          </q-item-section>

          <q-item-section side>
            <q-icon name="arrow_forward_ios" color="grey-5" />
          </q-item-section>
        </q-item>

        <div v-if="false" class="text-center q-py-xl text-grey-6">
            <q-icon name="beach_access" size="lg" class="q-mb-md" />
            <p>You haven't planned any trips yet. Start a new gala!</p>
        </div>

      </q-list>
    </div>

    <!-- New Trip Modal (will be implemented in Phase 3) -->
    <q-dialog v-model="showNewTripModal">
      <q-card style="width: 100%; max-width: 400px;">
        <q-card-section>
          <div class="text-h6">Plan a New Trip</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <p>The form to enter trip name, dates, and currency will appear here.</p>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="primary" v-close-popup />
          <q-btn flat label="Create" color="primary" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const showNewTripModal = ref(false);

function openNewTripModal() {
  showNewTripModal.value = true;
}
</script>

<style scoped>
.sticky-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: white;
}
</style>
