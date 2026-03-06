<template>
  <q-page class="editor-page gala-mesh-bg">

    <!-- ═══ Floating Header ═══ -->
    <q-header class="editor-header">
      <q-toolbar class="editor-toolbar">
        <q-btn flat round icon="arrow_back" @click="handleBack" aria-label="Cancel" color="on-background" />
        <q-toolbar-title class="editor-toolbar__title">
          {{ isEdit ? 'Edit Expense' : 'New Expense' }}
          <q-badge v-if="hasDraft" color="warning" class="q-ml-sm" label="Draft" />
        </q-toolbar-title>
        <q-btn
          v-if="hasDraft"
          flat round icon="delete_outline" color="warning"
          @click="clearDraft" aria-label="Clear draft"
        />
        <q-btn
          unelevated no-caps label="Save"
          color="primary"
          @click="handleSave"
          :loading="saving"
          :disable="!isValid"
          style="border-radius: 999px; padding: 0 20px; height: 36px;"
        />
      </q-toolbar>
    </q-header>

    <div class="editor-body">
      <q-form @submit.prevent="handleSave">

        <!-- ─── Section 1: Basic Details ───────────────────────────────────── -->
        <div class="editor-section-label">Details</div>
        <div class="editor-panel q-mb-lg">

          <!-- Description -->
          <q-input
            v-model="expenseForm.description"
            placeholder="What was this for?"
            :rules="[(val: string) => !!val || 'Description is required']"
            borderless
            dense
            class="editor-field editor-field--description"
            input-class="editor-input editor-input--description"
          />

          <div class="editor-divider" />

          <!-- Amount -->
          <div class="editor-amount-row">
            <span class="editor-amount-currency">{{ trip?.currency_code || 'PHP' }}</span>
            <q-input
              v-model="amountExpression"
              placeholder="0.00"
              :rules="[(val: string) => { const n = parseAmountExpression(val); return (n !== null && n > 0) || 'Enter a valid amount'; }]"
              borderless
              dense
              inputmode="decimal"
              class="editor-field editor-field--amount"
              input-class="editor-input editor-input--amount"
              @blur="onAmountBlur"
              @keyup.enter="onAmountBlur"
            />
            <div v-if="amountExpressionResult !== null" class="editor-amount-hint">
              = {{ trip?.currency_code || 'PHP' }} {{ amountExpressionResult.toFixed(2) }}
            </div>
          </div>

          <div class="editor-divider" />

          <!-- Category chips -->
          <div class="editor-field-label q-mt-sm q-mb-xs">Category</div>
          <div class="cat-chips">
            <button
              v-for="opt in categoryOptions"
              :key="opt.value"
              type="button"
              class="cat-chip"
              :class="expenseForm.category === opt.value ? 'cat-chip--active' : ''"
              @click="expenseForm.category = opt.value"
            >
              {{ opt.label }}
            </button>
          </div>

          <div class="editor-divider q-mt-sm" />

          <!-- Date -->
          <div class="editor-field-label q-mt-sm">Date</div>
          <q-input
            v-model="expenseForm.date"
            mask="date"
            :rules="['date']"
            borderless
            dense
            class="editor-field"
            input-class="editor-input"
          >
            <template v-slot:append>
              <q-icon name="calendar_today" size="18px" class="cursor-pointer text-muted">
                <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                  <q-date v-model="expenseForm.date" />
                </q-popup-proxy>
              </q-icon>
            </template>
          </q-input>
        </div>

        <!-- ─── Section 2: Who Paid? ───────────────────────────────────────── -->
        <div class="editor-section-label">Who paid?</div>
        <div class="editor-panel q-mb-lg">
          <q-select
            v-model="expenseForm.paid_by_id"
            :options="memberOptions"
            label="Paid By"
            outlined
            dense
            emit-value
            map-options
            :rules="[(val: string) => !!val || 'Payer is required']"
            class="editor-select"
          >
            <template v-slot:prepend>
              <q-icon name="payments" color="primary" size="20px" />
            </template>
          </q-select>
        </div>

        <!-- ─── Section 3: Split Type ──────────────────────────────────────── -->
        <div class="editor-section-label">How to split?</div>
        <div class="editor-panel q-mb-lg">
          <!-- Split mode pill tabs -->
          <div class="split-tabs q-mb-md">
            <button
              v-for="opt in splitModeOptions"
              :key="opt.value"
              type="button"
              class="split-tab"
              :class="splitMode === opt.value ? 'split-tab--active' : ''"
              @click="splitMode = opt.value as SplitMode"
            >{{ opt.label }}</button>
          </div>


          <!-- ITEM-BASED SPLITTING -->
          <div v-if="splitMode === 'itemized'" class="q-mt-sm">
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
              <div v-for="(item, idx) in items" :key="idx" class="item-card q-mb-sm">
                <div class="item-card__row">
                  <q-input
                    v-model="item.name"
                    placeholder="Item name"
                    outlined dense class="col"
                    :rules="[(val: string) => !!val || 'Required']"
                  />
                  <q-input
                    v-model.number="item.amount"
                    type="number" step="0.01"
                    :prefix="trip?.currency_code || 'PHP'"
                    outlined dense style="max-width: 110px"
                    :rules="[(val: number) => val > 0 || 'Required']"
                  />
                  <q-btn flat round dense icon="delete_outline" color="negative" size="sm" @click="removeItem(idx)" />
                </div>
                <q-checkbox v-model="item.isLibre" label="Libre (no split)" dense class="q-mt-xs" />
                <div v-if="!item.isLibre" class="q-mt-xs">
                  <div class="editor-field-label q-mb-xs">Who's sharing?</div>
                  <div class="member-chips">
                    <q-chip
                      v-for="member in members" :key="member.id"
                      :selected="item.participants.includes(member.id)"
                      clickable
                      @click="toggleItemParticipant(idx, member.id)"
                      :color="item.participants.includes(member.id) ? 'primary' : 'grey-3'"
                      :text-color="item.participants.includes(member.id) ? 'white' : 'grey-8'"
                      size="sm"
                    >{{ member.name }}</q-chip>
                  </div>
                </div>
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
          <div v-else-if="splitMode === 'equal'" class="q-mt-sm">
            <div class="editor-field-label q-mb-xs">Who's involved?</div>
            <div class="member-chips">
              <q-chip
                v-for="opt in memberCheckOptions" :key="opt.value"
                :selected="involvedMembers.includes(opt.value)"
                clickable
                @click="involvedMembers.includes(opt.value)
                  ? involvedMembers.splice(involvedMembers.indexOf(opt.value), 1)
                  : involvedMembers.push(opt.value)"
                :color="involvedMembers.includes(opt.value) ? 'primary' : 'grey-3'"
                :text-color="involvedMembers.includes(opt.value) ? 'white' : 'grey-8'"
                size="sm"
              >{{ opt.label }}</q-chip>
            </div>
          </div>

          <!-- GIFTED / LIBRE MODE -->
          <div v-else-if="splitMode === 'gifted'" class="q-mt-sm">
            <div class="split-info-banner split-info-banner--purple q-mb-sm">
              <q-icon name="card_giftcard" size="16px" />
              This is libre — selected members won't owe anything.
            </div>
            <div class="editor-field-label q-mb-xs">Who's receiving this gift?</div>
            <div class="member-chips">
              <q-chip
                v-for="opt in memberCheckOptions" :key="opt.value"
                :selected="involvedMembers.includes(opt.value)"
                clickable
                @click="involvedMembers.includes(opt.value)
                  ? involvedMembers.splice(involvedMembers.indexOf(opt.value), 1)
                  : involvedMembers.push(opt.value)"
                :color="involvedMembers.includes(opt.value) ? 'primary' : 'grey-3'"
                :text-color="involvedMembers.includes(opt.value) ? 'white' : 'grey-8'"
                size="sm"
              >{{ opt.label }}</q-chip>
            </div>
          </div>

          <!-- EQUAL MEALS MODE -->
          <div v-else-if="splitMode === 'equalized_meals'" class="q-mt-sm">
            <div class="split-info-banner split-info-banner--blue q-mb-sm">
              <q-icon name="restaurant" size="16px" />
              Everyone orders separately, total split equally.
            </div>
            <div class="editor-field-label q-mb-xs">Who's in?</div>
            <div class="member-chips q-mb-md">
              <q-chip
                v-for="opt in memberCheckOptions" :key="opt.value"
                :selected="involvedMembers.includes(opt.value)"
                clickable
                @click="involvedMembers.includes(opt.value)
                  ? involvedMembers.splice(involvedMembers.indexOf(opt.value), 1)
                  : involvedMembers.push(opt.value)"
                :color="involvedMembers.includes(opt.value) ? 'primary' : 'grey-3'"
                :text-color="involvedMembers.includes(opt.value) ? 'white' : 'grey-8'"
                size="sm"
              >{{ opt.label }}</q-chip>
            </div>
            <div class="row items-center justify-between q-mb-xs">
              <div class="editor-field-label">Track individual orders (optional)</div>
              <q-btn flat dense color="primary" icon="add" label="Add order" size="sm" no-caps @click="addItem" />
            </div>
            <div v-for="(item, idx) in items" :key="idx" class="item-card q-mb-sm">
              <div class="item-card__row">
                <q-select
                  :model-value="item.participants[0]"
                  @update:model-value="(val) => { item.participants = val ? [val] : [] }"
                  :options="memberOptions" emit-value map-options outlined dense class="col" placeholder="Who ordered?"
                />
                <q-input v-model="item.name" placeholder="Item" outlined dense style="max-width: 130px" />
                <q-input v-model.number="item.amount" type="number" step="0.01" :prefix="trip?.currency_code || 'PHP'" outlined dense style="max-width: 100px" />
                <q-btn flat round dense icon="delete_outline" color="negative" size="sm" @click="removeItem(idx)" />
              </div>
            </div>
          </div>

          <!-- PERSONAL + SHARED MODE -->
          <div v-else-if="splitMode === 'individual_shared'" class="q-mt-sm">
            <div class="split-info-banner split-info-banner--teal q-mb-sm">
              <q-icon name="people" size="16px" />
              Tag items as <b>personal</b> (one person) or <b>shared</b> (everyone splits).
            </div>
            <div class="row items-center justify-between q-mb-xs">
              <div class="editor-field-label">Items</div>
              <q-btn flat dense color="primary" icon="add" label="Add Item" size="sm" no-caps @click="addIndividualSharedItem" />
            </div>
            <div v-for="(item, idx) in items" :key="idx" class="item-card q-mb-sm">
              <div class="item-card__row">
                <q-input v-model="item.name" placeholder="Item name" outlined dense class="col" :rules="[(val: string) => !!val || 'Required']" />
                <q-input v-model.number="item.amount" type="number" step="0.01" :prefix="trip?.currency_code || 'PHP'" outlined dense style="max-width: 110px" :rules="[(val: number) => val > 0 || 'Required']" />
                <q-btn flat round dense icon="delete_outline" color="negative" size="sm" @click="removeItem(idx)" />
              </div>
              <q-btn-toggle
                v-model="item.itemType"
                :options="[{ label: 'Personal', value: 'individual', icon: 'person' }, { label: 'Shared', value: 'shared', icon: 'group' }]"
                toggle-color="primary" flat dense size="sm" class="q-mt-xs q-mb-xs"
              />
              <div v-if="item.itemType === 'individual'">
                <q-select
                  :model-value="item.participants[0]"
                  @update:model-value="(val) => { item.participants = val ? [val] : [] }"
                  :options="memberOptions" emit-value map-options label="Who pays for this?" outlined dense
                />
              </div>
              <div v-else>
                <div class="editor-field-label q-mb-xs">Who's sharing this?</div>
                <div class="member-chips">
                  <q-chip
                    v-for="member in members" :key="member.id"
                    :selected="item.participants.includes(member.id)"
                    clickable @click="toggleItemParticipant(idx, member.id)"
                    :color="item.participants.includes(member.id) ? 'teal' : 'grey-3'"
                    :text-color="item.participants.includes(member.id) ? 'white' : 'grey-8'"
                    size="sm"
                  >{{ member.name }}</q-chip>
                </div>
              </div>
            </div>
            <q-banner v-if="itemsTotalMismatch" class="bg-warning text-white q-mt-sm" rounded>
              <template v-slot:avatar><q-icon name="warning" /></template>
              Items total ({{ itemsTotal.toFixed(2) }}) doesn't match expense amount ({{ expenseForm.amount?.toFixed(2) || '0.00' }})
            </q-banner>
          </div>

          <!-- CUSTOM SPLIT MODE -->
          <div v-else-if="splitMode === 'custom'" class="q-mt-sm">
            <div class="editor-field-label q-mb-sm">Custom amounts per person</div>
            <div v-for="memberId in involvedMembers" :key="memberId" class="custom-split-row q-mb-sm">
              <div class="custom-split-row__name">{{ memberIdToName(memberId) }}</div>
              <q-input
                v-model.number="customSplits[memberId]"
                type="number" step="0.01" dense outlined
                :prefix="trip?.currency_code || 'PHP'"
                :rules="[(val: number) => val >= 0 || 'Must be positive']"
                input-class="text-right"
                style="max-width: 140px"
              />
            </div>
            <div class="custom-split-total q-mt-sm">
              <span>Total Assigned</span>
              <span :class="splitDifference !== 0 ? 'text-negative' : 'text-positive'">
                {{ customTotal.toFixed(2) }}
              </span>
            </div>
            <div v-if="splitDifference !== 0" class="text-caption text-negative text-right">
              Difference: {{ splitDifference.toFixed(2) }}
            </div>
          </div>

        </div><!-- /editor-panel -->

        <!-- ─── Section 4: Receipt Upload ──────────────────────────────────── -->
        <div class="editor-section-label">Receipt</div>
        <div class="editor-panel q-mb-lg">
          <q-file
            v-model="receiptFile"
            label="Attach receipt (optional)"
            outlined dense accept="image/*" max-file-size="5242880"
            @rejected="onFileRejected"
          >
            <template v-slot:prepend><q-icon name="receipt_long" color="primary" /></template>
            <template v-slot:append><q-icon name="attach_file" /></template>
          </q-file>
        </div>
      </q-form>

      <!-- Delete Button (Edit Mode Only) -->
      <q-btn
        v-if="isEdit"
        label="Delete Expense"
        color="negative"
        flat no-caps
        class="full-width q-mt-sm q-mb-xl"
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
import { sendPushToTripMembers } from 'src/utils/notificationService';
import { formatCurrency } from 'src/utils/settlementCalculator';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import type { TripMember } from 'src/types/expense';
import { useExpenseSplitting, splitModeOptions } from 'src/composables/useExpenseSplitting';
import type { ExpenseForm, SplitMode } from 'src/composables/useExpenseSplitting';
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
  if (!/^[\d.+\-*/()]+$/.test(cleaned)) return null;
  if (!/^[\d(]/.test(cleaned)) return null;

  let pos = 0;

  function parseExpr(): number {
    let val = parseTerm();
    while (pos < cleaned.length && (cleaned[pos] === '+' || cleaned[pos] === '-')) {
      const op = cleaned[pos++];
      const right = parseTerm();
      val = op === '+' ? val + right : val - right;
    }
    return val;
  }

  function parseTerm(): number {
    let val = parseFactor();
    while (pos < cleaned.length && (cleaned[pos] === '*' || cleaned[pos] === '/')) {
      const op = cleaned[pos++];
      const right = parseFactor();
      val = op === '*' ? val * right : val / right;
    }
    return val;
  }

  function parseFactor(): number {
    if (cleaned[pos] === '(') {
      pos++;
      const val = parseExpr();
      pos++; // skip ')'
      return val;
    }
    let numStr = '';
    while (pos < cleaned.length && /[\d.]/.test(cleaned[pos] ?? '')) numStr += cleaned[pos++];
    return parseFloat(numStr);
  }

  try {
    const result = parseExpr();
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
const { trip, members, categoryOptions, memberOptions } = expenseData;
const {
  splitMode, involvedMembers, customSplits, items, isValid,
  itemsTotal, itemsTotalMismatch, customTotal, splitDifference,
  memberCheckOptions, addItem, addIndividualSharedItem, removeItem, toggleItemParticipant,
} = splitting;
const { hasDraft, clearDraft, checkDraftExists, loadDraft } = draft;

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
          is_libre: item.isLibre ?? false,
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

    // Notify trip members about new expense (best-effort, fire-and-forget)
    if (!isEdit.value) {
      void sendPushToTripMembers({
        trip_id: tripId.value,
        exclude_user_id: authStore.user?.id,
        title: 'New expense added',
        body: `${expenseForm.value.description} — ${formatCurrency(total, trip.value?.currency_code || 'PHP')}`,
        url: `/#/trips/${tripId.value}`,
      });
    }

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

<style scoped lang="scss">
.editor-page {
  min-height: 100vh;
  background-color: var(--surface);
}

// ─── Header ───────────────────────────────────────────────────────────────────
.editor-header {
  background: transparent !important;
  box-shadow: none !important;
}

.editor-toolbar {
  padding: 8px 12px;

  &__title {
    font-size: 1rem;
    font-weight: 700;
    color: var(--on-background);
  }
}

// ─── Body ─────────────────────────────────────────────────────────────────────
.editor-body {
  padding: 0 16px 80px;
  padding-top: 8px;
}

// ─── Section label ─────────────────────────────────────────────────────────────
.editor-section-label {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--muted);
  margin-bottom: 6px;
  margin-top: 4px;
}

// ─── Panel ────────────────────────────────────────────────────────────────────
.editor-panel {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 16px;
}

// ─── Field labels ─────────────────────────────────────────────────────────────
.editor-field-label {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--muted);
}

