<template>
  <ItineraryTimeline
    v-model="itineraryItems"
    :trip-id="tripId"
    :trip-members="tripMembers ?? []"
    :trip-start-date="tripStartDate"
    :trip-end-date="tripEndDate"
  />
</template>

<script setup lang="ts">
import ItineraryTimeline from './ItineraryTimeline.vue';
import type { ItineraryItem } from './itinerary.model';

const { tripId, tripMembers, tripStartDate, tripEndDate } = defineProps<{
  tripId: string;
  tripMembers?: { id: string; name: string; user_id?: string }[];
  tripStartDate?: string | undefined;
  tripEndDate?: string | undefined;
}>();

const itineraryItems = defineModel<ItineraryItem[]>({ default: () => [] });
</script>
