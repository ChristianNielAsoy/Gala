<template>
  <q-page class="bg-surface">
    <q-header class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round icon="arrow_back" @click="handleBack" aria-label="Cancel" />
        <q-toolbar-title>
          {{ isEdit ? 'Edit Expense' : 'Add New Expense' }}
          <q-badge v-if="hasDraft" color="warning" class="q-ml-sm" label="Draft" />
        </q-toolbar-title>
        <q-btn
          v-if="hasDraft"
          flat
          round
          icon="delete"
          color="warning"
          @click="clearDraft"
          aria-label="Clear draft"
        />
        <q-btn flat no-caps label="Save" @click="handleSave" :loading="saving" :disable="!isValid" />
      </q-toolbar>
    </q-header>

    <div class="q-pa-md">
      <q-form @submit.prevent="handleSave" class="q-gutter-y-md">
        <!-- Section 1: Basic Details -->
        <q-card flat bordered>
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
              v-model="amountExpression"
              label="Total Amount"
              :prefix="trip?.currency_code || 'PHP'"
              :hint="amountExpressionResult !== null ? `= ${(trip?.currency_code || 'PHP')} ${amountExpressionResult.toFixed(2)}` : 'Tip: type 120+45+30 to calculate'"
              :rules="[(val: string) => { const n = parseAmountExpression(val); return (n !== null && n > 0) || 'Enter a valid amount greater than zero'; }]"
              outlined
              dense
              inputmode="decimal"
              input-class="text-right text-h6 text-primary"
              class="q-mb-md"
              @blur="onAmountBlur"
              @keyup.enter="onAmountBlur"
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
        <q-card flat bordered>
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
        <q-card flat bordered>
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
        <q-card flat bordered>
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
        no-caps
        class="full-width q-mt-lg"
        @click="handleDeleteExpense"
      />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { logActivity } from 'src/utils/activityLogger';
import { formatCurrency } from 'src/utils/settlementCalculator';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import type { TripMember } from 'src/types/expense';
import { useExpenseSplitting, splitModeOptions } from 'src/composables/useExpenseSplitting';
import type { ExpenseForm } from 'src/composables/useExpenseSplitting';
import { useExpenseDraft } from 'src/composables/useExpenseDraft';
import { useExpenseData } from 'src/composables/useExpenseData';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();

// Route params
const tripId = ref(route.params.tripId as string);
const expenseId = ref(route.params.expenseId as string | undefined);
const isEdit = computed(() => !!expenseId.value && expenseId.value !== 'new');

// Page-level state
const saving = ref(false);
const receiptFile = ref<File | null>(null);
const existingReceiptUrl = ref<string | null>(null);

const expenseForm = ref<ExpenseForm>({
  description: '',
  amount: null,
  paid_by_id: '',
  category: 'Food & Drinks',
  date: new Date().toISOString().split('T')[0] ?? '',
  split_type: 'equal',
});

// Amount calculator
const amountExpression = ref('');

