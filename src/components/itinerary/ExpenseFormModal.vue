<template>
  <q-dialog v-model="showDialog" persistent>
    <q-card style="min-width: 400px; max-width: 90vw">
      <q-card-section>
        <div class="text-h6">{{ isEditing ? 'Edit' : 'Add' }} Expense</div>
      </q-card-section>

      <q-card-section class="q-pt-none">
        <q-form @submit="handleSave" class="q-gutter-y-md">
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
                :prefix="currencyCode || 'PHP'"
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
                inline
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
                    @click="addExpenseItem"
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
                          :prefix="currencyCode || 'PHP'"
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
                          @click="removeExpenseItem(idx)"
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
                            v-for="member in tripMembers"
                            :key="member.id"
                            :selected="item.participants.includes(member.id)"
                            clickable
                            @click="toggleExpenseParticipant(idx, member.id)"
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
              </div>
            </q-card-section>
          </q-card>

          <!-- GIFTED ITEM SPLITTING -->
          <q-card v-if="splitMode === 'gifted'" flat bordered class="shadow-2 q-mt-md">
            <q-card-section>
              <div class="text-h6 q-pb-sm">Gifted Item Setup</div>
              <q-select
                v-model="giftedItemGifter"
                :options="memberOptions"
                label="Who is gifting the shared item?"
                outlined
                dense
                emit-value
                map-options
                class="q-mb-md"
              />
              <div class="text-caption text-grey-7">
                The selected person will pay for the entire shared item. Others pay only their
                individual costs.
              </div>
            </q-card-section>
          </q-card>

          <!-- INDIVIDUAL + SHARED ITEMS SPLITTING -->
          <q-card v-if="splitMode === 'individual_shared'" flat bordered class="shadow-2 q-mt-md">
            <q-card-section>
              <div class="text-h6 q-pb-sm">Individual + Shared Items</div>

              <!-- Individual Items -->
              <div class="q-mb-md">
                <div class="row items-center justify-between q-mb-sm">
                  <div class="text-subtitle2 text-weight-medium">Individual Items</div>
                  <q-btn
                    flat
                    dense
                    color="primary"
                    icon="add"
                    label="Add Individual Item"
                    size="sm"
                    @click="addIndividualItem"
                  />
                </div>

                <div v-for="(item, idx) in individualItems" :key="idx" class="q-mb-md">
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
                          :prefix="currencyCode || 'PHP'"
                          outlined
                          dense
                          style="max-width: 120px"
                          :rules="[(val: number) => val > 0 || 'Required']"
                        />
                        <q-select
                          v-model="item.designatedTo"
                          :options="memberOptions"
                          label="Belongs to"
                          outlined
                          dense
                          emit-value
                          map-options
                          style="max-width: 150px"
                          :rules="[(val: string) => !!val || 'Required']"
                        />
                        <q-btn
                          flat
                          round
                          dense
                          icon="delete"
                          color="negative"
                          size="sm"
                          @click="removeIndividualItem(idx)"
                        />
                      </div>
                    </q-card-section>
                  </q-card>
                </div>
              </div>

              <!-- Shared Items -->
              <div>
                <div class="row items-center justify-between q-mb-sm">
                  <div class="text-subtitle2 text-weight-medium">Shared Items</div>
                  <q-btn
                    flat
                    dense
                    color="primary"
                    icon="add"
                    label="Add Shared Item"
                    size="sm"
                    @click="addSharedItem"
                  />
                </div>

                <div v-for="(item, idx) in sharedItems" :key="idx" class="q-mb-md">
                  <q-card flat bordered class="bg-surface">
                    <q-card-section class="q-pa-sm">
                      <div class="row items-center q-gutter-sm">
                        <q-input
                          v-model="item.name"
                          placeholder="Shared item name"
                          outlined
                          dense
                          class="col"
                          :rules="[(val: string) => !!val || 'Required']"
                        />
                        <q-input
                          v-model.number="item.amount"
                          type="number"
                          step="0.01"
                          :prefix="currencyCode || 'PHP'"
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
                          @click="removeSharedItem(idx)"
                        />
                      </div>
                      <div class="text-caption text-grey-7 q-mt-xs">
                        This item will be split equally among all participants.
                      </div>
                    </q-card-section>
                  </q-card>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- EQUALIZED MEALS + SHARED SPLITTING -->
          <q-card v-if="splitMode === 'equalized_meals'" flat bordered class="shadow-2 q-mt-md">
            <q-card-section>
              <div class="text-h6 q-pb-sm">Equalized Meals + Shared Items</div>
              <div class="text-caption text-grey-7 q-mb-md">
                Individual meal costs will be pooled and split equally. Shared items will also be
                split equally.
              </div>

              <!-- Individual Items -->
              <div class="q-mb-md">
                <div class="row items-center justify-between q-mb-sm">
                  <div class="text-subtitle2 text-weight-medium">Individual Meals</div>
                  <q-btn
                    flat
                    dense
                    color="primary"
                    icon="add"
                    label="Add Meal"
                    size="sm"
                    @click="addIndividualItem"
                  />
                </div>

                <div v-for="(item, idx) in individualItems" :key="idx" class="q-mb-md">
                  <q-card flat bordered class="bg-surface">
                    <q-card-section class="q-pa-sm">
                      <div class="row items-center q-gutter-sm">
                        <q-input
                          v-model="item.name"
                          placeholder="Meal name"
                          outlined
                          dense
                          class="col"
                          :rules="[(val: string) => !!val || 'Required']"
                        />
                        <q-input
                          v-model.number="item.amount"
                          type="number"
                          step="0.01"
                          :prefix="currencyCode || 'PHP'"
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
                          @click="removeIndividualItem(idx)"
                        />
                      </div>
                    </q-card-section>
                  </q-card>
                </div>
              </div>

              <!-- Shared Items -->
              <div>
                <div class="row items-center justify-between q-mb-sm">
                  <div class="text-subtitle2 text-weight-medium">Shared Items</div>
                  <q-btn
                    flat
                    dense
                    color="primary"
                    icon="add"
                    label="Add Shared Item"
                    size="sm"
                    @click="addSharedItem"
                  />
                </div>

                <div v-for="(item, idx) in sharedItems" :key="idx" class="q-mb-md">
                  <q-card flat bordered class="bg-surface">
                    <q-card-section class="q-pa-sm">
                      <div class="row items-center q-gutter-sm">
                        <q-input
                          v-model="item.name"
                          placeholder="Shared item name"
                          outlined
                          dense
                          class="col"
                          :rules="[(val: string) => !!val || 'Required']"
                        />
                        <q-input
                          v-model.number="item.amount"
                          type="number"
                          step="0.01"
                          :prefix="currencyCode || 'PHP'"
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
                          @click="removeSharedItem(idx)"
                        />
                      </div>
                    </q-card-section>
                  </q-card>
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
                outlined
                label="Upload receipt"
                accept="image/*,.pdf,.doc,.docx"
                @rejected="onFileRejected"
                class="q-mb-md"
              >
                <template v-slot:prepend>
                  <q-icon name="receipt" />
                </template>
              </q-file>
              <div v-if="expenseForm.receipt_url" class="text-caption text-grey-7">
                Current receipt: {{ expenseForm.receipt_url.split('/').pop() }}
              </div>
            </q-card-section>
          </q-card>
        </q-form>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="Cancel" color="grey-7" v-close-popup @click="handleCancel" />
        <q-btn
          flat
          :label="isEditing ? 'Update' : 'Add'"
          color="primary"
          @click="handleSave"
          :loading="saving"
          :disable="!isValid"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { useQuasar } from 'quasar';
