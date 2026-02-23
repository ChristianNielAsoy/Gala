<template>
  <q-page class="bg-surface">
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round icon="arrow_back" @click="handleBack" aria-label="Cancel" />
        <q-toolbar-title>
          {{ isEdit ? 'Edit Expense' : 'Add New Expense' }}
          <q-badge v-if="hasDraft" color="orange" class="q-ml-sm" label="Draft" />
        </q-toolbar-title>
        <q-btn
          v-if="hasDraft"
          flat
          round
          icon="delete"
          color="orange"
          @click="clearDraft"
          hint="Clear draft"
        />
        <q-btn flat label="Save" @click="handleSave" :loading="saving" :disable="!isValid" />
      </q-toolbar>
    </q-header>

    <div class="">
      <q-form @submit.prevent="handleSave" class="q-gutter-y-md">
        <!-- Section 1: Basic Details -->
        <q-card flat bordered class="shadow-2">
          <q-card-section>
            <div class="text-h6 q-pb-sm">Basic Details</div>

            <q-input
              v-model="expenseForm.description"
              label="Description"
              placeholder="e.g., Dinner at Bali, Fuel for rental"
              :rules="[(val: string) => !!val || 'Description is required']"
              outlined
              dense
              class="q-mb-md"
            />

            <q-input
              v-model.number="expenseForm.amount"
              label="Total Amount"
              type="number"
              step="0.01"
              :prefix="trip?.currency_code || 'PHP'"
              :rules="[(val: number) => val > 0 || 'Amount must be greater than zero']"
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
              :rules="[(val: string) => !!val || 'Payer is required']"
            >
              <template v-slot:prepend>
                <q-icon name="payments" />
              </template>
            </q-select>
          </q-card-section>
        </q-card>

        <!-- Section 3: Split Type -->
        <q-card flat bordered class="shadow-2">
          <q-card-section>
            <div class="text-h6 q-pb-sm">How to split?</div>
            <q-option-group
              v-model="splitMode"
              :options="splitModeOptions"
              color="primary"
              class="q-mb-md"
            />


            <!-- ITEM-BASED SPLITTING -->
            <div v-if="splitMode === 'itemized'" class="q-mt-md">
              <div class="row items-center justify-between q-mb-sm">
                <div class="text-subtitle2 text-weight-medium">Items</div>
                <q-btn
                  flat
                  dense
                  color="primary"
                  icon="add"
                  label="Add Item"
                  size="sm"
                  @click="addItem"
                />
              </div>

              <!-- Item Cards -->
              <div v-for="(item, idx) in items" :key="idx" class="q-mb-md">
                <q-card flat bordered class="bg-surface">
                  <q-card-section class="q-pa-sm">
                    <div class="row items-center q-gutter-sm">
                      <q-input
                        v-model="item.name"
                        placeholder="Item name"
                        outlined
                        dense
                        class="col"
                        :rules="[(val: string) => !!val || 'Required']"
                      />
                      <q-input
                        v-model.number="item.amount"
                        type="number"
                        step="0.01"
                        :prefix="trip?.currency_code || 'PHP'"
                        outlined
                        dense
                        style="max-width: 120px"
                        :rules="[(val: number) => val > 0 || 'Required']"
                      />
                      <q-btn
                        flat
                        round
                        dense
                        icon="delete"
                        color="negative"
                        size="sm"
                        @click="removeItem(idx)"
                      />
                    </div>

                    <!-- Is this "libre" (free)? -->
                    <q-checkbox
                      v-model="item.isLibre"
                      label="This is libre (no split needed)"
                      dense
                      class="q-mt-xs"
                    />

                    <!-- Participant Selection for this item -->
                    <div v-if="!item.isLibre" class="q-mt-sm">
                      <div class="text-caption text-grey-7 q-mb-xs">Who's sharing this?</div>
                      <div class="row q-gutter-xs">
                        <q-chip
                          v-for="member in members"
                          :key="member.id"
                          :selected="item.participants.includes(member.id)"
                          clickable
                          @click="toggleItemParticipant(idx, member.id)"
                          :color="item.participants.includes(member.id) ? 'primary' : 'grey-3'"
                          :text-color="item.participants.includes(member.id) ? 'white' : 'grey-8'"
                          size="sm"
                        >
                          {{ member.name }}
                        </q-chip>
                      </div>
                    </div>
                  </q-card-section>
                </q-card>
              </div>

              <!-- Items Total vs Expense Total Validation -->
              <q-banner v-if="itemsTotalMismatch" class="bg-warning text-white" rounded>
                <template v-slot:avatar>
                  <q-icon name="warning" />
                </template>
                Items total ({{ itemsTotal.toFixed(2) }}) doesn't match expense amount ({{
                  expenseForm.amount?.toFixed(2) || '0.00'
                }})
              </q-banner>
            </div>

            <!-- EQUAL SPLIT MODE -->
            <div v-else-if="splitMode === 'equal'" class="q-mt-md">
              <div class="text-subtitle2 text-weight-medium q-mb-sm">Who's involved?</div>
              <q-option-group
                :options="memberCheckOptions"
                type="checkbox"
                v-model="involvedMembers"
                :rules="[(val: string[]) => val.length > 0 || 'Select at least one']"
              />
            </div>

            <!-- GIFTED / LIBRE MODE -->
            <div v-else-if="splitMode === 'gifted'" class="q-mt-md">
              <q-banner class="bg-purple-1 text-purple-8 q-mb-md" rounded>
                <template v-slot:avatar>
                  <q-icon name="card_giftcard" color="purple" />
                </template>
                This is a free treat (libre) — selected members won't owe anything.
              </q-banner>
              <div class="text-subtitle2 text-weight-medium q-mb-sm">Who's receiving this gift?</div>
              <q-option-group
                :options="memberCheckOptions"
                type="checkbox"
                v-model="involvedMembers"
              />
            </div>

            <!-- EQUAL MEALS MODE -->
            <div v-else-if="splitMode === 'equalized_meals'" class="q-mt-md">
              <q-banner class="bg-blue-1 text-blue-8 q-mb-md" rounded>
                <template v-slot:avatar>
                  <q-icon name="restaurant" color="blue" />
                </template>
                Everyone orders separately, but the total is split equally.
              </q-banner>
              <div class="text-subtitle2 text-weight-medium q-mb-sm">Who's in?</div>
              <q-option-group
                :options="memberCheckOptions"
                type="checkbox"
                v-model="involvedMembers"
              />
              <!-- Optional: track individual orders for reference -->
              <div class="q-mt-md">
                <div class="row items-center justify-between q-mb-sm">
                  <div class="text-caption text-grey-7">Track individual orders (optional)</div>
                  <q-btn flat dense color="primary" icon="add" label="Add order" size="sm" @click="addItem" />
                </div>
                <div v-for="(item, idx) in items" :key="idx" class="q-mb-sm">
                  <q-card flat bordered class="bg-surface">
                    <q-card-section class="q-pa-sm">
                      <div class="row items-center q-gutter-sm">
                        <q-select
                          :model-value="item.participants[0]"
                          @update:model-value="(val) => { item.participants = val ? [val] : [] }"
                          :options="memberOptions"
                          emit-value
                          map-options
                          outlined
                          dense
                          class="col"
                          placeholder="Who ordered?"
                        />
                        <q-input
                          v-model="item.name"
                          placeholder="Item name"
                          outlined
                          dense
                          style="max-width: 130px"
                        />
                        <q-input
                          v-model.number="item.amount"
                          type="number"
                          step="0.01"
                          :prefix="trip?.currency_code || 'PHP'"
                          outlined
                          dense
                          style="max-width: 100px"
                        />
                        <q-btn flat round dense icon="delete" color="negative" size="sm" @click="removeItem(idx)" />
                      </div>
                    </q-card-section>
                  </q-card>
                </div>
              </div>
            </div>

            <!-- PERSONAL + SHARED MODE -->
            <div v-else-if="splitMode === 'individual_shared'" class="q-mt-md">
              <q-banner class="bg-teal-1 text-teal-8 q-mb-md" rounded>
                <template v-slot:avatar>
                  <q-icon name="people" color="teal" />
                </template>
                Tag items as <b>personal</b> (one person pays) or <b>shared</b> (split among all).
              </q-banner>
              <div class="row items-center justify-between q-mb-sm">
                <div class="text-subtitle2 text-weight-medium">Items</div>
                <q-btn flat dense color="primary" icon="add" label="Add Item" size="sm" @click="addIndividualSharedItem" />
              </div>
              <div v-for="(item, idx) in items" :key="idx" class="q-mb-md">
                <q-card flat bordered class="bg-surface">
                  <q-card-section class="q-pa-sm">
                    <div class="row items-center q-gutter-sm q-mb-sm">
                      <q-input
                        v-model="item.name"
                        placeholder="Item name"
                        outlined
                        dense
                        class="col"
                        :rules="[(val: string) => !!val || 'Required']"
                      />
                      <q-input
                        v-model.number="item.amount"
                        type="number"
                        step="0.01"
                        :prefix="trip?.currency_code || 'PHP'"
                        outlined
                        dense
                        style="max-width: 120px"
                        :rules="[(val: number) => val > 0 || 'Required']"
                      />
                      <q-btn flat round dense icon="delete" color="negative" size="sm" @click="removeItem(idx)" />
                    </div>
                    <!-- Individual vs Shared toggle -->
                    <q-btn-toggle
                      v-model="item.itemType"
                      :options="[{ label: 'Personal', value: 'individual', icon: 'person' }, { label: 'Shared', value: 'shared', icon: 'group' }]"
                      toggle-color="primary"
                      flat
                      dense
                      size="sm"
                      class="q-mb-sm"
                    />
                    <!-- For individual: select one person -->
                    <div v-if="item.itemType === 'individual'">
                      <q-select
                        :model-value="item.participants[0]"
                        @update:model-value="(val) => { item.participants = val ? [val] : [] }"
                        :options="memberOptions"
                        emit-value
                        map-options
                        label="Who pays for this?"
                        outlined
                        dense
                      />
                    </div>
                    <!-- For shared: select multiple people -->
                    <div v-else>
                      <div class="text-caption text-grey-7 q-mb-xs">Who's sharing this?</div>
                      <div class="row q-gutter-xs">
                        <q-chip
                          v-for="member in members"
                          :key="member.id"
                          :selected="item.participants.includes(member.id)"
                          clickable
                          @click="toggleItemParticipant(idx, member.id)"
                          :color="item.participants.includes(member.id) ? 'teal' : 'grey-3'"
                          :text-color="item.participants.includes(member.id) ? 'white' : 'grey-8'"
                          size="sm"
                        >
                          {{ member.name }}
                        </q-chip>
                      </div>
                    </div>
                  </q-card-section>
                </q-card>
              </div>
              <!-- Items total vs expense total -->
              <q-banner v-if="itemsTotalMismatch" class="bg-warning text-white" rounded>
                <template v-slot:avatar><q-icon name="warning" /></template>
                Items total ({{ itemsTotal.toFixed(2) }}) doesn't match expense amount ({{ expenseForm.amount?.toFixed(2) || '0.00' }})
              </q-banner>
            </div>

            <!-- CUSTOM SPLIT MODE -->
            <div v-else-if="splitMode === 'custom'" class="q-mt-md">
              <div class="text-subtitle2 text-weight-medium q-mb-sm">Custom amounts per person</div>

              <div
                v-for="memberId in involvedMembers"
                :key="memberId"
                class="row items-center q-mb-sm"
              >
                <div class="col-6 text-weight-medium">
                  {{ memberIdToName(memberId) }}
                </div>
                <div class="col-6">
                  <q-input
                    v-model.number="customSplits[memberId]"
                    type="number"
                    step="0.01"
                    dense
                    outlined
                    :prefix="trip?.currency_code || 'PHP'"
                    :rules="[(val: number) => val >= 0 || 'Must be positive']"
                    input-class="text-right"
                  />
                </div>
              </div>

              <q-separator class="q-my-sm" />

              <div class="row q-pt-sm">
                <div class="col-6 text-weight-bold">Total Assigned:</div>
                <div
                  class="col-6 text-right text-weight-bold"
                  :class="{
                    'text-negative': splitDifference !== 0,
                    'text-positive': splitDifference === 0,
                  }"
                >
                  {{ customTotal.toFixed(2) }}
                </div>
              </div>

              <div class="text-caption text-negative text-right" v-if="splitDifference !== 0">
                Difference: {{ splitDifference.toFixed(2) }}
              </div>
            </div>
          </q-card-section>
        </q-card>

        <!-- Section 4: Receipt Upload -->
        <q-card flat bordered class="shadow-2">
          <q-card-section>
            <div class="text-h6 q-pb-sm">Receipt (Optional)</div>
            <q-file
              v-model="receiptFile"
              label="Attach receipt"
              outlined
              dense
              accept="image/*"
              max-file-size="5242880"
              @rejected="onFileRejected"
            >
              <template v-slot:prepend>
                <q-icon name="receipt" />
              </template>
              <template v-slot:append>
                <q-icon name="attach_file" />
              </template>
            </q-file>
          </q-card-section>
        </q-card>
      </q-form>

      <!-- Delete Button (Edit Mode Only) -->
      <q-btn
        v-if="isEdit"
        label="Delete Expense"
        color="negative"
        flat
        class="full-width q-mt-lg"
        @click="handleDeleteExpense"
      />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { logActivity, getCurrentUserId } from 'src/utils/activityLogger';
