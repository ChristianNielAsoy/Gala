<template>
  <q-page class="q-pb-xl bg-surface">
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
      <q-tabs v-model="tab" align="justify" indicator-color="white" active-color="white" dense>
        <q-tab name="itinerary" label="Itinerary" icon="event_note" />
        <q-tab name="expenses" label="Expenses" icon="receipt_long" />
        <q-tab name="settlement" label="Settlement" icon="account_balance_wallet" />
        <q-tab name="members" label="Members" icon="people" />
        <q-tab name="activity" label="Activity" icon="timeline" />
      </q-tabs>
    </q-header>

    <!-- Tab Panels -->
    <q-tab-panels v-model="tab" animated class="bg-surface">
      <!-- Itinerary Tab Panel -->
      <q-tab-panel name="itinerary" class="q-pa-md">
        <itinerary-tab
          v-model="itineraryItems"
          :trip-id="trip?.id ?? ''"
          :trip-members="members"
        />
      </q-tab-panel>

      <!-- Expenses Tab Panel -->
      <q-tab-panel name="expenses" class="q-pa-md">
        <div class="row items-center justify-between q-mb-md">
          <div class="text-h6">Expenses</div>
          <q-btn
            unelevated
            color="primary"
            icon="add"
            label="Add Expense"
            size="sm"
            @click="goToAddExpense"
          />
        </div>

        <div v-if="loadingExpenses" class="text-center q-pa-xl">
          <q-spinner color="primary" size="md" />
        </div>

        <div v-else-if="expenses.length === 0" class="text-center q-pa-xl">
          <q-icon name="receipt_long" size="xl" color="grey-4" class="q-mb-md" />
          <p class="text-grey-6">No expenses yet.</p>
          <q-btn flat color="primary" label="Add the first one" @click="goToAddExpense" />
        </div>

        <q-list v-else bordered separator rounded>
          <q-item
            v-for="expense in expenses"
            :key="expense.id"
            clickable
            @click="goToEditExpense(expense.id)"
          >
            <q-item-section avatar>
              <q-avatar color="primary" text-color="white" size="md">
                <q-icon :name="getCategoryIcon(expense.category)" size="sm" />
              </q-avatar>
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-weight-medium">{{ expense.description }}</q-item-label>
              <q-item-label caption>
                {{ expense.category }} · {{ formatDate(expense.date) }} · paid by {{ getMemberName(expense.paid_by_id) }}
              </q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-item-label class="text-weight-bold text-primary">
                {{ formatAmount(expense.amount, trip?.currency_code) }}
              </q-item-label>
              <q-item-label caption>{{ expense.split_type || 'equal' }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>

        <div v-if="expenses.length > 0" class="q-mt-md text-right text-caption text-grey-7">
          Total: <span class="text-weight-bold text-body2">{{ formatAmount(totalExpenses, trip?.currency_code) }}</span>
        </div>
      </q-tab-panel>

      <!-- Settlement Tab Panel -->
      <q-tab-panel name="settlement" class="q-pa-none">
        <settlement-view
          v-if="trip && members.length > 0 && !loading"
          :expenses="expensesWithSplits"
          :members="members"
          :current-member-id="currentMemberId ?? ''"
          :currency-code="trip.currency_code"
          :trip-id="trip.id"
        />
        <div v-else class="text-center q-pa-xl">
          <q-spinner color="primary" size="lg" />
          <p class="text-grey-6 q-mt-md">Loading settlement data...</p>
        </div>
      </q-tab-panel>

      <!-- Members Tab -->
      <q-tab-panel name="members" class="q-pa-md">
        <div class="text-h6 q-mb-md">Trip Members</div>
        <q-list bordered separator rounded>
          <q-item v-for="member in members" :key="member.id">
            <q-item-section avatar>
              <q-avatar :color="member.is_owner ? 'primary' : 'grey-5'" text-color="white">
                {{ member.name.charAt(0).toUpperCase() }}
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-medium">{{ member.name }}</q-item-label>
              <q-item-label caption v-if="member.is_owner">Trip Owner</q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-icon name="star" color="amber" v-if="member.is_owner" />
              <q-btn
                v-else
                flat
                round
                dense
                icon="remove_circle_outline"
                color="negative"
                size="sm"
                @click.stop="confirmRemoveMember(member)"
              />
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

        <div v-if="loadingActivity" class="text-center q-pa-xl">
          <q-spinner color="primary" size="md" />
        </div>

        <q-timeline v-else color="primary">
          <q-timeline-entry
            v-for="entry in activityLog"
            :key="entry.id"
            :title="entry.description"
            :subtitle="formatDate(entry.created_at)"
            :icon="getActivityIcon(entry.action_type)"
          />
          <!-- Always show trip creation at the bottom -->
          <q-timeline-entry
            title="Trip Created"
            :subtitle="formatDate(trip?.created_at || '')"
            icon="add_circle"
          >
            <div>{{ trip?.name }} was created</div>
          </q-timeline-entry>
        </q-timeline>
      </q-tab-panel>
    </q-tab-panels>

    <!-- Settings Modal -->
    <q-dialog v-model="showSettings" persistent>
      <q-card style="width: 100%; max-width: 450px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Trip Settings</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section class="q-gutter-y-md q-pt-md">
          <q-input
            v-model="editForm.name"
            label="Trip Name"
            outlined
            dense
            :rules="[(val: string) => !!val || 'Required']"
          >
            <template v-slot:prepend><q-icon name="flight_takeoff" /></template>
          </q-input>

          <q-input
            v-model="editForm.description"
            label="Description (optional)"
            outlined
            dense
            type="textarea"
            rows="2"
          />

          <div class="row q-gutter-sm">
            <q-input
              v-model="editForm.start_date"
              label="Start Date"
              mask="date"
              :rules="['date']"
              outlined
              dense
              class="col"
            >
              <template v-slot:append>
                <q-icon name="event" class="cursor-pointer">
                  <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                    <q-date v-model="editForm.start_date" />
                  </q-popup-proxy>
                </q-icon>
              </template>
            </q-input>
            <q-input
              v-model="editForm.end_date"
              label="End Date"
              mask="date"
              :rules="['date']"
              outlined
              dense
              class="col"
            >
              <template v-slot:append>
                <q-icon name="event" class="cursor-pointer">
                  <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                    <q-date v-model="editForm.end_date" :options="(d: string) => d >= editForm.start_date" />
                  </q-popup-proxy>
                </q-icon>
              </template>
            </q-input>
          </div>
        </q-card-section>

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn
            unelevated
            color="primary"
            label="Save Changes"
            :loading="savingTrip"
            @click="saveTrip"
          />
        </q-card-actions>

        <q-separator />

        <q-card-actions class="q-pa-md">
          <q-btn
            flat
            color="negative"
            label="Delete Trip"
            icon="delete"
            @click="confirmDeleteTrip"
          />
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
          <q-btn flat label="Add" color="primary" @click="addMember" :disable="!newMemberName" />
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
import type { TripMember } from 'src/types/expense';
import SettlementView from 'src/components/settlement/SettlementView.vue';
import ItineraryTab from 'src/components/itinerary/ItineraryTab.vue';
import type { ItineraryItem } from 'src/components/itinerary/itinerary.model';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();

// State
const tab = ref('itinerary');
const loading = ref(true);
const loadingExpenses = ref(false);
const loadingActivity = ref(false);
const savingTrip = ref(false);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const showSettings = ref(false);
const addMemberDialog = ref(false);
const newMemberName = ref('');
const tripId = ref(route.params.tripId as string);

interface ItineraryEvent {
  id: string;
  trip_id: string;
  event_date: string;
  event_time?: string;
  title: string;
  location?: string;
  description?: string;
  icon?: string;
  display_order: number;
  created_at: string;
}

interface Expense {
  id: string;
  description: string;
  amount: number;
  category: string;
  date: string;
  paid_by_id: string;
  split_type?: string;
  splits: { member_id: string; share_amount: number }[];
}

interface ActivityEntry {
  id: string;
  action_type: string;
  description: string;
  created_at: string;
}

const itineraryItems = ref<ItineraryItem[]>([]);
const expenses = ref<Expense[]>([]);
const activityLog = ref<ActivityEntry[]>([]);

// Edit form for settings modal
const editForm = ref({
  name: '',
  description: '',
  start_date: '',
  end_date: '',
});

// Expenses formatted for settlement calculator
const expensesWithSplits = computed(() => expenses.value);

const totalExpenses = computed(() =>
  expenses.value.reduce((sum, e) => sum + e.amount, 0)
);

const currentUserId = ref<string | undefined>(undefined);

const currentMemberId = computed(
  () => members.value.find((m: TripMember) => m.user_id === currentUserId.value)?.id,
);

// Helpers
function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function formatAmount(amount: number, currency = 'PHP'): string {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency }).format(amount);
}