function parseAmountExpression(input: string): number | null {
  const cleaned = input.replace(/\s/g, '').replace(/,/g, '');
  if (!cleaned) return null;
  // Only allow digits, decimal point, and basic arithmetic operators/parens
  if (!/^[\d.+\-*/()]+$/.test(cleaned)) return null;
  if (!/^[\d(]/.test(cleaned)) return null;
  try {
    // eslint-disable-next-line no-new-func
    const result = new Function(`return +(${cleaned})`)() as number;
    if (!isFinite(result) || isNaN(result) || result < 0) return null;
    return Math.round(result * 100) / 100;
  } catch {
    return null;
  }
}

const amountExpressionResult = computed<number | null>(() => {
  if (!/[+\-*/]/.test(amountExpression.value)) return null;
  return parseAmountExpression(amountExpression.value);
});

function onAmountBlur(): void {
  const result = parseAmountExpression(amountExpression.value);
  if (result !== null) {
    expenseForm.value.amount = result;
    amountExpression.value = String(result);
  } else if (!amountExpression.value.trim()) {
    expenseForm.value.amount = null;
  }
}

// Sync amountExpression when amount is set externally (draft load, edit load)
watch(
  () => expenseForm.value.amount,
  (amount) => {
    if (amount != null && !amountExpression.value.match(/[+\-*/]/)) {
      amountExpression.value = String(amount);
    }
  },
);

// Composables
const expenseData = useExpenseData(tripId);
const splitting = useExpenseSplitting(expenseData.members, expenseForm);
const userId = computed(() => authStore.user?.id);
const draft = useExpenseDraft(tripId, expenseId, userId, {
  expenseForm,
  splitMode: splitting.splitMode,
  items: splitting.items,
  involvedMembers: splitting.involvedMembers,
  customSplits: splitting.customSplits,
});

// Destructure for template
const { trip, members, loading, categoryOptions, memberOptions } = expenseData;
const {
  splitMode, involvedMembers, customSplits, items, isValid,
  itemsTotal, itemsTotalMismatch, customTotal, splitDifference,
  memberCheckOptions, addItem, addIndividualSharedItem, removeItem, toggleItemParticipant,
} = splitting;
const { hasDraft, clearDraft, checkDraftExists, loadDraft } = draft;

// Options
const currencyOptions = expenseData.currencyOptions;

function memberIdToName(id: string): string {
  return members.value.find((m: TripMember) => m.id === id)?.name || 'Unknown';
}

function onFileRejected(): void {
  $q.notify({ type: 'negative', message: 'File too large or invalid format' });
}

// Save expense
async function handleSave(): Promise<void> {
  if (!isValid.value) {
    $q.notify({ type: 'warning', message: 'Please complete all required fields and ensure splits are valid.' });
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
        $q.notify({ type: 'warning', message: 'Receipt upload failed — expense will save without receipt.' });
      } else {
        const { data: urlData } = supabase.storage.from('expense-receipts').getPublicUrl(uploadData.path);
        receipt_url = urlData.publicUrl;
      }
    }

    // 1. Calculate splits
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
        .map((memberId) => ({ member_id: memberId, share_amount: customSplits.value[memberId] || 0 }))
        .filter((s) => s.share_amount > 0);
    } else if (splitMode.value === 'itemized') {
      const itemSplits: Record<string, number> = {};
      items.value.forEach((item) => {
        if (!item.isLibre && item.participants.length > 0) {
          const sharePerPerson = item.amount / item.participants.length;
          item.participants.forEach((m) => { itemSplits[m] = (itemSplits[m] || 0) + sharePerPerson; });
        }
      });
      splits = Object.entries(itemSplits)
        .filter(([, amount]) => amount > 0)
        .map(([member_id, share_amount]) => ({ member_id, share_amount: Math.round(share_amount * 100) / 100 }));
    } else if (splitMode.value === 'gifted') {
      splits = [{ member_id: expenseForm.value.paid_by_id, share_amount: total }];
      involvedMembers.value.forEach((m) => {
        if (m !== expenseForm.value.paid_by_id) splits.push({ member_id: m, share_amount: 0 });
      });
    } else if (splitMode.value === 'individual_shared') {
      const memberShares: Record<string, number> = {};
      items.value.forEach((item) => {
        if (item.participants.length > 0) {
          if (item.itemType === 'individual') {
            const m = item.participants[0];
            if (m) memberShares[m] = (memberShares[m] || 0) + item.amount;
          } else {
            const sharePerPerson = item.amount / item.participants.length;
            item.participants.forEach((m) => { memberShares[m] = (memberShares[m] || 0) + sharePerPerson; });
          }
        }
      });
      splits = Object.entries(memberShares)
        .filter(([, amount]) => amount > 0)
        .map(([member_id, share_amount]) => ({ member_id, share_amount: Math.round(share_amount * 100) / 100 }));
    }

    // 2. Save expense record
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
      const { error } = await supabase.from('expenses').update(expensePayload).eq('id', expenseId.value);
      if (error) throw error;
      savedExpenseId = expenseId.value;
      await supabase.from('expense_splits').delete().eq('expense_id', savedExpenseId);
      if (['itemized', 'individual_shared', 'equalized_meals'].includes(splitMode.value)) {
        await supabase.from('expense_items').delete().eq('expense_id', savedExpenseId);
      }
    } else {
      const { data: newExpense, error } = await supabase.from('expenses').insert(expensePayload).select('id').single();
      if (error || !newExpense) throw error;
      savedExpenseId = newExpense.id;
    }

    // 3. Save items
    const saveItems = ['itemized', 'individual_shared', 'equalized_meals'].includes(splitMode.value);
    if (saveItems && items.value.length > 0) {
      const { error } = await supabase.from('expense_items').insert(
        items.value.map((item, index) => ({
          expense_id: savedExpenseId,
          item_name: item.name,
          item_amount: item.amount,
          quantity: 1,
          display_order: index,
          consumers: item.isLibre ? [] : item.participants,
        })),
      );
      if (error) throw error;
    }

    // 4. Save splits
    if (splits.length > 0) {
      const { error } = await supabase.from('expense_splits').insert(
        splits.map((s) => ({ expense_id: savedExpenseId, member_id: s.member_id, share_amount: s.share_amount })),
      );
      if (error) throw error;
    }

    // 5. Log activity
    await logActivity({
      trip_id: tripId.value,
      user_id: authStore.user?.id,
      action_type: isEdit.value ? 'expense_updated' : 'expense_added',
      entity_type: 'expense',
      entity_id: savedExpenseId,
      description: isEdit.value
        ? `Updated expense: ${expenseForm.value.description}`
        : `Added expense: ${expenseForm.value.description} (${formatCurrency(total, trip.value?.currency_code || 'PHP')})`,
      metadata: { amount: total, category: expenseForm.value.category, split_type: splitMode.value,
                  item_count: saveItems ? items.value.length : undefined },
    });

    $q.notify({ type: 'positive', message: 'Expense saved successfully!', icon: 'check_circle' });
    void clearDraft();
    router.back();
  } catch (error) {
    console.error('Error saving expense:', error);
    $q.notify({ type: 'negative', message: error instanceof Error ? error.message : 'Failed to save expense' });
  } finally {
    saving.value = false;
  }
}

