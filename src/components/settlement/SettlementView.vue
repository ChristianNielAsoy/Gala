<template>
  <div class="settlement-view">

    <!-- Loading State -->
    <div v-if="loading" class="flex flex-center q-py-xl">
      <q-spinner color="primary" size="lg" />
    </div>

    <div v-else class="settlement-content">

      <!-- ═══ Your Balance Panel ═══ -->
      <div v-if="currentMemberId" class="balance-panel q-mb-lg">
        <div class="balance-panel__eyebrow">Your Balance</div>
        <div class="balance-panel__amount" :class="yourBalance >= 0 ? 'balance-panel__amount--positive' : 'balance-panel__amount--negative'">
          <span v-if="yourBalance > 0">+</span>{{ currencyCode }} {{ Math.abs(yourBalance).toFixed(2) }}
        </div>
        <div class="balance-panel__status">
          <span v-if="yourBalance > 0">You are owed</span>
          <span v-else-if="yourBalance < 0">You owe</span>
          <span v-else>You're all settled up!</span>
        </div>

        <!-- Member balance pills -->
        <div class="balance-pills q-mt-md">
          <div
            v-for="balance in memberBalances"
            :key="balance.memberId"
            class="balance-pill"
            :class="balance.netBalance > 0 ? 'balance-pill--positive' : balance.netBalance < 0 ? 'balance-pill--negative' : 'balance-pill--neutral'"
          >
            <div class="balance-pill__avatar">{{ balance.memberName.charAt(0).toUpperCase() }}</div>
            <div class="balance-pill__info">
              <div class="balance-pill__name">{{ balance.memberName }}</div>
              <div class="balance-pill__amount">
                <span v-if="balance.netBalance > 0">+</span>{{ currencyCode }} {{ Math.abs(balance.netBalance).toFixed(2) }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ You Need to Pay ═══ -->
      <template v-if="settlementsToPay.length > 0">
        <div class="section-label q-mb-sm">You Need to Pay</div>
        <div class="settle-card settle-card--pay q-mb-sm" v-for="settlement in settlementsToPay" :key="settlement.to">
          <div class="settle-card__avatar settle-card__avatar--negative">{{ settlement.toName.charAt(0).toUpperCase() }}</div>
          <div class="settle-card__body">
            <div class="settle-card__name">{{ settlement.toName }}</div>
            <div class="settle-card__caption">Settle your share of expenses</div>
          </div>
          <div class="settle-card__right">
            <div class="settle-card__amount settle-card__amount--negative">{{ currencyCode }} {{ settlement.amount.toFixed(2) }}</div>
            <button class="settle-card__action" @click="markAsPaid(settlement)">Mark Paid</button>
          </div>
        </div>
      </template>

      <!-- ═══ Awaiting Confirmation ═══ -->
      <template v-if="pendingVerifications.length > 0">
        <div class="section-label q-mb-sm q-mt-lg">Awaiting Confirmation</div>
        <div class="settle-card settle-card--pending q-mb-sm" v-for="s in pendingVerifications" :key="s.id">
          <div class="settle-card__avatar settle-card__avatar--warning">{{ getMemberName(s.from_member_id).charAt(0).toUpperCase() }}</div>
          <div class="settle-card__body">
            <div class="settle-card__name">{{ getMemberName(s.from_member_id) }} paid you</div>
            <div class="settle-card__caption">{{ s.payment_method }}{{ s.notes ? ' · ' + s.notes : '' }}</div>
          </div>
          <div class="settle-card__right">
            <div class="settle-card__amount settle-card__amount--positive">{{ currencyCode }} {{ s.amount.toFixed(2) }}</div>
            <button class="settle-card__action settle-card__action--positive" @click="verifyPayment(s)">Confirm</button>
          </div>
        </div>
      </template>

      <!-- ═══ You Will Receive ═══ -->
      <template v-if="settlementsToReceive.length > 0">
        <div class="section-label q-mb-sm q-mt-lg">You Will Receive</div>
        <div class="settle-card q-mb-sm" v-for="settlement in settlementsToReceive" :key="settlement.from">
          <div class="settle-card__avatar settle-card__avatar--positive">{{ settlement.fromName.charAt(0).toUpperCase() }}</div>
          <div class="settle-card__body">
            <div class="settle-card__name">{{ settlement.fromName }}</div>
            <div class="settle-card__caption">Waiting for payment</div>
          </div>
          <div class="settle-card__right">
            <div class="settle-card__amount settle-card__amount--positive">{{ currencyCode }} {{ settlement.amount.toFixed(2) }}</div>
          </div>
        </div>
      </template>

      <!-- ═══ All Trip Settlements ═══ -->
      <template v-if="allSettlements.length > 0">
        <div class="section-label q-mb-sm q-mt-lg">All Settlements</div>
        <div class="flow-list">
          <div class="flow-item" v-for="(settlement, index) in allSettlements" :key="index">
            <div class="flow-item__member">
              <div class="flow-item__pip">{{ settlement.fromName.charAt(0) }}</div>
              <span class="flow-item__name">{{ settlement.fromName }}</span>
            </div>
            <div class="flow-item__arrow">
              <div class="flow-item__line" />
              <div class="flow-item__badge">{{ currencyCode }} {{ settlement.amount.toFixed(2) }}</div>
              <q-icon name="arrow_forward" size="14px" color="primary" />
            </div>
            <div class="flow-item__member flow-item__member--right">
              <div class="flow-item__pip flow-item__pip--teal">{{ settlement.toName.charAt(0) }}</div>
              <span class="flow-item__name">{{ settlement.toName }}</span>
            </div>
          </div>
        </div>
      </template>

      <!-- All Settled State -->
      <div v-if="allSettlements.length === 0" class="settled-state">
        <div class="settled-state__icon">
          <q-icon name="check_circle" size="40px" color="positive" />
        </div>
        <div class="settled-state__title">All Settled!</div>
        <div class="settled-state__sub">Everyone has paid their share.</div>
      </div>

    </div>

    <!-- ═══ Mark as Paid Dialog ═══ -->
    <q-dialog v-model="showPaymentDialog">
      <q-card class="payment-dialog">
        <q-card-section class="q-pb-sm">
          <div class="text-subtitle1 text-weight-bold">Record Payment</div>
          <div class="text-caption text-grey-6 q-mt-xs">
            {{ currencyCode }} {{ selectedSettlement?.amount.toFixed(2) }} to <strong>{{ selectedSettlement?.toName }}</strong>
          </div>
        </q-card-section>

        <q-card-section class="q-pt-sm">
          <q-select
            v-model="paymentMethod"
            :options="paymentMethods"
            label="Payment Method"
            outlined
            dense
            class="q-mb-md"
          />
          <q-input
            v-model="paymentNotes"
            label="Notes (optional)"
            outlined
            dense
            type="textarea"
            rows="2"
            class="q-mb-md"
          />
          <q-file
            v-model="paymentProofFile"
            label="Payment proof (optional)"
            outlined
            dense
            accept="image/*,.pdf"
          >
            <template v-slot:prepend><q-icon name="attach_file" /></template>
          </q-file>
        </q-card-section>

        <q-card-actions align="right" class="q-px-md q-pb-md">
          <q-btn flat no-caps label="Cancel" color="grey-7" v-close-popup />
          <q-btn
            unelevated
            no-caps
            label="Confirm Payment"
            color="primary"
            @click="confirmPayment"
            :loading="processingPayment"
            style="border-radius: 999px; padding: 0 20px;"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { sendPushToTripMembers } from 'src/utils/notificationService';
import {
  calculateMemberBalances,
  simplifySettlements,
  type MemberBalance,
  type Settlement,
  type ExpenseWithSplits,
} from 'src/utils/settlementCalculator';

const props = defineProps<{
  expenses: ExpenseWithSplits[];
  members: { id: string; name: string }[];
  currentMemberId?: string;
  currencyCode: string;
  tripId: string;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'payment-recorded'): void;
}>();

