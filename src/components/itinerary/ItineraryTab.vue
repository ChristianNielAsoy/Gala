<template>
  <ItineraryTimeline
    v-model="itineraryItems"
    :trip-id="tripId"
    :trip-members="tripMembers ?? []"
  />
</template>

<script setup lang="ts">
import ItineraryTimeline from './ItineraryTimeline.vue';
import type { ItineraryItem } from './itinerary.model';

const { tripId, tripMembers } = defineProps<{
  tripId: string;
  tripMembers?: { id: string; name: string; user_id?: string }[];
}>();

const itineraryItems = defineModel<ItineraryItem[]>({ default: () => [] });
</script>