.editor-divider {
  height: 1px;
  background: var(--border);
  margin: 10px 0;
}

// ─── Description ──────────────────────────────────────────────────────────────
.editor-field--description {
  :deep(.q-field__control) { padding: 0; min-height: unset; }
  :deep(.q-field__bottom) { padding: 2px 0 0; }
}

.editor-input--description {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--on-background);
  &::placeholder { color: var(--muted); font-weight: 400; }
}

// ─── Amount ───────────────────────────────────────────────────────────────────
.editor-amount-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.editor-amount-currency {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--muted);
  flex-shrink: 0;
}

.editor-field--amount {
  flex: 1;
  :deep(.q-field__control) { padding: 0; min-height: unset; }
  :deep(.q-field__bottom) { display: none; }
}

.editor-input--amount {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2rem;
  color: $primary;
  font-variant-numeric: tabular-nums;
  &::placeholder { color: var(--border); }
}

.editor-amount-hint {
  font-size: 0.75rem;
  color: var(--muted);
  white-space: nowrap;
  flex-shrink: 0;
}

// ─── Category chips ────────────────────────────────────────────────────────────
.cat-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cat-chip {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1.5px solid var(--border);
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.15s;

  &--active {
    background: $primary;
    border-color: $primary;
    color: #fff;
  }

  &:hover:not(&--active) {
    border-color: $primary;
    color: $primary;
  }
}

