<template>
  <q-page class="q-pb-xl">
    <!-- Header with Tabs -->
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round icon="arrow_back" @click="handleBack" aria-label="Go Back" />
        <q-toolbar-title>
          {{ trip ? trip.name : 'Loading...' }}
        </q-toolbar-title>
        <q-btn flat round icon="settings" @click="openSettingsModal" aria-label="Settings" />
      </q-toolbar>

      <!-- Navigation Tabs -->
      <q-tabs
        v-model="tab"
        align="justify"
        indicator-color="white"
        active-color="white"
        dense
      >
        <q-tab name="expenses" label="Expenses" icon="receipt_long" />
        <q-tab name="settlement" label="Settlement" icon="account_balance_wallet" />

        <q-tab name="members" label="Members" icon="people" />
        <q-tab name="activity" label="Activity" icon="timeline" />
      </q-tabs>
    </q-header>

    <!-- Tab Panels -->
    <q-tab-panels v-model="tab" animated class="bg-grey-1">

      <!-- Expenses Tab -->
      <q-tab-panel name="expenses" class="q-pa-none">
        <div v-if="loading" class="text-center q-pa-xl">
          <q-spinner color="primary" size="lg" />
        </div>
        <div v-else-if="!trip" class="text-center q-pa-xl">
          <q-icon name="error_outline" size="lg" color="negative" />
          <p class="text-h6 q-mt-sm">Trip Not Found</p>
        </div>
        <div v-else class="q-pa-md">

          <!-- Balance Overview -->
          <q-card class="q-mb-md shadow-2">
            <q-card-section>
              <div class="text-h6 text-primary">Balances Overview</div>
              <div class="text-subtitle2 text-grey-7">
                Total spent: {{ trip.currency_code }} {{ totalSpent.toFixed(2) }}
              </div>
            </q-card-section>
          </q-card>

          <!-- Expense List -->
          <q-card class="q-mb-md shadow-2">
            <q-card-section>
              <div class="text-h6">Expense List</div>
              <q-list separator class="q-mt-sm">
                <q-item
                  v-for="expense in expenses"
                  :key="expense.id"
                  clickable
                  v-ripple
                  @click="handleEditExpense(expense.id)"
                >
                  <q-item-section avatar>
                    <q-avatar
                      :icon="getCategoryIcon(expense.category)"
                      color="grey-2"
                      text-color="primary"
                    />
                  </q-item-section>

                  <q-item-section>
                    <q-item-label class="text-weight-bold">
                      {{ expense.description }}
                    </q-item-label>
                    <q-item-label caption>
                      Paid by {{ getMemberName(expense.paid_by_id) }} | {{ formatDate(expense.date) }}
                    </q-item-label>
                  </q-item-section>

                  <q-item-section side>
                    <q-item-label
                      class="text-weight-bolder text-right"
                      :class="expense.paid_by_id === currentMemberId ? 'text-positive' : 'text-grey-8'"
                    >
                      {{ expense.currency_code }} {{ expense.amount.toFixed(2) }}
                    </q-item-label>
                    <q-item-label caption class="text-right">
                      {{ expense.paid_by_id === currentMemberId ? 'You paid' : 'Shared' }}
                    </q-item-label>
                  </q-item-section>
                </q-item>

                <!-- Empty State -->
                <div class="text-center q-py-lg text-grey-6" v-if="expenses.length === 0">
                  <q-icon name="receipt_long" size="48px" class="q-mb-sm" />
                  <div>No expenses recorded yet</div>
                  <div class="text-caption">Tap the + button to add one</div>
                </div>
              </q-list>
            </q-card-section>
          </q-card>
        </div>
      </q-tab-panel>

      <!-- Settlement Tab -->
      <q-tab-panel name="settlement" class="q-pa-none">
        <div class="q-pa-md">
          <q-btn
            color="primary"
            class="full-width"
            size="lg"
            label="View Settlement Details"
            icon="account_balance"
            @click="goToSettlement"
          />

          <div class="q-mt-md text-center text-grey-7">
            <q-icon name="info" size="sm" class="q-mr-xs" />
            See who owes whom and mark payments
          </div>
        </div>
      </q-tab-panel>

      <!-- Members Tab -->
      <q-tab-panel name="members" class="q-pa-md">
        <div class="text-h6 q-mb-md">Trip Members</div>
        <q-list bordered separator rounded>
          <q-item v-for="member in members" :key="member.id">
            <q-item-section avatar>
              <q-avatar
                :color="member.is_owner ? 'primary' : 'grey-5'"
                text-color="white"
              >
                {{ member.name.charAt(0).toUpperCase() }}
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-medium">
                {{ member.name }}
              </q-item-label>
              <q-item-label caption v-if="member.is_owner">
                Trip Owner
              </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-icon name="star" color="amber" v-if="member.is_owner" />
            </q-item-section>
          </q-item>

          <!-- Add Member Button -->
          <q-item clickable @click="addMemberDialog = true">
            <q-item-section avatar>
              <q-avatar color="grey-3" text-color="grey-7" icon="add" />
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-grey-7">Add Member</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-tab-panel>

      <!-- Activity Tab -->
      <q-tab-panel name="activity" class="q-pa-md">
        <div class="text-h6 q-mb-md">Recent Activity</div>
        <q-timeline color="primary">
          <q-timeline-entry
            title="Trip Created"
            :subtitle="formatDate(trip?.created_at || '')"
            icon="add_circle"
          >
            <div>{{ trip?.name }} was created</div>
          </q-timeline-entry>

          <q-timeline-entry
            v-for="expense in recentExpenses"
            :key="expense.id"
            :title="`${expense.description}`"
            :subtitle="formatDate(expense.date)"
            icon="receipt_long"
          >
            <div>
              {{ getMemberName(expense.paid_by_id) }} paid
              {{ expense.currency_code }} {{ expense.amount.toFixed(2) }}
            </div>
          </q-timeline-entry>

          <q-timeline-entry
            v-if="expenses.length === 0"
            title="No activity yet"
            subtitle="Start adding expenses"
            icon="info"
          />
        </q-timeline>
      </q-tab-panel>

    </q-tab-panels>

    <!-- Floating Action Button -->
    <q-page-sticky position="bottom-right" :offset="[18, 18]" v-if="tab === 'expenses'">
      <q-btn
        fab
        icon="add"
        color="accent"
        size="lg"
        @click="handleAddExpense"
        aria-label="Add Expense"
      />
    </q-page-sticky>

    <!-- Settings Modal -->
    <q-dialog v-model="showSettings">
      <q-card style="width: 100%; max-width: 400px;">
        <q-card-section>
          <div class="text-h6">Trip Settings: {{ trip?.name }}</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <p>Edit trip details, change currency, or archive the trip.</p>
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="primary" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Add Member Dialog -->
    <q-dialog v-model="addMemberDialog">
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Add Member</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <q-input
            v-model="newMemberName"
            label="Member Name"
            outlined
            autofocus
            @keyup.enter="addMember"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn
            flat
            label="Add"
            color="primary"
            @click="addMember"
            :disable="!newMemberName"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>

  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';