import type { SplitType } from 'src/types/expense';

// Type for initial expense data
type InitialExpense = {
  description: string;
  amount: number | null;
  paid_by_id: string;
  category: string;
  date?: string;
  split_type: SplitType;
  receipt_url?: string;
  involved_members?: string[];
  custom_splits?: Record<string, number>;
  expense_items?: ExpenseItem[];
  individual_items?: ExpenseItem[];
  shared_items?: ExpenseItem[];
  gifted_item_gifter?: string;
};

// Props
const props = defineProps<{
  show: boolean;
  isEditing: boolean;
  tripMembers: { id: string; name: string; user_id?: string }[];
  currencyCode?: string;
  initialExpense?: {
    description: string;
    amount: number | null;
    paid_by_id: string;
    category: string;
    date?: string;
    split_type: SplitType;
    receipt_url?: string;
    involved_members?: string[];
    custom_splits?: Record<string, number>;
    expense_items?: ExpenseItem[];
    individual_items?: ExpenseItem[];
    shared_items?: ExpenseItem[];
    gifted_item_gifter?: string;
  };
}>();

// Emits
const emit = defineEmits<{
  'update:show': [value: boolean];
  save: [
    expenseData: {
      expenseForm: ExpenseForm;
      splitMode: SplitMode;
      involvedMembers: string[];
      customSplits: Record<string, number>;
      items: ExpenseItem[];
      individualItems: ExpenseItem[];
      sharedItems: ExpenseItem[];
      giftedItemGifter: string;
      receiptFile: File | null;
    },
  ];
  cancel: [];
}>();