function getMemberName(memberId: string): string {
  return members.value.find((m) => m.id === memberId)?.name || 'Unknown';
}

function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    'Food & Drinks': 'restaurant',
    Accommodation: 'hotel',
    Transportation: 'commute',
    Activities: 'attractions',
    Groceries: 'local_grocery_store',
    Other: 'more_horiz',
  };
  return icons[category] || 'receipt';
}

function getActivityIcon(actionType: string): string {
  const icons: Record<string, string> = {
    expense_added: 'add_circle',
    expense_updated: 'edit',
    expense_deleted: 'delete',
    member_joined: 'person_add',
    member_left: 'person_remove',
    settlement_created: 'account_balance_wallet',
    settlement_paid: 'check_circle',
    settlement_verified: 'verified',
    trip_created: 'flight_takeoff',
    trip_updated: 'edit',
    trip_completed: 'check_circle',
  };
  return icons[actionType] || 'circle';
}

// Navigation
function goToAddExpense(): void {
  void router.push(`/trips/${tripId.value}/expenses/new`);
}

function goToEditExpense(expenseId: string): void {
  void router.push(`/trips/${tripId.value}/expenses/${expenseId}`);
}

// Data fetching
async function fetchTripData(): Promise<void> {
  loading.value = true;
  const id = tripId.value;
  if (!id) { loading.value = false; return; }

  try {
    // 0. Current user
    const { data: { user: authUser } } = await supabase.auth.getUser();
    currentUserId.value = authUser?.id;

    // 1. Trip
    const { data: tripData, error: tripError } = await supabase
      .from('trips').select('*').eq('id', id).single();
    if (tripError || !tripData) throw new Error('Trip not found or access denied.');
    trip.value = { ...tripData, currencyCode: tripData.currency_code } as Trip;

    // 2. Members
    const { data: memberData, error: memberError } = await supabase
      .from('members').select('*').eq('trip_id', id).order('name');
    if (memberError || !memberData) throw new Error('Could not load trip members.');
    members.value = memberData as TripMember[];

    // 3. Itinerary events
    const { data: itineraryData, error: itineraryError } = await supabase
      .from('itinerary_events').select('*').eq('trip_id', id)
      .order('event_date', { ascending: true })
      .order('display_order', { ascending: true });

    if (!itineraryError && itineraryData) {
      itineraryItems.value = (itineraryData as ItineraryEvent[]).map((event) => ({
        id: event.id,
        phase: 'on' as const,
        title: event.title,
        type: 'text' as const,
        date: event.event_date,
        ...(event.event_time && { time: event.event_time }),
        ...(event.location && { location: event.location }),
        ...(event.description && { notes: event.description }),
      }));
    }
  } catch (error) {
    const msg = error instanceof Error ? error.message : 'Error fetching trip data.';
    console.error('Error fetching trip data:', error);
    $q.notify({ type: 'negative', message: msg });
    trip.value = null;
    members.value = [];
  } finally {
    loading.value = false;
  }
}

