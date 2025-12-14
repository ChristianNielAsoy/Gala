<template>
  <ItineraryTimeline v-model="itineraryItems" />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ItineraryTimeline from './ItineraryTimeline.vue';

import type { ItineraryItem } from './itinerary.model';

const itineraryItems = ref<ItineraryItem[]>([]);
</script>