const $q = useQuasar();

// Computed for v-model
const showDialog = computed({
  get: () => props.show,
  set: (value: boolean) => emit('update:show', value),
});

// Expense form data
interface ExpenseForm {
  description: string;
  amount: number | null;
  paid_by_id: string;
  category: string;
  date?: string;
  split_type: SplitType;
  receipt_url?: string;
}

const expenseForm = ref<ExpenseForm>({
  description: '',
  amount: null,
  paid_by_id: '',
  category: 'Food',
  split_type: 'equal',
});

// Split modes
type SplitMode =
  | 'equal'
  | 'custom'
  | 'itemized'
  | 'gifted'
  | 'individual_shared'
  | 'equalized_meals';
const splitMode = ref<SplitMode>('equal');
const involvedMembers = ref<string[]>([]);
const customSplits = ref<Record<string, number>>({});

// New split mode data
const giftedItemGifter = ref<string>('');
const individualItems = ref<ExpenseItem[]>([]);
const sharedItems = ref<ExpenseItem[]>([]);

// Item-based splitting
interface ExpenseItem {
  name: string;
  amount: number;
  isLibre: boolean;
  participants: string[];
  designatedTo?: string;
}

const items = ref<ExpenseItem[]>([]);

// Receipt file
const receiptFile = ref<File | null>(null);

// State
const saving = ref(false);

// Options
const categoryOptions = [
  { label: 'Food & Drinks', value: 'Food & Drinks', icon: 'restaurant' },
  { label: 'Accommodation', value: 'Accommodation', icon: 'hotel' },
  { label: 'Transportation', value: 'Transportation', icon: 'commute' },
  { label: 'Activities', value: 'Activities', icon: 'attractions' },
  { label: 'Groceries', value: 'Groceries', icon: 'local_grocery_store' },
  { label: 'Other', value: 'Other', icon: 'more_horiz' },
];

const splitModeOptions = [
  { label: 'Equal Split', value: 'equal' },
  { label: 'Custom Amounts', value: 'custom' },
  { label: 'Item by Item', value: 'itemized' },
  { label: 'Gifted Item', value: 'gifted' },
  { label: 'Individual + Shared', value: 'individual_shared' },
  { label: 'Equalized Meals + Shared', value: 'equalized_meals' },
];

// Computed
const memberOptions = computed(() =>
  props.tripMembers.map((m) => ({ label: m.name, value: m.id })),
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
    return involvedMembers.value.length > 0 && splitDifference.value === 0;
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
    return involvedMembers.value.length > 0 && !!giftedItemGifter.value;
  }

  if (splitMode.value === 'individual_shared') {
    return (
      involvedMembers.value.length > 0 &&
      (individualItems.value.length > 0 || sharedItems.value.length > 0)
    );
  }

  if (splitMode.value === 'equalized_meals') {
    return (
      involvedMembers.value.length > 0 &&
      (individualItems.value.length > 0 || sharedItems.value.length > 0)
    );
  }

  return false;
});

// Watchers
watch(splitMode, (newMode) => {
  expenseForm.value.split_type = newMode;

  // Initialize data when switching modes
  if (newMode === 'itemized' && items.value.length === 0) {
    addExpenseItem();
  }

  if (newMode === 'equal' || newMode === 'custom') {
    involvedMembers.value = props.tripMembers.map((m) => m.id);
  }

  if (newMode === 'gifted') {
    involvedMembers.value = props.tripMembers.map((m) => m.id);
    if (!giftedItemGifter.value && props.tripMembers.length > 0) {
      giftedItemGifter.value = props.tripMembers[0]!.id;
    }
  }

  if (newMode === 'individual_shared' || newMode === 'equalized_meals') {
    involvedMembers.value = props.tripMembers.map((m) => m.id);
    if (individualItems.value.length === 0 && sharedItems.value.length === 0) {
      addIndividualItem();
    }
  }
});

