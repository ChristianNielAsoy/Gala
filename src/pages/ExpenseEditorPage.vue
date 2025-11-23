<template>
  <q-page>
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round icon="arrow_back" @click="router.back()" aria-label="Cancel" />
        <q-toolbar-title>
          {{ isEdit ? 'Edit Expense' : 'Add New Expense' }}
        </q-toolbar-title>
        <q-btn flat label="Save" @click="handleSave" :loading="saving" :disable="!isValid" />
      </q-toolbar>
    </q-header>

    <div class="q-pa-md">
      <q-form @submit.prevent="handleSave" class="q-gutter-y-lg">
        <!-- Section 1: Basic Expense Details -->
        <q-card flat bordered class="shadow-2">
          <q-card-section>
            <div class="text-h6 q-pb-sm">What was it for?</div>

            <q-input
              v-model="expenseForm.description"
              label="Description (e.g., Dinner at Bali, Fuel for rental)"
              :rules="[val => !!val || 'Description is required']"
              outlined
              dense
              class="q-mb-md"
            />

            <q-input
              v-model.number="expenseForm.amount"
              label="Amount"
              type="number"
              prefix="₱"
              :placeholder="trip?.currency_code || 'PHP'"
              :rules="[val => val > 0 || 'Amount must be greater than zero']"
              outlined
              dense
              input-class="text-right text-h6 text-primary"
              class="q-mb-md"
            />

            <q-select
              v-model="expenseForm.category"
              :options="categoryOptions"
              label="Category"
              outlined
              dense
              emit-value
              map-options
              class="q-mb-md"
            >
              <template v-slot:prepend>
                <q-icon name="list" />
              </template>
            </q-select>

            <q-input
              v-model="expenseForm.date"
              label="Date"
              mask="date"
              :rules="['date']"
              outlined
              dense
            >
              <template v-slot:append>
                <q-icon name="event" class="cursor-pointer">
                  <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                    <q-date v-model="expenseForm.date" />
                  </q-popup-proxy>
                </q-icon>
              </template>
            </q-input>

          </q-card-section>
        </q-card>

        <!-- Section 2: Who Paid? -->
        <q-card flat bordered class="shadow-2">
          <q-card-section>
            <div class="text-h6 q-pb-sm">Who paid?</div>
            <q-select
              v-model="expenseForm.paid_by_id"
              :options="memberOptions"
              label="Paid By"
              outlined
              dense
              emit-value
              map-options
              :rules="[val => !!val || 'Payer is required']"
            >
              <template v-slot:prepend>
                <q-icon name="payments" />
              </template>
            </q-select>
          </q-card-section>
        </q-card>

        <!-- Section 3: Who's Involved and How to Split? -->
        <q-card flat bordered class="shadow-2">
          <q-card-section>
            <div class="text-h6 q-pb-sm">Who's involved?</div>

            <!-- Involved Members (Only members checked are included in split) -->
            <q-option-group
              :options="memberCheckOptions"
              type="checkbox"
              v-model="involvedMembers"
              :rules="[val => val.length > 0 || 'At least one member must be involved']"
              class="q-mb-md"
            />

            <q-separator class="q-my-md" />

            <!-- Split Type Selector -->
            <div class="text-h6 q-pb-sm">How to split?</div>
            <q-option-group
              v-model="expenseForm.split_type"
              :options="splitTypeOptions"
              color="primary"
              inline
            />

            <!-- Split Details (Only visible for Custom split) -->
            <div v-if="expenseForm.split_type === 'custom'" class="q-mt-md q-pa-sm bg-blue-1 rounded-borders">
                <div class="text-subtitle1 text-primary q-mb-sm">Custom Split Amount ({{ trip?.currency_code || 'PHP' }})</div>
                <div v-for="memberId in involvedMembers" :key="memberId" class="row items-center q-mb-sm">
                    <div class="col-6 text-weight-medium">
                        {{ memberIdToName(memberId) }}
                    </div>
                    <div class="col-6">
                        <q-input
                            v-model.number="customSplits[memberId]"
                            type="number"
                            dense
                            outlined
                            :prefix="trip?.currency_code || 'PHP'"
                            :rules="[val => val >= 0 || 'Must be positive']"
                            input-class="text-right"
                            @update:model-value="recalculateTotal"
                        />
                    </div>
                </div>
                <q-separator class="q-my-sm" />
                <div class="row q-pt-sm">
                    <div class="col-6 text-weight-bold">Total Assigned:</div>
                    <div class="col-6 text-right text-weight-bold" :class="{'text-negative': splitDifference !== 0, 'text-positive': splitDifference === 0}">
                        {{ (customTotal).toFixed(2) }}
                    </div>
                </div>
                <div class="text-caption text-negative text-right" v-if="splitDifference !== 0">
                    Difference: {{ (splitDifference).toFixed(2) }}
                </div>
            </div>
          </q-card-section>
        </q-card>

      </q-form>

      <!-- Delete Button (Only for Edit Mode) -->
      <q-btn
        v-if="isEdit"
        label="Delete Expense"
        color="negative"
        flat
        class="full-width q-mt-lg"
        @click="deleteExpense"
      />

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { Trip } from 'src/types/trip';
import { Expense, TripMember, SplitType, ExpenseSplit } from 'src/types/expense'; // Use the new expense types