import { formatCurrency } from 'src/utils/settlementCalculator';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';
import type { TripMember, SplitType } from 'src/types/expense';


const route = useRoute();
const router = useRouter();
const $q = useQuasar();

// Route params
const tripId = ref(route.params.tripId as string);
const expenseId = ref(route.params.expenseId as string | undefined);
const isEdit = computed(() => !!expenseId.value && expenseId.value !== 'new');

// State
const saving = ref(false);
const loading = ref(true);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const receiptFile = ref<File | null>(null);
const existingReceiptUrl = ref<string | null>(null);

// Form model
interface ExpenseForm {
  description: string;
  amount: number | null;
  paid_by_id: string;
  category: string;
  date: string;
  split_type: SplitType;
}

const expenseForm = ref<ExpenseForm>({
  description: '',
  amount: null,
  paid_by_id: '',
  category: 'Food & Drinks',
  date: new Date().toISOString().split('T')[0] ?? '',
  split_type: 'equal',
});

// Split modes
type SplitMode = 'equal' | 'custom' | 'itemized' | 'gifted' | 'equalized_meals' | 'individual_shared';
const splitMode = ref<SplitMode>(expenseForm.value.split_type as SplitMode);
const involvedMembers = ref<string[]>([]);
const customSplits = ref<Record<string, number>>({});