const $q = useQuasar();

// Payment dialog state
const showPaymentDialog = ref(false);
const selectedSettlement = ref<Settlement | null>(null);
const paymentMethod = ref('Cash');
const paymentNotes = ref('');
const paymentProofFile = ref<File | null>(null);
const processingPayment = ref(false);
const paidSettlementKeys = ref<Set<string>>(new Set());

const paymentMethods = ['Cash', 'GCash', 'Bank Transfer', 'PayMaya', 'Other', 'PayPal', 'Venmo'];

interface SettlementRecord {
  id: string;
  from_member_id: string;
  to_member_id: string;
  amount: number;
  status: string;
  payment_method: string;
  notes: string | null;
}

const existingSettlements = ref<SettlementRecord[]>([]);

const pendingVerifications = computed(() => {
  if (!props.currentMemberId) return [];
  return existingSettlements.value.filter(
    (s) => s.to_member_id === props.currentMemberId && s.status === 'pending',
  );
});

function getMemberName(memberId: string): string {
  return props.members.find((m) => m.id === memberId)?.name ?? 'Unknown';
}

async function fetchExistingSettlements(): Promise<void> {
  const { data } = await supabase
    .from('settlements')
    .select('id, from_member_id, to_member_id, amount, status, payment_method, notes')
    .eq('trip_id', props.tripId);

  existingSettlements.value = (data || []) as SettlementRecord[];

  const keys = new Set(paidSettlementKeys.value);
  for (const s of existingSettlements.value) {
    keys.add(`${s.from_member_id}->${s.to_member_id}`);
  }
  paidSettlementKeys.value = keys;
}