watch(
  () => expenseForm.value.split_type,
  (newType) => {
    splitMode.value = newType as SplitMode;
  },
);

// Watch for prop changes
watch(
  () => props.show,
  (newShow) => {
    if (newShow) {
      initializeForm();
    }
  },
);

watch(
  () => props.initialExpense,
  (newExpense) => {
    if (newExpense && props.show) {
      loadExpenseData(newExpense);
    }
  },
);

// Methods
function initializeForm() {
  if (props.initialExpense) {
    loadExpenseData(props.initialExpense);
  } else {
    // New expense
    expenseForm.value = {
      description: '',
      amount: null,
      paid_by_id: '',
      category: 'Food',
      date: new Date().toISOString().split('T')[0] ?? '',
      split_type: 'equal',
    };
    splitMode.value = 'equal';
    involvedMembers.value = props.tripMembers.map((m) => m.id);
    customSplits.value = {};
    items.value = [];
    giftedItemGifter.value = '';
    individualItems.value = [];
    sharedItems.value = [];
    receiptFile.value = null;
  }
}

function loadExpenseData(expense: InitialExpense) {
  expenseForm.value = {
    description: expense.description || '',
    amount: expense.amount || null,
    paid_by_id: expense.paid_by_id || '',
    category: expense.category || 'Food',
    date: expense.date || new Date().toISOString().split('T')[0]!,
    split_type: expense.split_type || 'equal',
  };
  splitMode.value = (expense.split_type as SplitMode) || 'equal';
  involvedMembers.value = expense.involved_members || [];
  customSplits.value = expense.custom_splits || {};
  items.value = expense.expense_items || [];
  giftedItemGifter.value = expense.gifted_item_gifter || '';
  individualItems.value = expense.individual_items || [];
  sharedItems.value = expense.shared_items || [];
  receiptFile.value = null;
}

function addExpenseItem(): void {
  items.value.push({
    name: '',
    amount: 0,
    isLibre: false,
    participants: [...involvedMembers.value],
  });
}

function removeExpenseItem(index: number): void {
  items.value.splice(index, 1);
}

function toggleExpenseParticipant(itemIndex: number, memberId: string): void {
  const item = items.value[itemIndex];
  if (!item) return;

  const idx = item.participants.indexOf(memberId);
  if (idx > -1) {
    item.participants.splice(idx, 1);
  } else {
    item.participants.push(memberId);
  }
}

function addIndividualItem(): void {
  individualItems.value.push({
    name: '',
    amount: 0,
    isLibre: false,
    participants: [],
    designatedTo: props.tripMembers[0]?.id || '',
  });
}

function removeIndividualItem(index: number): void {
  individualItems.value.splice(index, 1);
}

function addSharedItem(): void {
  sharedItems.value.push({
    name: '',
    amount: 0,
    isLibre: false,
    participants: [...involvedMembers.value],
  });
}

function removeSharedItem(index: number): void {
  sharedItems.value.splice(index, 1);
}

function onFileRejected(): void {
  $q.notify({ type: 'negative', message: 'File too large or invalid format' });
}

function handleSave() {
  if (!isValid.value) {
    $q.notify({
      type: 'warning',
      message: 'Please complete all required fields and ensure splits are valid.',
    });
    return;
  }

  saving.value = true;

  emit('save', {
    expenseForm: expenseForm.value,
    splitMode: splitMode.value,
    involvedMembers: involvedMembers.value,
    customSplits: customSplits.value,
    items: items.value,
    individualItems: individualItems.value,
    sharedItems: sharedItems.value,
    giftedItemGifter: giftedItemGifter.value,
    receiptFile: receiptFile.value,
  });

  saving.value = false;
}

function handleCancel() {
  emit('cancel');
  emit('update:show', false);
}
</script>

<style scoped>
/* Add any specific styles if needed */
</style>