// Sync splitMode with expenseForm.split_type
watch(splitMode, (newMode) => {
  expenseForm.value.split_type = newMode;

  // Initialize data when switching modes
  if (newMode === 'itemized' && items.value.length === 0) {
    // Initialize with one empty item
    addItem();
  }

  if (['equal', 'custom', 'gifted', 'equalized_meals'].includes(newMode)) {
    // Set involved members to all members by default
    involvedMembers.value = members.value.map((m: TripMember) => m.id);
  }
});

watch(
  () => expenseForm.value.split_type,
  (newType) => {
    splitMode.value = newType as SplitMode;
  },
);

// Item-based splitting
interface ExpenseItem {
  name: string;
  amount: number;
  isLibre: boolean;
  participants: string[];
  itemType?: 'individual' | 'shared'; // Used by individual_shared mode
}

const items = ref<ExpenseItem[]>([]);

// Draft persistence key
const draftKey = `expense_draft_${tripId.value}_${expenseId.value || 'new'}`;

// Check if draft exists
const hasDraft = ref(false);

// Load draft data from Supabase
async function loadDraft() {
  try {
    const userId = await getCurrentUserId();
    if (!userId) return;

    const { data, error } = await supabase
      .from('expense_drafts')
      .select('expense_data')
      .eq('trip_id', tripId.value)
      .eq('user_id', userId)
      .eq('draft_key', draftKey)
      .single();

    if (error && error.code !== 'PGRST116') {
      // PGRST116 is "not found"
      console.warn('Failed to load draft:', error);
      return;
    }

    if (data?.expense_data) {
      const draftData = data.expense_data;

      // Restore form data
      if (draftData.expenseForm) {
        Object.assign(expenseForm.value, draftData.expenseForm);
      }

      // Restore split mode
      if (draftData.splitMode) {
        splitMode.value = draftData.splitMode;
      }

      // Restore items
      if (draftData.items) {
        items.value = draftData.items;
      }

      // Restore other data
      if (draftData.involvedMembers) {
        involvedMembers.value = draftData.involvedMembers;
      }

      if (draftData.customSplits) {
        customSplits.value = draftData.customSplits;
      }
    }
  } catch (error) {
    console.warn('Failed to load draft:', error);
  }
}