const route = useRoute();
const router = useRouter();
const $q = useQuasar();

// --- STATE ---
const tripId = ref(route.params.tripId as string);
const expenseId = ref(route.params.expenseId as string | undefined);
const isEdit = computed(() => !!expenseId.value);

const saving = ref(false);
const loading = ref(true);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);

// Form Data Model
interface ExpenseForm {
  description: string;
  amount: number | null;
  paid_by_id: string; // member_id
  category: string;
  date: string;
  split_type: SplitType;
}

// Form state
const expenseForm = ref<ExpenseForm>({
  description: '',
  amount: null,
  paid_by_id: '',
  category: 'Food',
  date: new Date().toISOString().split('T')[0], // Today's date
  split_type: 'equal',
});

const involvedMembers = ref<string[]>([]); // Array of member_ids involved in the split
const customSplits = ref<Record<string, number>>({}); // { member_id: amount }

// --- CONSTANTS ---

const categoryOptions = [
  { label: 'Food & Drinks', value: 'Food', icon: 'restaurant' },
  { label: 'Accommodation', value: 'Lodging', icon: 'hotel' },
  { label: 'Transportation', value: 'Transport', icon: 'commute' },
  { label: 'Activities', value: 'Activity', icon: 'attractions' },
  { label: 'Groceries', value: 'Groceries', icon: 'local_grocery_store' },
  { label: 'Other', value: 'Other', icon: 'more_horiz' },
];

const splitTypeOptions = [
  { label: 'Equally', value: 'equal' },
  { label: 'Custom Amounts', value: 'custom' },
  // { label: 'Percentages', value: 'percentages' }, // Future extension
];

// --- COMPUTED / UI HELPERS ---

// Map members to Quasar Select options for the 'Paid By' dropdown
const memberOptions = computed(() => {
  return members.value.map(m => ({
    label: m.name,
    value: m.id,
  }));
});

// Map members to Quasar Checkbox options for the 'Involved Members' list
const memberCheckOptions = computed(() => {
  return members.value.map(m => ({
    label: m.name,
    value: m.id,
  }));
});

// Used for Custom Split validation
const customTotal = computed(() => {
    return involvedMembers.value.reduce((sum, memberId) => sum + (customSplits.value[memberId] || 0), 0);
});

const splitDifference = computed(() => {
    return (expenseForm.amount || 0) - customTotal.value;
});

// Simple validation check before saving
const isValid = computed(() => {
  return (
    !!expenseForm.value.description &&
    (expenseForm.value.amount || 0) > 0 &&
    !!expenseForm.value.paid_by_id &&
    involvedMembers.value.length > 0 &&
    (expenseForm.value.split_type !== 'custom' || splitDifference.value === 0)
  );
});

// Helper to convert member ID back to name for display
function memberIdToName(id: string): string {
    return members.value.find(m => m.id === id)?.name || 'Unknown Member';
}

function recalculateTotal() {
    // Force computed property to re-evaluate and update the difference display
    // This function is mainly a placeholder to trigger Vue reactivity
}

// --- DATA FETCHING ---

async function fetchTripData() {
  loading.value = true;

  // 1. Fetch Trip Details
  const { data: tripData, error: tripError } = await supabase
    .from('trips')
    .select('*')
    .eq('id', tripId.value)
    .single();

  if (tripError || !tripData) {
    console.error('Error fetching trip:', tripError);
    $q.notify({ type: 'negative', message: 'Could not load trip context.' });
    loading.value = false;
    return;
  }
  trip.value = tripData as Trip;

  // 2. Fetch Trip Members (We need these for splitting)
  // NOTE: This assumes a 'members' table exists linked to 'trips'
  const { data: memberData, error: memberError } = await supabase
    .from('members')
    .select('*')
    .eq('trip_id', tripId.value)
    .order('name');

  if (memberError || !memberData) {
    console.error('Error fetching members:', memberError);
    // Continue even if members fail, but warn the user
    $q.notify({ type: 'warning', message: 'Could not load trip members.' });
  } else {
    members.value = memberData as TripMember[];
  }

  // Set default payer to the current user's member ID (if available)
  const currentUserId = supabase.auth.currentUser?.id;
  const currentUserMember = members.value.find(m => m.user_id === currentUserId);

  if (currentUserMember) {
    expenseForm.value.paid_by_id = currentUserMember.id;
    // Default: everyone is involved
    involvedMembers.value = members.value.map(m => m.id);
  }

  // 3. If editing, fetch existing expense data
  if (isEdit.value && expenseId.value) {
      // Logic to fetch existing expense and split data
      // For simplicity, we skip fetching splits for now and assume new expense creation
      // In a real app, this would involve complex logic to load the Expense and ExpenseSplits.
      $q.notify({ type: 'info', message: 'Edit mode enabled, but existing expense data loading is a complex future step.' });
  }

  loading.value = false;
}

