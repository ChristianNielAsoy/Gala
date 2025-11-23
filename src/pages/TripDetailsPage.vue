<template>
  <q-page class="q-pb-xl">
    <!-- Header with Back Button and Trip Name -->
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round icon="arrow_back" @click="router.back()" aria-label="Go Back to Trips" />
        <q-toolbar-title>
          {{ trip ? trip.name : 'Loading Trip...' }}
        </q-toolbar-title>
        <!-- Trip Settings Button -->
        <q-btn flat round icon="settings" @click="openSettingsModal" aria-label="Trip Settings" />
      </q-toolbar>

      <!-- Nested Tabs for Navigation within the Trip -->
      <q-tabs
        v-model="tab"
        align="justify"
        indicator-color="white"
        active-color="white"
        dense
      >
        <q-tab name="expenses" label="Expenses" icon="receipt_long" />
        <q-tab name="members" label="Members" icon="people" />
        <q-tab name="activity" label="Activity" icon="timeline" />
      </q-tabs>
    </q-header>

    <!-- Page Content (Matching current Tab) -->
    <q-tab-panels v-model="tab" animated class="bg-grey-1">
      <!-- Expenses Tab Panel -->
      <q-tab-panel name="expenses" class="q-pa-none">
        <div v-if="loading" class="text-center q-pa-xl">
          <q-spinner color="primary" size="lg" />
        </div>
        <div v-else-if="!trip" class="text-center q-pa-xl">
          <q-icon name="error_outline" size="lg" color="negative" />
          <p class="text-h6 q-mt-sm">Trip Not Found</p>
        </div>
        <div v-else class="q-pa-md">
          <!-- Balances Overview -->
          <q-card class="q-mb-md shadow-2">
            <q-card-section>
              <div class="text-h6 text-primary">Balances Overview</div>
              <div class="text-subtitle2 text-grey-7">Total spent: {{ trip.currency_code }} {{ totalSpent.toFixed(2) }}</div>
            </q-card-section>
          </q-card>

          <!-- Expense List -->
          <q-card class="q-mb-md shadow-2">
            <q-card-section>
              <div class="text-h6">Expense List</div>
              <q-list separator class="q-mt-sm">

                <!-- Actual Expense Listing -->
                <q-item
                  v-for="expense in expenses"
                  :key="expense.id"
                  clickable
                  v-ripple
                  @click="editExpense(expense.id)"
                >
                  <q-item-section avatar>
                    <q-avatar :icon="getCategoryIcon(expense.category)" color="grey-2" text-color="primary" />
                  </q-item-section>

                  <q-item-section>
                    <q-item-label class="text-weight-bold">{{ expense.description }}</q-item-label>
                    <q-item-label caption>
                      Paid by {{ getMemberName(expense.paid_by_id) }} | {{ expense.date }}
                    </q-item-label>
                  </q-item-section>

                  <q-item-section side>
                    <q-item-label class="text-weight-bolder text-right" :class="expense.paid_by_id === currentMemberId ? 'text-positive' : 'text-negative'">
                        {{ expense.currency_code }} {{ expense.amount.toFixed(2) }}
                    </q-item-label>
                    <q-item-label caption class="text-right">
                        {{ expense.paid_by_id === currentMemberId ? 'You paid' : 'Settlement due' }}
                    </q-item-label>
                  </q-item-section>

                </q-item>

                <!-- Empty State for Expenses -->
                <div class="text-center q-py-lg text-grey-6" v-if="expenses.length === 0">
                  No expenses recorded yet. Be the first to add one!
                </div>
              </q-list>
            </q-card-section>
          </q-card>
        </div>
      </q-tab-panel>

      <!-- Members Tab Panel -->
      <q-tab-panel name="members">
        <div class="text-h6 q-mb-md">Trip Members</div>
        <q-list bordered separator rounded>
          <q-item v-for="member in members" :key="member.id">
            <q-item-section avatar>
              <q-avatar icon="person" :color="member.is_owner ? 'primary' : 'grey-5'" text-color="white" />
            </q-item-section>
            <q-item-section>{{ member.name }} {{ member.is_owner ? '(Owner)' : '' }}</q-item-section>
            <q-item-section side>
              <q-icon name="star" color="amber" v-if="member.is_owner" />
              <q-btn flat round icon="share" color="primary" size="sm" v-else @click="shareInvite(member.id)" />
            </q-item-section>
          </q-item>
        </q-list>
      </q-tab-panel>

      <!-- Activity Tab Panel -->
      <q-tab-panel name="activity">
        <div class="text-h6 q-mb-md">Recent Activity</div>
        <p class="text-grey-7">Timeline of expenses, payments, and member changes will appear here.</p>
        <q-timeline color="secondary">
          <q-timeline-entry title="Trip Created" subtitle="Yesterday" icon="add_circle">
            <div>The {{ trip?.name }} was started by you.</div>
          </q-timeline-entry>
          <q-timeline-entry title="Expense Added" subtitle="2 hours ago" icon="receipt_long" v-if="expenses.length > 0">
            <div>{{ expenses[0].description }} ({{ expenses[0].currency_code }} {{ expenses[0].amount.toFixed(2) }}) was added.</div>
          </q-timeline-entry>
        </q-timeline>
      </q-tab-panel>
    </q-tab-panels>

    <!-- Floating Action Button for Adding EXPENSE (Visible only on Expenses tab) -->
    <q-page-sticky position="bottom-right" :offset="[18, 18]" v-if="tab === 'expenses'">
      <q-btn fab icon="add" color="accent" size="lg" aria-label="Add Expense" @click="addExpense" />
    </q-page-sticky>

    <!-- Trip Settings Modal Placeholder -->
    <q-dialog v-model="showSettings">
      <q-card style="width: 100%; max-width: 400px;">
        <q-card-section>
          <div class="text-h6">Trip Settings: {{ trip?.name }}</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <p>This is where you can edit trip details, change currency, or archive the trip.</p>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Close" color="primary" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { Trip } from 'src/types/trip';