// Save draft data to Supabase
async function saveDraft() {
  try {
    const userId = await getCurrentUserId();
    if (!userId) return;

    const draftData = {
      expenseForm: expenseForm.value,
      splitMode: splitMode.value,
      items: items.value,
      involvedMembers: involvedMembers.value,
      customSplits: customSplits.value,
      timestamp: Date.now(),
    };

    const { error } = await supabase.from('expense_drafts').upsert({
      trip_id: tripId.value,
      user_id: userId,
      draft_key: draftKey,
      expense_data: draftData,
      updated_at: new Date().toISOString(),
    });

    if (error) {
      console.warn('Failed to save draft:', error);
    } else {
      hasDraft.value = true;
    }
  } catch (error) {
    console.warn('Failed to save draft:', error);
  }
}

// Clear draft data
async function clearDraft() {
  try {
    const userId = await getCurrentUserId();
    if (!userId) return;

    const { error } = await supabase
      .from('expense_drafts')
      .delete()
      .eq('trip_id', tripId.value)
      .eq('user_id', userId)
      .eq('draft_key', draftKey);

    if (error) {
      console.warn('Failed to clear draft:', error);
    } else {
      hasDraft.value = false;
      $q.notify({
        type: 'info',
        message: 'Draft cleared',
        icon: 'delete',
      });
    }
  } catch (error) {
    console.warn('Failed to clear draft:', error);
  }
}

