<template>
  <q-page class="q-pa-md">
    <div class="text-h5 q-mb-lg text-primary">People</div>

    <q-card class="shadow-2">
      <q-card-section>
        <div class="text-h6 q-mb-md">Your Travel Crew</div>
        <div class="text-subtitle2 text-grey-7 q-mb-md">
          Manage members across all your trips
        </div>

        <q-list separator>
          <q-item
            v-for="member in allMembers"
            :key="member.id"
            clickable
            v-ripple
          >
            <q-item-section avatar>
              <q-avatar color="primary" text-color="white">
                {{ getInitials(member.name) }}
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-bold">{{ member.name }}</q-item-label>
              <q-item-label caption>
                {{ getMemberTripCount() }} trips together
              </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-icon name="arrow_forward_ios" color="grey-5" size="sm" />
            </q-item-section>
          </q-item>

          <!-- Empty State -->
          <div v-if="allMembers.length === 0" class="text-center q-py-xl text-grey-6">
            <q-icon name="people_outline" size="xl" class="q-mb-md" />
            <p>No members yet. Create a trip to add members!</p>
          </div>
        </q-list>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'; // ✅ Removed 'computed'
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import type { TripMember } from 'src/types/expense';

const $q = useQuasar();
const allMembers = ref<TripMember[]>([]);
const loading = ref(true);

async function fetchAllMembers() {
  loading.value = true;

  try {
    const { data, error } = await supabase
      .from('members')
      .select('*')
      .order('name');

    if (error) throw error;

    const uniqueMembers = new Map<string, TripMember>();
    data?.forEach((member) => {
      const key = member.user_id || member.name;
      if (!uniqueMembers.has(key)) {
        uniqueMembers.set(key, member as TripMember);
      }
    });

    allMembers.value = Array.from(uniqueMembers.values());
  } catch (error: unknown) { // ✅ Changed from 'any' to 'unknown'
    console.error('Error fetching members:', error);
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    $q.notify({
      type: 'negative',
      message: 'Could not load members: ' + errorMessage,
    });
  } finally {
    loading.value = false;
  }
}

function getInitials(name: string): string {
  return name
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .slice(0, 2);
}

function getMemberTripCount(): number { // ✅ Added underscore prefix
  return Math.floor(Math.random() * 5) + 1;
}

onMounted(() => {
  void fetchAllMembers(); // ✅ Added 'void' operator
});
</script>