async function verifyPayment(settlement: SettlementRecord): Promise<void> {
  const { error } = await supabase
    .from('settlements')
    .update({ status: 'verified' })
    .eq('id', settlement.id);

  if (error) {
    $q.notify({ type: 'negative', message: 'Failed to confirm payment.' });
    return;
  }
  const s = existingSettlements.value.find((r) => r.id === settlement.id);
  if (s) s.status = 'verified';
  $q.notify({ type: 'positive', message: 'Payment confirmed!' });
}

onMounted(() => {
  void fetchExistingSettlements();
});

// Computed settlements
const memberBalances = computed((): MemberBalance[] => {
  return calculateMemberBalances(props.expenses, props.members);
});

const allSettlements = computed((): Settlement[] => {
  return simplifySettlements(memberBalances.value).filter(
    (s) => !paidSettlementKeys.value.has(`${s.from}->${s.to}`),
  );
});

const yourBalance = computed((): number => {
  if (!props.currentMemberId) return 0;
  const balance = memberBalances.value.find((b) => b.memberId === props.currentMemberId);
  return balance?.netBalance || 0;
});

const settlementsToPay = computed((): Settlement[] => {
  if (!props.currentMemberId) return [];
  return allSettlements.value.filter((s) => s.from === props.currentMemberId);
});

const settlementsToReceive = computed((): Settlement[] => {
  if (!props.currentMemberId) return [];
  return allSettlements.value.filter((s) => s.to === props.currentMemberId);
});

// Methods
function markAsPaid(settlement: Settlement) {
  selectedSettlement.value = settlement;
  paymentMethod.value = 'Cash';
  paymentNotes.value = '';
  paymentProofFile.value = null;
  showPaymentDialog.value = true;
}

async function confirmPayment() {
  if (!selectedSettlement.value) return;
  processingPayment.value = true;

  try {
    let proofUrl: string | null = null;
    if (paymentProofFile.value) {
      const file = paymentProofFile.value;
      const path = `payment-proofs/${props.tripId}/${Date.now()}_${file.name}`;
      const { data: uploadData, error: uploadError } = await supabase.storage
        .from('payment-proofs')
        .upload(path, file, { upsert: false });
      if (!uploadError && uploadData) {
        const { data: urlData } = supabase.storage.from('payment-proofs').getPublicUrl(uploadData.path);
        proofUrl = urlData.publicUrl;
      }
    }

    const { error } = await supabase.from('settlements').insert({
      trip_id: props.tripId,
      from_member_id: selectedSettlement.value.from,
      to_member_id: selectedSettlement.value.to,
      amount: selectedSettlement.value.amount,
      currency_code: props.currencyCode,
      payment_method: paymentMethod.value,
      notes: paymentNotes.value || null,
      payment_proof_url: proofUrl,
      status: 'pending',
      paid_at: new Date().toISOString(),
    });

    if (error) throw error;

    // Notify the receiver that payment was recorded
    void sendPushToTripMembers({
      trip_id: props.tripId,
      exclude_user_id: undefined,
      title: 'Payment recorded',
      body: `${getMemberName(selectedSettlement.value.from)} paid you ${props.currencyCode} ${selectedSettlement.value.amount.toFixed(2)}`,
      url: `/#/trips/${props.tripId}`,
    });

    // Hide this settlement from the list immediately
    paidSettlementKeys.value = new Set([
      ...paidSettlementKeys.value,
      `${selectedSettlement.value.from}->${selectedSettlement.value.to}`,
    ]);

    $q.notify({
      type: 'positive',
      message: `Payment of ${props.currencyCode} ${selectedSettlement.value.amount.toFixed(2)} via ${paymentMethod.value} recorded!`,
      icon: 'check_circle',
    });

    showPaymentDialog.value = false;
    selectedSettlement.value = null;
    emit('payment-recorded');
  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error instanceof Error ? error.message : 'Failed to record payment',
    });
  } finally {
    processingPayment.value = false;
  }
}
</script>

<style scoped lang="scss">
.settlement-view {
  max-width: 640px;
  margin: 0 auto;
  padding: 0 16px 80px;
}