// Check if draft exists in Supabase
async function checkDraftExists() {
  try {
    const userId = await getCurrentUserId();
    if (!userId) return;

    const { data, error } = await supabase
      .from('expense_drafts')
      .select('id')
      .eq('trip_id', tripId.value)
      .eq('user_id', userId)
      .eq('draft_key', draftKey)
      .single();

    hasDraft.value = !error && !!data;
  } catch (error) {
    console.warn('Failed to check draft existence:', error);
    hasDraft.value = false;
  }
}

// Auto-save draft when data changes
watch(
  [expenseForm, splitMode, items, involvedMembers, customSplits],
  () => {
    void saveDraft();
  },
  { deep: true },
);

// Options
const categoryOptions = ref<{ label: string; value: string; icon?: string }[]>([]);
const currencyOptions = ref<{ label: string; value: string }[]>([]);

const splitModeOptions = [
  { label: 'Equal Split', value: 'equal' },
  { label: 'Custom Amounts', value: 'custom' },
  { label: 'Item by Item', value: 'itemized' },
  { label: 'Gifted / Libre', value: 'gifted' },
  { label: 'Equal Meals', value: 'equalized_meals' },
  { label: 'Personal + Shared', value: 'individual_shared' },
];

// Computed
const memberOptions = computed(() =>
  members.value.map((m: TripMember) => ({ label: m.name, value: m.id })),
);

const memberCheckOptions = computed(() =>
  members.value.map((m: TripMember) => ({ label: m.name, value: m.id })),
);

const itemsTotal = computed(() => items.value.reduce((sum, item) => sum + (item.amount || 0), 0));

const itemsTotalMismatch = computed(
  () =>
    splitMode.value === 'itemized' &&
    expenseForm.value.amount !== null &&
    Math.abs(itemsTotal.value - expenseForm.value.amount) > 0.01,
);

const customTotal = computed(() =>
  involvedMembers.value.reduce(
    (sum: number, memberId: string) => sum + (customSplits.value[memberId] || 0),
    0,
  ),
);

const splitDifference = computed(() => (expenseForm.value.amount || 0) - customTotal.value);

const isValid = computed(() => {
  const basicValid =
    !!expenseForm.value.description &&
    (expenseForm.value.amount || 0) > 0 &&
    !!expenseForm.value.paid_by_id;

  if (!basicValid) return false;

  if (splitMode.value === 'equal') {
    return involvedMembers.value.length > 0;
  }

  if (splitMode.value === 'custom') {
    return involvedMembers.value.length > 0 && Math.abs(splitDifference.value) < 0.01;
  }

  if (splitMode.value === 'itemized') {
    return (
      items.value.length > 0 &&
      !itemsTotalMismatch.value &&
      items.value.every(
        (item) => item.name && item.amount > 0 && (item.isLibre || item.participants.length > 0),
      )
    );
  }

  if (splitMode.value === 'gifted') {
    return involvedMembers.value.length > 0;
  }

  if (splitMode.value === 'equalized_meals') {
    return involvedMembers.value.length > 0;
  }

  if (splitMode.value === 'individual_shared') {
    return (
      items.value.length > 0 &&
      !itemsTotalMismatch.value &&
      items.value.every((item) => item.name && item.amount > 0 && item.participants.length > 0)
    );
  }

  return false;
});

// Helper functions
function memberIdToName(id: string): string {
  return members.value.find((m: TripMember) => m.id === id)?.name || 'Unknown';
}

function addItem(): void {
  items.value.push({
    name: '',
    amount: 0,
    isLibre: false,
    participants: [...involvedMembers.value],
    itemType: 'shared',
  });
}

function addIndividualSharedItem(): void {
  items.value.push({
    name: '',
    amount: 0,
    isLibre: false,
    participants: [...involvedMembers.value],
    itemType: 'shared',
  });
}

function removeItem(index: number): void {
  items.value.splice(index, 1);
}

function toggleItemParticipant(itemIndex: number, memberId: string): void {
  const item = items.value[itemIndex];
  if (!item) return;

  const idx = item.participants.indexOf(memberId);
  if (idx > -1) {
    item.participants.splice(idx, 1);
  } else {
    item.participants.push(memberId);
  }
}

function onFileRejected(): void {
  $q.notify({ type: 'negative', message: 'File too large or invalid format' });
}

// Data fetching
async function fetchCategories(): Promise<void> {
  const { data, error } = await supabase
    .from('expense_categories')
    .select('name, icon')
    .eq('is_active', true)
    .order('display_order');

  if (error) {
    console.warn('Failed to load categories:', error);
    // Fallback to hardcoded categories
    categoryOptions.value = [
      { label: 'Food & Drinks', value: 'Food & Drinks', icon: 'restaurant' },
      { label: 'Accommodation', value: 'Accommodation', icon: 'hotel' },
      { label: 'Transportation', value: 'Transportation', icon: 'commute' },
      { label: 'Activities', value: 'Activities', icon: 'attractions' },
      { label: 'Groceries', value: 'Groceries', icon: 'local_grocery_store' },
      { label: 'Other', value: 'Other', icon: 'more_horiz' },
    ];
  } else {
    categoryOptions.value = data.map((cat) => ({
      label: cat.name,
      value: cat.name,
      icon: cat.icon || 'more_horiz',
    }));
  }
}