// ─── Editor select ─────────────────────────────────────────────────────────────
.editor-select {
  :deep(.q-field__control) { border-radius: var(--gala-radius-md); }
}

// ─── Split tabs ────────────────────────────────────────────────────────────────
.split-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.split-tab {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 999px;
  border: 1.5px solid var(--border);
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.15s;

  &--active {
    background: $primary;
    border-color: $primary;
    color: #fff;
  }

  &:hover:not(&--active) {
    border-color: $primary;
    color: $primary;
  }
}

// ─── Info banners ──────────────────────────────────────────────────────────────
.split-info-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.8125rem;
  padding: 10px 12px;
  border-radius: var(--gala-radius-md);

  &--purple { background: rgba(139, 92, 246, 0.08); color: #7C3AED; }
  &--blue { background: rgba(14, 165, 233, 0.08); color: #0369A1; }
  &--teal { background: rgba(13, 148, 136, 0.08); color: #0D9488; }
}

// ─── Member chips ──────────────────────────────────────────────────────────────
.member-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

// ─── Item cards ────────────────────────────────────────────────────────────────
.item-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-md);
  padding: 10px 12px;

  &__row {
    display: flex;
    align-items: flex-start;
    gap: 8px;
  }
}

// ─── Custom split ──────────────────────────────────────────────────────────────
.custom-split-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  &__name {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--on-background);
    flex: 1;
  }
}

.custom-split-total {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
  font-weight: 700;
  padding: 8px 0;
  border-top: 1px solid var(--border);
  color: var(--on-background);
}
</style>