function handleDeleteExpense(): void {
  $q.dialog({ title: 'Confirm Delete', message: 'Delete this expense permanently?', cancel: true, persistent: true, color: 'negative' })
    .onOk(() => void deleteExpense());
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
  const hasChanges = items.value.length > 0 || expenseForm.value.description || expenseForm.value.amount;
  if (hasChanges) {
    $q.dialog({
      title: 'Unsaved Changes',
      message: 'You have unsaved changes. Do you want to save them as a draft for later?',
      cancel: { label: 'Discard', color: 'negative', flat: true },
      ok: { label: 'Keep Draft', color: 'primary' },
      persistent: true,
    })
      .onOk(() => router.back())
      .onCancel(() => { void clearDraft(); router.back(); });
  } else {
    router.back();
  }
}

onMounted(async () => {
  if (!tripId.value) {
    $q.notify({ type: 'negative', message: 'Missing Trip ID.' });
    void router.back();
    return;
  }

  const ok = await expenseData.fetchTripData();
  if (!ok) return;

  if (isEdit.value && expenseId.value) {
    const result = await expenseData.fetchExpenseData(expenseId.value);
    if (result) {
      expenseForm.value = result.form;
      splitting.splitMode.value = result.splitMode;
      existingReceiptUrl.value = result.receiptUrl;
      splitting.items.value = result.items;
      splitting.involvedMembers.value = result.involvedMemberIds;
      splitting.customSplits.value = result.customSplitAmounts;
      if (
        splitting.items.value.length === 0 &&
        ['itemized', 'individual_shared', 'equalized_meals'].includes(result.splitMode)
      ) {
        splitting.addItem();
      }
    }
  } else {
    const currentUserMember = expenseData.members.value.find(
      (m: TripMember) => m.user_id === authStore.user?.id,
    );
    if (currentUserMember) {
      expenseForm.value.paid_by_id = currentUserMember.id;
      splitting.involvedMembers.value = expenseData.members.value.map((m: TripMember) => m.id);
    }
    await checkDraftExists();
    if (hasDraft.value) await loadDraft();
  }

  const handleBeforeUnload = (event: BeforeUnloadEvent) => {
    const hasChanges = items.value.length > 0 || expenseForm.value.description || expenseForm.value.amount;
    if (hasChanges) { event.preventDefault(); event.returnValue = ''; }
  };
  window.addEventListener('beforeunload', handleBeforeUnload);
  onUnmounted(() => window.removeEventListener('beforeunload', handleBeforeUnload));
});
</script>

<style scoped>
.q-page {
  min-height: 100vh;
}
</style>