async function fetchCurrencies(): Promise<void> {
  const { data, error } = await supabase
    .from('currencies')
    .select('code, name, symbol')
    .eq('is_active', true)
    .order('display_order');

  if (error) {
    console.warn('Failed to load currencies:', error);
    // Fallback to hardcoded currencies
    currencyOptions.value = [
      { label: 'PHP - Philippine Peso', value: 'PHP' },
      { label: 'USD - US Dollar', value: 'USD' },
      { label: 'EUR - Euro', value: 'EUR' },
    ];
  } else {
    currencyOptions.value = data.map((curr) => ({
      label: `${curr.code} - ${curr.name}`,
      value: curr.code,
    }));
  }
}

async function fetchTripData(): Promise<void> {
  loading.value = true;

  // Load categories and currencies in parallel
  await Promise.all([fetchCategories(), fetchCurrencies()]);

  const { data: tripData, error: tripError } = await supabase
    .from('trips')
    .select('*')
    .eq('id', tripId.value)
    .single();

  if (tripError || !tripData) {
    $q.notify({ type: 'negative', message: 'Could not load trip context.' });
    loading.value = false;
    return;
  }
  trip.value = tripData as Trip;

  const { data: memberData, error: memberError } = await supabase
    .from('members')
    .select('*')
    .eq('trip_id', tripId.value)
    .order('name');

  if (memberError || !memberData) {
    $q.notify({ type: 'warning', message: 'Could not load trip members.' });
  } else {
    members.value = memberData as TripMember[];
  }

  // If editing, load existing expense data
  if (isEdit.value) {
    await fetchExpenseData();
  } else {
    // Set defaults for new expense
    const {
      data: { user },
    } = await supabase.auth.getUser();
    const currentUserMember = members.value.find((m: TripMember) => m.user_id === user?.id);

    if (currentUserMember) {
      expenseForm.value.paid_by_id = currentUserMember.id;
      involvedMembers.value = members.value.map((m: TripMember) => m.id);
    }

    // Load any existing draft for new expenses
    void loadDraft();
  }

  loading.value = false;
}

// Fetch existing expense data for editing
async function fetchExpenseData(): Promise<void> {
  if (!expenseId.value) return;

  const { data: expenseData, error: expenseError } = await supabase
    .from('expenses')
    .select('*')
    .eq('id', expenseId.value)
    .single();

  if (expenseError || !expenseData) {
    $q.notify({ type: 'negative', message: 'Could not load expense data.' });
    return;
  }

  // Populate form
  expenseForm.value = {
    description: expenseData.description,
    amount: expenseData.amount,
    paid_by_id: expenseData.paid_by_id,
    category: expenseData.category,
    date: expenseData.date,
    split_type: expenseData.split_type || 'equal',
  };

  // Set split mode
  splitMode.value = expenseData.split_type as SplitMode;

  // Store existing receipt URL for edit mode
  existingReceiptUrl.value = expenseData.receipt_url || null;

  // Load expense items if itemized, individual_shared, or equalized_meals
  if (['itemized', 'individual_shared', 'equalized_meals'].includes(expenseData.split_type)) {
    const { data: itemsData, error: itemsError } = await supabase
      .from('expense_items')
      .select('*')
      .eq('expense_id', expenseId.value)
      .order('display_order');

    if (!itemsError && itemsData) {
      items.value = itemsData.map((item) => ({
        name: item.item_name,
        amount: item.item_amount,
        isLibre: item.is_libre || false,
        participants: item.consumers || [],
        itemType: item.consumers?.length === 1 ? ('individual' as const) : ('shared' as const),
      }));
    }

    // If no items found, initialize with one empty item
    if (items.value.length === 0) {
      addItem();
    }
  }

  // Load expense splits to determine involved members
  const { data: splitsData, error: splitsError } = await supabase
    .from('expense_splits')
    .select('member_id, share_amount')
    .eq('expense_id', expenseId.value);

  if (!splitsError && splitsData) {
    involvedMembers.value = splitsData.map((split) => split.member_id);

    // For custom splits, populate customSplits
    if (expenseData.split_type === 'custom') {
      customSplits.value = {};
      splitsData.forEach((split) => {
        customSplits.value[split.member_id] = split.share_amount;
      });
    }
  }
}