// --- CORE LOGIC ---

/**
 * Calculates the split amounts based on the selected split type and involved members.
 */
function calculateSplits(): ExpenseSplit[] {
    const totalAmount = expenseForm.value.amount || 0;
    const splits: ExpenseSplit[] = [];

    if (expenseForm.value.split_type === 'equal') {
        const shareCount = involvedMembers.value.length;
        if (shareCount === 0) return [];

        // Split equally, handle remaining cents on the first member
        let baseShare = Math.floor((totalAmount * 100) / shareCount) / 100;
        let remainder = totalAmount - (baseShare * shareCount);

        involvedMembers.value.forEach((memberId, index) => {
            let shareAmount = baseShare;
            // Add the remainder (cents) to the first member's share
            if (index === 0) {
                shareAmount = parseFloat((shareAmount + remainder).toFixed(2));
            }

            splits.push({
                expense_id: 'TEMP_ID', // Will be replaced on insert
                member_id: memberId,
                share_amount: shareAmount,
            });
        });

    } else if (expenseForm.value.split_type === 'custom') {
        // Use the amounts specified in the customSplits ref
        involvedMembers.value.forEach((memberId) => {
            const amount = customSplits.value[memberId] || 0;
            splits.push({
                expense_id: 'TEMP_ID',
                member_id: memberId,
                share_amount: amount,
            });
        });
    }

    // Filter out members with a zero share if using custom/percentages (optional, but good practice)
    return splits.filter(s => s.share_amount > 0);
}

/**
 * Handles saving the new or edited expense and its associated splits.
 */
async function handleSave() {
  if (!isValid.value) {
    $q.notify({ type: 'warning', message: 'Please ensure all fields are valid and the custom split totals correctly.' });
    return;
  }

  saving.value = true;
  const splits = calculateSplits();

  // 1. Prepare Expense Data
  const newExpense: Partial<Expense> = {
    trip_id: tripId.value,
    paid_by_id: expenseForm.value.paid_by_id,
    description: expenseForm.value.description,
    amount: expenseForm.value.amount!,
    currency_code: trip.value?.currency_code || 'PHP', // Use trip currency
    category: expenseForm.value.category,
    date: expenseForm.value.date,
  };

  try {
    // 2. Insert the Expense
    const { data: expenseInsertData, error: expenseError } = await supabase
      .from('expenses')
      .insert(newExpense)
      .select('id')
      .single(); // Get the ID of the new expense

    if (expenseError || !expenseInsertData) throw expenseError;

    const newExpenseId = expenseInsertData.id;

    // 3. Prepare Splits Data
    const splitsToInsert = splits.map(s => ({
        ...s,
        expense_id: newExpenseId,
    }));

    // 4. Insert Splits
    const { error: splitError } = await supabase
        .from('expense_splits')
        .insert(splitsToInsert);

    if (splitError) throw splitError;

    // 5. Success
    $q.notify({ type: 'positive', message: 'Expense saved successfully!' });
    router.back(); // Go back to the Trip Details Page

  } catch (error: any) {
    console.error('Error saving expense:', error);
    $q.notify({ type: 'negative', message: 'Failed to save expense: ' + (error.message || 'Unknown error') });
  } finally {
    saving.value = false;
  }
}

/**
 * Handles deleting an existing expense.
 */
async function deleteExpense() {
    if (!expenseId.value) return;

    $q.dialog({
        title: 'Confirm Delete',
        message: 'Are you sure you want to delete this expense? This action cannot be undone.',
        cancel: true,
        persistent: true,
        color: 'negative'
    }).onOk(async () => {
        saving.value = true;
        try {
            // RLS should handle cascading deletes for splits
            const { error } = await supabase
                .from('expenses')
                .delete()
                .eq('id', expenseId.value);

            if (error) throw error;

            $q.notify({ type: 'positive', message: 'Expense deleted.' });
            router.back();
        } catch (error: any) {
            console.error('Error deleting expense:', error);
            $q.notify({ type: 'negative', message: 'Failed to delete expense: ' + (error.message || 'Unknown error') });
        } finally {
            saving.value = false;
        }
    });
}

// --- LIFECYCLE ---

onMounted(() => {
  if (!tripId.value) {
    $q.notify({ type: 'negative', message: 'Missing Trip ID. Cannot open expense editor.' });
    router.back();
    return;
  }
  fetchTripData();
});
</script>

<style scoped>
.q-page {
  min-height: 100vh;
}
</style>