import { Expense, TripMember } from 'src/types/expense'; // Import Expense and TripMember

const route = useRoute();
const router = useRouter();
const $q = useQuasar();

// State
const tab = ref('expenses'); // Default to the expenses tab
const loading = ref(true);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const expenses = ref<Expense[]>([]);
const showSettings = ref(false);

// Get the trip ID from the route parameters
const tripId = ref(route.params.tripId as string);
const currentUserId = supabase.auth.currentUser?.id;
const currentMemberId = computed(() => members.value.find(m => m.user_id === currentUserId)?.id);

// --- Computed ---

const totalSpent = computed(() => {
  return expenses.value.reduce((sum, expense) => sum + expense.amount, 0);
});

// --- Utility Functions ---

function getCategoryIcon(category: string): string {
  switch (category) {
    case 'Food': return 'restaurant';
    case 'Lodging': return 'hotel';
    case 'Transport': return 'commute';
    case 'Activity': return 'attractions';
    case 'Groceries': return 'local_grocery_store';
    default: return 'receipt_long';
  }
}

function getMemberName(memberId: string): string {
  return members.value.find(m => m.id === memberId)?.name || 'Unknown';
}

function shareInvite() {
  $q.notify({ type: 'info', message: 'Share invite link feature coming soon!' });
}

// --- Data Fetching ---

/**
 * Fetches all necessary data for the trip: details, members, and expenses.
 */
async function fetchTripData() {
  loading.value = true;
  const id = tripId.value;

  if (!id) {
    trip.value = null;
    loading.value = false;
    return;
  }

  try {
    // 1. Fetch Trip Details
    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .select('*')
      .eq('id', id)
      .single();

    if (tripError || !tripData) throw new Error('Trip not found or access denied.');
    trip.value = tripData as Trip;

    // 2. Fetch Trip Members (Needed for expense display)
    const { data: memberData, error: memberError } = await supabase
        .from('members')
        .select('*')
        .eq('trip_id', id)
        .order('name');

    if (memberError || !memberData) throw new Error('Could not load trip members.');
    members.value = memberData as TripMember[];


    // 3. Fetch Expenses
    const { data: expenseData, error: expenseError } = await supabase
        .from('expenses')
        .select('*')
        .eq('trip_id', id)
        .order('date', { ascending: false });

    if (expenseError || !expenseData) throw new Error('Could not load expenses.');
    expenses.value = expenseData as Expense[];

  } catch (error: any) {
    console.error('Error fetching trip data:', error);
    $q.notify({ type: 'negative', message: error.message || 'Error fetching trip data.' });
    trip.value = null;
    members.value = [];
    expenses.value = [];
  } finally {
    loading.value = false;
  }
}

// --- Action Handlers ---

function openSettingsModal() {
  showSettings.value = true;
}

/**
 * Navigates to the Expense Editor to ADD a new expense.
 */
function addExpense() {
  // Navigate to the Expense Editor page, passing the tripId
  router.push(`/trips/${tripId.value}/expense/new`);
}

/**
 * Navigates to the Expense Editor to EDIT an existing expense.
 */
function editExpense(expenseId: string) {
  // Navigate to the Expense Editor page, passing the tripId and expenseId
  router.push(`/trips/${tripId.value}/expense/${expenseId}`);
}


// --- Lifecycle and Watchers ---

// Watch the route param to re-fetch if the user navigates between trip detail pages
watch(() => route.params.tripId, (newId) => {
  if (newId) {
    tripId.value = newId as string;
    fetchTripData();
  }
});

onMounted(() => {
  fetchTripData();

  // Set up real-time subscription for expenses and members in this trip
  supabase
    .channel(`trip_${tripId.value}_changes`)
    .on('postgres_changes', {
        event: '*',
        schema: 'public',
        table: 'expenses',
        filter: `trip_id=eq.${tripId.value}`
    }, (payload) => {
        console.log('Realtime expense change detected:', payload);
        // Only re-fetch if the change affects this trip
        if (payload.new.trip_id === tripId.value || payload.old.trip_id === tripId.value) {
            fetchTripData();
        }
    })
    .on('postgres_changes', {
        event: '*',
        schema: 'public',
        table: 'members',
        filter: `trip_id=eq.${tripId.value}`
    }, (payload) => {
        console.log('Realtime member change detected:', payload);
        if (payload.new.trip_id === tripId.value || payload.old.trip_id === tripId.value) {
            fetchTripData();
        }
    })
    .subscribe();
});
</script>

<style scoped>
/* Ensure the page uses the full viewport height */
.q-page {
  min-height: 100vh;
}
</style>