// Save expense
async function handleSave() {
  if (!isValid.value) {
    $q.notify({
      type: 'warning',
      message: 'Please complete all required fields and ensure splits are valid.',
    });
    return;
  }

  saving.value = true;

  try {
    // 0. Upload receipt if a new file was selected
    let receipt_url: string | null = existingReceiptUrl.value;
    if (receiptFile.value) {
      const ext = receiptFile.value.name.split('.').pop() || 'jpg';
      const path = `${tripId.value}/${Date.now()}.${ext}`;
      const { data: uploadData, error: uploadError } = await supabase.storage
        .from('expense-receipts')
        .upload(path, receiptFile.value);
      if (uploadError) {
        $q.notify({
          type: 'warning',
          message: 'Receipt upload failed — expense will save without receipt.',
        });
      } else {
        const { data: urlData } = supabase.storage.from('expense-receipts').getPublicUrl(uploadData.path);
        receipt_url = urlData.publicUrl;
      }
    }

    // 1. Calculate splits based on split type
    let splits: { member_id: string; share_amount: number }[] = [];
    const total = expenseForm.value.amount!;

    if (splitMode.value === 'equal' || splitMode.value === 'equalized_meals') {
      const count = involvedMembers.value.length;
      const baseShare = Math.floor((total * 100) / count) / 100;
      const remainder = Math.round((total - baseShare * count) * 100) / 100;
      splits = involvedMembers.value.map((memberId, index) => ({
        member_id: memberId,
        share_amount: index === 0 ? baseShare + remainder : baseShare,
      }));
    } else if (splitMode.value === 'custom') {
      splits = involvedMembers.value
        .map((memberId) => ({
          member_id: memberId,
          share_amount: customSplits.value[memberId] || 0,
        }))
        .filter((s) => s.share_amount > 0);
    } else if (splitMode.value === 'itemized') {
      const itemSplits: Record<string, number> = {};
      items.value.forEach((item) => {
        if (!item.isLibre && item.participants.length > 0) {
          const sharePerPerson = item.amount / item.participants.length;
          item.participants.forEach((memberId) => {
            itemSplits[memberId] = (itemSplits[memberId] || 0) + sharePerPerson;
          });
        }
      });
      splits = Object.entries(itemSplits)
        .filter(([, amount]) => amount > 0)
        .map(([memberId, share_amount]) => ({
          member_id: memberId,
          share_amount: Math.round(share_amount * 100) / 100,
        }));
    } else if (splitMode.value === 'gifted') {
      // Payer covers full amount (their expense alone); recipients owe nothing
      splits = [{ member_id: expenseForm.value.paid_by_id, share_amount: total }];
      // Record recipients as 0-amount splits for display purposes
      involvedMembers.value.forEach((memberId) => {
        if (memberId !== expenseForm.value.paid_by_id) {
          splits.push({ member_id: memberId, share_amount: 0 });
        }
      });
    } else if (splitMode.value === 'individual_shared') {
      const memberShares: Record<string, number> = {};
      items.value.forEach((item) => {
        if (item.participants.length > 0) {
          if (item.itemType === 'individual') {
            // Assigned to one person
            const memberId = item.participants[0];
            if (memberId) memberShares[memberId] = (memberShares[memberId] || 0) + item.amount;
          } else {
            // Shared equally among participants
            const sharePerPerson = item.amount / item.participants.length;
            item.participants.forEach((memberId) => {
              memberShares[memberId] = (memberShares[memberId] || 0) + sharePerPerson;
            });
          }
        }
      });
      splits = Object.entries(memberShares)
        .filter(([, amount]) => amount > 0)
        .map(([memberId, share_amount]) => ({
          member_id: memberId,
          share_amount: Math.round(share_amount * 100) / 100,
        }));
    }

    // 2. Insert or Update the Expense record
    const expensePayload = {
      trip_id: tripId.value,
      paid_by_id: expenseForm.value.paid_by_id,
      description: expenseForm.value.description,
      amount: total,
      currency_code: trip.value?.currency_code || 'PHP',
      category: expenseForm.value.category,
      date: expenseForm.value.date,
      split_type: splitMode.value,
      receipt_url,
    };

    let savedExpenseId: string;

    if (isEdit.value && expenseId.value) {
      const { error: updateError } = await supabase
        .from('expenses')
        .update(expensePayload)
        .eq('id', expenseId.value);
      if (updateError) throw updateError;
      savedExpenseId = expenseId.value;

      // Delete existing splits and items before re-inserting
      await supabase.from('expense_splits').delete().eq('expense_id', savedExpenseId);
      if (['itemized', 'individual_shared', 'equalized_meals'].includes(splitMode.value)) {
        await supabase.from('expense_items').delete().eq('expense_id', savedExpenseId);
      }
    } else {
      const { data: newExpense, error: insertError } = await supabase
        .from('expenses')
        .insert(expensePayload)
        .select('id')
        .single();
      if (insertError || !newExpense) throw insertError;
      savedExpenseId = newExpense.id;
    }

    // 3. Insert Expense Items (itemized / individual_shared / equalized_meals)
    const saveItems = ['itemized', 'individual_shared', 'equalized_meals'].includes(splitMode.value);
    if (saveItems && items.value.length > 0) {
      const itemsToInsert = items.value.map((item, index) => ({
        expense_id: savedExpenseId,
        item_name: item.name,
        item_amount: item.amount,
        quantity: 1,
        display_order: index,
        consumers: item.isLibre ? [] : item.participants,
      }));
      const { error: itemsError } = await supabase.from('expense_items').insert(itemsToInsert);
      if (itemsError) throw itemsError;
    }

    // 4. Insert Splits
    if (splits.length > 0) {
      const splitsToInsert = splits.map((s) => ({
        expense_id: savedExpenseId,
        member_id: s.member_id,
        share_amount: s.share_amount,
      }));
      const { error: splitsError } = await supabase.from('expense_splits').insert(splitsToInsert);
      if (splitsError) throw splitsError;
    }

    // 5. Log activity
    const userId = await getCurrentUserId();
    await logActivity({
      trip_id: tripId.value,
      user_id: userId,
      action_type: isEdit.value ? 'expense_updated' : 'expense_added',
      entity_type: 'expense',
      entity_id: savedExpenseId,
      description: isEdit.value
        ? `Updated expense: ${expenseForm.value.description}`
        : `Added expense: ${expenseForm.value.description} (${formatCurrency(total, trip.value?.currency_code || 'PHP')})`,
      metadata: {
        amount: total,
        category: expenseForm.value.category,
        split_type: splitMode.value,
        item_count: saveItems ? items.value.length : undefined,
      },
    });

    $q.notify({ type: 'positive', message: 'Expense saved successfully!', icon: 'check_circle' });
    void clearDraft();
    router.back();
  } catch (error) {
    console.error('Error saving expense:', error);
    $q.notify({
      type: 'negative',
      message: error instanceof Error ? error.message : 'Failed to save expense',
    });
  } finally {
    saving.value = false;
  }
}