async function fetchExpenses(): Promise<void> {
  loadingExpenses.value = true;
  try {
    const { data, error } = await supabase
      .from('expenses')
      .select('id, description, amount, category, date, paid_by_id, split_type, expense_splits(member_id, share_amount)')
      .eq('trip_id', tripId.value)
      .order('date', { ascending: false });

    if (error) throw error;
    expenses.value = (data || []).map((e) => ({
      id: e.id,
      description: e.description,
      amount: e.amount,
      category: e.category,
      date: e.date,
      paid_by_id: e.paid_by_id,
      split_type: e.split_type,
      splits: (e.expense_splits || []) as { member_id: string; share_amount: number }[],
    }));
  } catch (error) {
    console.error('Error fetching expenses:', error);
  } finally {
    loadingExpenses.value = false;
  }
}

async function fetchActivity(): Promise<void> {
  loadingActivity.value = true;
  try {
    const { data, error } = await supabase
      .from('activity_log')
      .select('id, action_type, description, created_at')
      .eq('trip_id', tripId.value)
      .order('created_at', { ascending: false })
      .limit(30);

    if (!error && data) activityLog.value = data as ActivityEntry[];
  } catch (error) {
    console.error('Error fetching activity:', error);
  } finally {
    loadingActivity.value = false;
  }
}