// ─── Balance Panel ─────────────────────────────────────────────────────────────
.balance-panel {
  background: linear-gradient(135deg, #0D9488 0%, #134E4A 100%);
  border-radius: var(--gala-radius-xl);
  padding: 24px 20px 20px;
  color: #fff;

  &__eyebrow {
    font-size: 0.7rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.1em;
    opacity: 0.75;
    margin-bottom: 4px;
  }

  &__amount {
    font-family: 'Instrument Serif', Georgia, serif;
    font-size: 2.5rem;
    line-height: 1;
    margin-bottom: 4px;

    &--positive { color: #6EE7B7; }
    &--negative { color: #FCA5A5; }
  }

  &__status {
    font-size: 0.85rem;
    opacity: 0.85;
    font-weight: 500;
  }
}

// ─── Balance Pills ─────────────────────────────────────────────────────────────
.balance-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.balance-pill {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: var(--gala-radius-pill);
  padding: 6px 12px 6px 6px;
  border: 1px solid rgba(255, 255, 255, 0.15);

  &__avatar {
    width: 26px;
    height: 26px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.25);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.7rem;
    font-weight: 700;
    color: #fff;
    flex-shrink: 0;
  }

  &__info {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  &__name {
    font-size: 0.72rem;
    font-weight: 600;
    color: rgba(255,255,255,0.9);
    line-height: 1.2;
  }

  &__amount {
    font-size: 0.68rem;
    font-weight: 700;
    line-height: 1.2;
  }

  &--positive &__amount { color: #6EE7B7; }
  &--negative &__amount { color: #FCA5A5; }
  &--neutral &__amount { color: rgba(255,255,255,0.6); }
}

// ─── Section label ─────────────────────────────────────────────────────────────
.section-label {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--muted);
}

// ─── Settlement Cards ──────────────────────────────────────────────────────────
.settle-card {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;

  &--pay { border-left: 3px solid #DC2626; }
  &--pending { border-left: 3px solid #D97706; }

  &__avatar {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1rem;
    font-weight: 700;
    color: #fff;
    flex-shrink: 0;

    &--negative { background: #DC2626; }
    &--positive { background: #16A34A; }
    &--warning { background: #D97706; }
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--on-background);
    line-height: 1.3;
  }

  &__caption {
    font-size: 0.75rem;
    color: var(--muted);
    margin-top: 2px;
  }

  &__right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 6px;
    flex-shrink: 0;
  }

  &__amount {
    font-size: 0.9375rem;
    font-weight: 700;
    font-variant-numeric: tabular-nums;

    &--negative { color: #DC2626; }
    &--positive { color: #16A34A; }
  }

  &__action {
    font-size: 0.72rem;
    font-weight: 700;
    background: rgba(13, 148, 136, 0.1);
    color: #0D9488;
    border: none;
    border-radius: 999px;
    padding: 4px 12px;
    cursor: pointer;
    transition: background 0.15s;

    &:hover { background: rgba(13, 148, 136, 0.2); }
    &--positive {
      background: rgba(22, 163, 74, 0.1);
      color: #16A34A;
      &:hover { background: rgba(22, 163, 74, 0.2); }
    }
  }
}

// ─── Flow List ─────────────────────────────────────────────────────────────────
.flow-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.flow-item {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 8px;

  &__member {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 72px;
    &--right { justify-content: flex-end; }
  }

  &__pip {
    width: 30px;
    height: 30px;
    border-radius: 50%;
    background: var(--surface);
    border: 1.5px solid var(--border);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.75rem;
    font-weight: 700;
    color: var(--muted);
    flex-shrink: 0;

    &--teal {
      background: rgba(13, 148, 136, 0.12);
      border-color: rgba(13, 148, 136, 0.3);
      color: #0D9488;
    }
  }

  &__name {
    font-size: 0.78rem;
    font-weight: 600;
    color: var(--on-background);
  }

  &__arrow {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    flex-direction: column;
  }

  &__badge {
    font-size: 0.68rem;
    font-weight: 700;
    color: $primary;
    background: rgba(13, 148, 136, 0.1);
    border-radius: 999px;
    padding: 2px 8px;
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
  }

  &__line {
    display: none;
  }
}

// ─── All Settled State ─────────────────────────────────────────────────────────
.settled-state {
  text-align: center;
  padding: 40px 0 20px;

  &__icon {
    margin-bottom: 12px;
  }

  &__title {
    font-size: 1.125rem;
    font-weight: 700;
    color: var(--on-background);
    margin-bottom: 4px;
  }

  &__sub {
    font-size: 0.875rem;
    color: var(--muted);
  }
}

// ─── Dialog ───────────────────────────────────────────────────────────────────
.payment-dialog {
  min-width: 340px;
  border-radius: var(--gala-radius-xl) !important;
}
</style>