function handleDeleteExpense(): void {
  $q.dialog({
    title: 'Confirm Delete',
    message: 'Delete this expense permanently?',
    cancel: true,
    persistent: true,
    color: 'negative',
  }).onOk(() => void deleteExpense());
}

async function deleteExpense(): Promise<void> {
  saving.value = true;
  try {
    const { error } = await supabase.from('expenses').delete().eq('id', expenseId.value);

    if (error) throw error;

    $q.notify({ type: 'positive', message: 'Expense deleted.' });
    void router.back();
  } catch (error) {
    console.error('Error deleting expense:', error);
    $q.notify({ type: 'negative', message: 'Failed to delete expense.' });
  } finally {
    saving.value = false;
  }
}

function handleBack(): void {
  // Check if there are unsaved changes
  const hasChanges =
    items.value.length > 0 || expenseForm.value.description || expenseForm.value.amount;

  if (hasChanges) {
    $q.dialog({
      title: 'Unsaved Changes',
      message: 'You have unsaved changes. Do you want to save them as a draft for later?',
      cancel: {
        label: 'Discard',
        color: 'negative',
        flat: true,
      },
      ok: {
        label: 'Keep Draft',
        color: 'primary',
      },
      persistent: true,
    })
      .onOk(() => {
        // Keep draft (already auto-saved)
        router.back();
      })
      .onCancel(() => {
        // Discard draft
        void clearDraft();
        router.back();
      });
  } else {
    router.back();
  }
}

// Lifecycle
onMounted(async () => {
  if (!tripId.value) {
    $q.notify({ type: 'negative', message: 'Missing Trip ID.' });
    void router.back();
    return;
  }
  await fetchTripData();
  await checkDraftExists();
  if (hasDraft.value) {
    await loadDraft();
  }

  // Warn about unsaved changes when closing tab
  const handleBeforeUnload = (event: BeforeUnloadEvent) => {
    const hasChanges =
      items.value.length > 0 || expenseForm.value.description || expenseForm.value.amount;
    if (hasChanges) {
      event.preventDefault();
      event.returnValue = '';
    }
  };

  window.addEventListener('beforeunload', handleBeforeUnload);

  // Cleanup on unmount
  onUnmounted(() => {
    window.removeEventListener('beforeunload', handleBeforeUnload);
  });
});
</script>

<style scoped>
.q-page {
  min-height: 100vh;
}
</style>