import type { Expense, TripMember } from 'src/types/expense';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();

// State
const tab = ref('expenses');
const loading = ref(true);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const expenses = ref<Expense[]>([]);
const showSettings = ref(false);
const addMemberDialog = ref(false);
const newMemberName = ref('');
const tripId = ref(route.params.tripId as string);

let currentUserId: string | undefined;
supabase.auth.getUser().then(({ data: { user } }) => {
  currentUserId = user?.id;
}).catch(console.error);

const currentMemberId = computed(() =>
  members.value.find((m: TripMember) => m.user_id === currentUserId)?.id
);

// Computed
const totalSpent = computed(() =>
  expenses.value.reduce((sum: number, expense: Expense) => sum + expense.amount, 0)
);

const recentExpenses = computed(() =>
  expenses.value.slice(0, 5).sort((a, b) =>
    new Date(b.date).getTime() - new Date(a.date).getTime()
  )
);

// Methods
function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    'Food': 'restaurant',
    'Lodging': 'hotel',
    'Transport': 'commute',
    'Activity': 'attractions',
    'Groceries': 'local_grocery_store',
  };
  return icons[category] || 'receipt_long';
}

function getMemberName(memberId: string): string {
  return members.value.find((m: TripMember) => m.id === memberId)?.name || 'Unknown';
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

async function fetchTripData(): Promise<void> {
  loading.value = true;

  try {
    // Fetch trip
    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .select('*')
      .eq('id', tripId.value)
      .single();

    if (tripError || !tripData) throw new Error('Trip not found');
    trip.value = tripData as Trip;

    // Fetch members
    const { data: memberData } = await supabase
      .from('members')
      .select('*')
      .eq('trip_id', tripId.value)
      .order('name');

    members.value = (memberData as TripMember[]) || [];

    // Fetch expenses
    const { data: expenseData } = await supabase
      .from('expenses')
      .select('*')
      .eq('trip_id', tripId.value)
      .order('date', { ascending: false });

    expenses.value = (expenseData as Expense[]) || [];

  } catch (error) {
    console.error('Error fetching trip data:', error);
    $q.notify({ type: 'negative', message: 'Failed to load trip' });
  } finally {
    loading.value = false;
  }
}

function goToSettlement() {
  void router.push(`/trips/${tripId.value}/settlement`);
}

async function addMember(): Promise<void> {
  if (!newMemberName.value.trim()) return;

  try {
    const { error } = await supabase
      .from('members')
      .insert({
        trip_id: tripId.value,
        name: newMemberName.value.trim(),
        is_owner: false
      });

    if (error) throw error;

    $q.notify({ type: 'positive', message: 'Member added!' });
    newMemberName.value = '';
    addMemberDialog.value = false;
    await fetchTripData();

  } catch (error) {
    console.error('Error adding member:', error);
    $q.notify({ type: 'negative', message: 'Failed to add member' });
  }
}

function openSettingsModal(): void {
  showSettings.value = true;
}

function handleAddExpense(): void {
  void router.push(`/trips/${tripId.value}/expense/new`);
}

function handleEditExpense(expenseId: string): void {
  void router.push(`/trips/${tripId.value}/expense/${expenseId}`);
}

function handleBack(): void {
  void router.back();
}

// Lifecycle
onMounted(() => {
  void fetchTripData();
});
</script>

<style scoped>
.q-page {
  min-height: 100vh;
}
</style>