// Member management
async function addMember(): Promise<void> {
  if (!newMemberName.value.trim()) return;
  try {
    const { error } = await supabase.from('members').insert({
      trip_id: tripId.value,
      name: newMemberName.value.trim(),
      is_owner: false,
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

function confirmRemoveMember(member: TripMember): void {
  $q.dialog({
    title: 'Remove Member',
    message: `Remove ${member.name} from this trip?`,
    cancel: true,
    persistent: true,
    color: 'negative',
  }).onOk(() => void removeMember(member.id));
}

async function removeMember(memberId: string): Promise<void> {
  try {
    const { error } = await supabase.from('members').delete().eq('id', memberId);
    if (error) throw error;
    $q.notify({ type: 'positive', message: 'Member removed.' });
    await fetchTripData();
  } catch (error) {
    console.error('Error removing member:', error);
    $q.notify({ type: 'negative', message: 'Failed to remove member' });
  }
}

// Trip settings
function openSettingsModal(): void {
  if (trip.value) {
    editForm.value = {
      name: trip.value.name,
      description: trip.value.description || '',
      start_date: trip.value.start_date,
      end_date: trip.value.end_date,
    };
  }
  showSettings.value = true;
}

async function saveTrip(): Promise<void> {
  if (!editForm.value.name.trim()) {
    $q.notify({ type: 'warning', message: 'Trip name is required.' });
    return;
  }
  savingTrip.value = true;
  try {
    const { error } = await supabase
      .from('trips')
      .update({
        name: editForm.value.name.trim(),
        description: editForm.value.description || null,
        start_date: editForm.value.start_date,
        end_date: editForm.value.end_date,
      })
      .eq('id', tripId.value);
    if (error) throw error;
    $q.notify({ type: 'positive', message: 'Trip updated!' });
    showSettings.value = false;
    await fetchTripData();
  } catch (error) {
    console.error('Error saving trip:', error);
    $q.notify({ type: 'negative', message: 'Failed to save trip.' });
  } finally {
    savingTrip.value = false;
  }
}

function confirmDeleteTrip(): void {
  $q.dialog({
    title: 'Delete Trip',
    message: `Permanently delete "${trip.value?.name}"? This cannot be undone.`,
    cancel: true,
    persistent: true,
    color: 'negative',
  }).onOk(() => void deleteTrip());
}

async function deleteTrip(): Promise<void> {
  try {
    const { error } = await supabase.from('trips').delete().eq('id', tripId.value);
    if (error) throw error;
    $q.notify({ type: 'positive', message: 'Trip deleted.' });
    showSettings.value = false;
    void router.replace('/trips');
  } catch (error) {
    console.error('Error deleting trip:', error);
    $q.notify({ type: 'negative', message: 'Failed to delete trip.' });
  }
}

function handleBack(): void {
  void router.back();
}

// Lifecycle
onMounted(async () => {
  await fetchTripData();
  void fetchExpenses();
  void fetchActivity();
});
</script>

<style scoped>
.q-page {
  min-height: 100vh;
}
</style>
