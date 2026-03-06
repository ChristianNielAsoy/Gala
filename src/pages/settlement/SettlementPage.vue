<template>
  <q-page class="settle-page">

    <!-- ═══ Floating back button ═══ -->
    <q-btn
      flat round
      icon="arrow_back"
      class="settle-back-btn"
      @click="router.back()"
      aria-label="Go back"
    />

    <!-- ═══ Loading ═══ -->
    <div v-if="loading" class="settle-loading">
      <q-spinner color="primary" size="40px" />
    </div>

    <template v-else>
      <!-- ═══ Balance hero ═══ -->
      <div class="settle-hero">
        <div class="settle-hero__avatar">
          <img v-if="currentMember?.avatar_url" :src="currentMember.avatar_url" alt="avatar" />
          <span v-else>{{ getMemberInitial(currentMember?.name ?? '') }}</span>
        </div>

        <template v-if="userBalance">
          <template v-if="Math.abs(userBalance.netBalance) < 0.01">
            <div class="settle-hero__settled">
              <q-icon name="check_circle" size="22px" />
              All Settled Up!
            </div>
            <p class="settle-hero__settled-sub">You're square with everyone.</p>
          </template>
          <template v-else-if="userBalance.netBalance < 0">
            <div class="settle-hero__label">You owe</div>
            <div class="settle-hero__amount settle-hero__amount--owe">
              {{ formatCurrency(Math.abs(userBalance.netBalance), trip?.currency_code || 'PHP') }}
            </div>
          </template>
          <template v-else>
            <div class="settle-hero__label">You are owed</div>
            <div class="settle-hero__amount settle-hero__amount--owed">
              {{ formatCurrency(userBalance.netBalance, trip?.currency_code || 'PHP') }}
            </div>
          </template>
        </template>
      </div>

      <!-- ═══ Orbit Visualization ═══ -->
      <div class="settle-orbit-section" v-if="orbitMembers.length > 0">
        <div class="settle-orbit-label">Debt Orbit</div>
        <div class="settle-orbit-container">
          <svg
            class="settle-orbit-svg"
            viewBox="0 0 340 300"
            role="img"
            aria-label="Settlement debt orbit"
          >
            <defs>
              <filter id="settle-glow" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur stdDeviation="3" result="blur"/>
                <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
              </filter>
              <radialGradient id="center-grad" cx="50%" cy="50%" r="50%">
                <stop offset="0%" stop-color="#0d9488" stop-opacity="0.3"/>
                <stop offset="100%" stop-color="#0d9488" stop-opacity="0.08"/>
              </radialGradient>
            </defs>

            <!-- Dashed orbit ring -->
            <circle cx="170" cy="150" r="100" fill="none" stroke="rgba(100,116,139,0.25)" stroke-width="1" stroke-dasharray="4 5" />

            <!-- Debt lines from center to each member -->
            <g v-for="m in orbitMembers" :key="`line-${m.id}`">
              <line
                :x1="orbitLineStart(m).x"
                :y1="orbitLineStart(m).y"
                :x2="orbitLineEnd(m).x"
                :y2="orbitLineEnd(m).y"
                :stroke="m.youOwe ? '#ef4444' : m.theyOwe ? '#10b981' : 'var(--border)'"
                :stroke-width="getLineThickness(m)"
                stroke-linecap="round"
                :opacity="selectedOrbitMember && selectedOrbitMember.id !== m.id ? 0.2 : 0.9"
                style="transition: opacity 0.2s ease"
              />
              <!-- Amount label on line -->
              <text
                v-if="m.youOwe || m.theyOwe"
                :x="(ORBIT_CX + m.x) / 2"
                :y="(ORBIT_CY + m.y) / 2 - 6"
                text-anchor="middle"
                class="orbit-line-label"
                :fill="m.youOwe ? '#ef4444' : '#10b981'"
                :opacity="selectedOrbitMember && selectedOrbitMember.id !== m.id ? 0.2 : 1"
              >
                {{ m.youOwe
                  ? formatCurrency(m.youOwe.amount, m.youOwe.currency_code)
                  : formatCurrency(m.theyOwe!.amount, m.theyOwe!.currency_code) }}
              </text>
            </g>

            <!-- Member orbit nodes -->
            <g
              v-for="m in orbitMembers"
              :key="`node-${m.id}`"
              @click="selectOrbitMember(m)"
              :opacity="selectedOrbitMember && selectedOrbitMember.id !== m.id ? 0.35 : 1"
              style="transition: opacity 0.2s ease; cursor: pointer"
            >
              <!-- Selection ring (dashed, animated) -->
              <circle
                v-if="selectedOrbitMember?.id === m.id"
                :cx="m.x" :cy="m.y" r="33"
                fill="none"
                :stroke="m.youOwe ? '#ef4444' : '#10b981'"
                stroke-width="1.5"
                stroke-dasharray="5 4"
                class="orbit-selected-ring"
              />
              <!-- Node background -->
              <circle
                :cx="m.x" :cy="m.y" r="26"
                :fill="m.youOwe ? 'rgba(239,68,68,0.12)' : m.theyOwe ? 'rgba(16,185,129,0.12)' : 'var(--background)'"
                :stroke="m.youOwe ? '#ef4444' : m.theyOwe ? '#10b981' : 'var(--border)'"
                stroke-width="2"
              />
              <!-- Member initial -->
              <text
                :x="m.x" :y="m.y + 6"
                text-anchor="middle"
                class="orbit-member-initial"
                :fill="m.youOwe ? '#ef4444' : m.theyOwe ? '#10b981' : 'var(--on-background)'"
              >{{ m.name.charAt(0).toUpperCase() }}</text>
              <!-- Name below node -->
              <text
                :x="m.x" :y="m.y + 43"
                text-anchor="middle"
                class="orbit-member-name"
              >{{ m.name.split(' ')[0] }}</text>
            </g>

            <!-- Center YOU node -->
            <circle cx="170" cy="150" r="36" fill="url(#center-grad)" class="orbit-center-breathe" />
            <circle cx="170" cy="150" r="32" fill="none" stroke="#0d9488" stroke-width="2.5" />
            <text x="170" y="146" text-anchor="middle" class="orbit-center-you">YOU</text>
            <text x="170" y="159" text-anchor="middle" class="orbit-center-amount">
              {{ formatCurrency(Math.abs(userBalance?.netBalance ?? 0), trip?.currency_code || 'PHP') }}
            </text>
          </svg>

          <!-- Legend -->
          <div class="orbit-legend">
            <span class="orbit-legend-item orbit-legend-item--owe">
              <span class="orbit-legend-line" />
              You owe
            </span>
            <span class="orbit-legend-item orbit-legend-item--owed">
              <span class="orbit-legend-line" />
              Owed to you
            </span>
          </div>

          <!-- Selected member detail -->
          <transition name="orbit-slide">
            <div class="orbit-detail gala-panel" v-if="selectedOrbitMember">
              <div class="row items-center no-wrap q-gutter-sm">
                <div class="orbit-detail__avatar" :style="{ background: getMemberHue(selectedOrbitMember.name) }">
                  <img v-if="selectedOrbitMember.avatar_url" :src="selectedOrbitMember.avatar_url" :alt="selectedOrbitMember.name" />
                  <span v-else>{{ selectedOrbitMember.name.charAt(0).toUpperCase() }}</span>
                </div>
                <div class="col">
                  <div class="text-subtitle2 text-weight-bold">{{ selectedOrbitMember.name }}</div>
                  <div v-if="selectedOrbitMember.youOwe" class="text-caption" style="color: #ef4444">
                    You owe {{ formatCurrency(selectedOrbitMember.youOwe.amount, selectedOrbitMember.youOwe.currency_code) }}
                  </div>
                  <div v-else-if="selectedOrbitMember.theyOwe" class="text-caption" style="color: #10b981">
                    Owes you {{ formatCurrency(selectedOrbitMember.theyOwe.amount, selectedOrbitMember.theyOwe.currency_code) }}
                  </div>
                  <div v-else class="text-caption" style="color: var(--muted)">All settled up</div>
                </div>
                <q-btn
                  v-if="selectedOrbitMember.youOwe && !isPaid(selectedOrbitMember.youOwe)"
                  unelevated no-caps dense
                  color="negative"
                  label="Settle"
                  size="sm"
                  padding="6px 16px"
                  @click="markAsPaid(selectedOrbitMember.youOwe)"
                />
                <q-btn flat round dense icon="close" size="sm" @click="selectedOrbitMember = null" />
              </div>
            </div>
          </transition>
        </div>
      </div>

      <div class="settle-body">

        <!-- ═══ Your payments ═══ -->
        <template v-if="userBalance && userBalance.netBalance < -0.01">
          <div class="settle-section-label">Your Payments</div>

          <div
            v-for="suggestion in userSettlements"
            :key="suggestion.to_member_id"
            class="settle-card settle-card--pay q-mb-md"
          >
            <!-- Recipient header -->
            <div class="settle-card__header">
              <div class="settle-avatar" :style="{ background: getMemberHue(getMemberName(suggestion.to_member_id)) }">
                <img v-if="getMemberAvatar(suggestion.to_member_id)" :src="getMemberAvatar(suggestion.to_member_id) || ''" alt="" />
                <span v-else>{{ getMemberInitial(getMemberName(suggestion.to_member_id)) }}</span>
              </div>
              <div class="col">
                <div class="settle-card__to">Pay {{ getMemberName(suggestion.to_member_id) }}</div>
                <div class="settle-card__amount settle-card__amount--owe">
                  {{ formatCurrency(suggestion.amount, suggestion.currency_code) }}
                </div>
              </div>
            </div>

            <!-- Payment method pills -->
            <div class="settle-methods">
              <button
                v-for="method in paymentMethods"
                :key="method.value"
                class="settle-method-pill"
                :class="{ 'settle-method-pill--active': selectedPaymentMethod[suggestion.to_member_id] === method.value }"
                @click="selectPaymentMethod(suggestion.to_member_id, method.value as PaymentMethod)"
              >
                <q-icon :name="method.icon" size="16px" />
                {{ method.label }}
              </button>
            </div>

            <!-- Hint -->
            <div class="settle-card__hint">
              Prepare the exact amount via
              {{ selectedPaymentMethod[suggestion.to_member_id] || 'your chosen method' }}
              when you meet {{ getMemberName(suggestion.to_member_id) }}.
            </div>

            <q-btn
              unelevated no-caps
              color="primary"
              label="Mark as Paid"
              class="full-width settle-card__cta"
              :loading="markingPaid[suggestion.to_member_id]"
              :disable="!selectedPaymentMethod[suggestion.to_member_id]"
              @click="markAsPaid(suggestion)"
            />
          </div>
        </template>

        <!-- ═══ Payments to receive ═══ -->
        <template v-if="userBalance && userBalance.netBalance > 0.01">
          <div class="settle-section-label">Incoming Payments</div>

          <div
            v-for="suggestion in paymentsToReceive"
            :key="suggestion.from_member_id"
            class="settle-card settle-card--receive q-mb-md"
          >
            <div class="settle-card__header">
              <div class="settle-avatar" :style="{ background: getMemberHue(getMemberName(suggestion.from_member_id)) }">
                <img v-if="getMemberAvatar(suggestion.from_member_id)" :src="getMemberAvatar(suggestion.from_member_id) || ''" alt="" />
                <span v-else>{{ getMemberInitial(getMemberName(suggestion.from_member_id)) }}</span>
              </div>
              <div class="col">
                <div class="settle-card__to">From {{ getMemberName(suggestion.from_member_id) }}</div>
                <div class="settle-card__amount settle-card__amount--owed">
                  {{ formatCurrency(suggestion.amount, suggestion.currency_code) }}
                </div>
              </div>
              <q-icon
                :name="isPaid(suggestion) ? 'check_circle' : 'schedule'"
                :color="isPaid(suggestion) ? 'positive' : 'grey-4'"
                size="22px"
              />
            </div>
          </div>
        </template>

        <!-- All even state -->
        <div v-if="allSettlements.length === 0" class="settle-empty">
          <q-icon name="check_circle_outline" size="32px" color="positive" />
          <p>No settlements needed — everyone's even!</p>
        </div>

      </div>
    </template>

    <!-- ═══ Settlement celebration overlay ═══ -->
    <transition name="settle-celebrate">
      <div v-if="showCelebration" class="settle-celebration" @click="showCelebration = false">
        <div class="settle-celebration__icon">🥂</div>
        <div class="settle-celebration__text gala-display">All settled!</div>
        <div class="settle-celebration__sub">Payment marked — cheers!</div>
      </div>
    </transition>

    <!-- ═══ Payment proof dialog ═══ -->
    <q-dialog v-model="showProofDialog">
      <div class="settle-dialog">
        <div class="settle-dialog__title">Confirm Payment</div>
        <div class="settle-dialog__body">
          <q-input
            v-model="paymentNotes"
            label="Notes (optional)"
            type="textarea"
            outlined dense rows="3"
            class="q-mb-md"
          />
          <q-file
            v-model="paymentProof"
            label="Upload receipt / screenshot (optional)"
            outlined accept="image/*"
            max-file-size="5242880"
            @rejected="onProofRejected"
          >
            <template #prepend><q-icon name="attach_file" /></template>
          </q-file>
        </div>
        <div class="settle-dialog__actions">
          <q-btn flat no-caps label="Cancel" v-close-popup />
          <q-btn
            unelevated no-caps
            color="primary"
            label="Confirm Payment"
            :loading="submittingPayment"
            @click="confirmPayment"
          />
        </div>
      </div>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { fireConfetti } from 'src/utils/confetti';
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { logActivity } from 'src/utils/activityLogger';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import type { Trip } from 'src/types/expense';
import type { Expense, ExpenseSplit, TripMember } from 'src/types/expense';
import {
  calculateMemberBalances,
  formatCurrency,
  type MemberBalance,
} from 'src/utils/settlementCalculator';
import type { ExpenseWithSplits } from 'src/utils/settlementCalculator';

interface SettlementSuggestion {
  from_member_id: string;
  to_member_id: string;
  amount: number;
  currency_code: string;
}

interface Settlement {
  id?: string;
  trip_id: string;
  from_member_id: string;
  to_member_id: string;
  amount: number;
  currency_code: string;
  payment_method?: string;
  payment_proof_url?: string;
  notes?: string;
  status: string;
  paid_at?: string;
}

type PaymentMethod = 'cash' | 'gcash' | 'card';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();

const loading = ref(true);
const tripId = ref(route.params.tripId as string);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const expenses = ref<Expense[]>([]);
const splits = ref<ExpenseSplit[]>([]);
const existingSettlements = ref<Settlement[]>([]);
const currentUserId = ref<string | undefined>(undefined);

const selectedPaymentMethod = ref<Record<string, PaymentMethod>>({});
const markingPaid = ref<Record<string, boolean>>({});

const showProofDialog = ref(false);
const showCelebration = ref(false);
const currentSettlement = ref<SettlementSuggestion | null>(null);
const paymentNotes = ref('');
const paymentProof = ref<File | null>(null);
const submittingPayment = ref(false);

const paymentMethods = [
  { value: 'cash', label: 'Cash', icon: 'payments' },
  { value: 'gcash', label: 'GCash', icon: 'phone_android' },
  { value: 'card', label: 'Card', icon: 'credit_card' },
];

const currentMember = computed(() => members.value.find((m) => m.user_id === currentUserId.value));

function generateSettlementSuggestions(balances: MemberBalance[], currencyCode: string): SettlementSuggestion[] {
  const suggestions: SettlementSuggestion[] = [];
  const debtors = balances.filter((b) => b.netBalance < -0.01).sort((a, b) => a.netBalance - b.netBalance);
  const creditors = balances.filter((b) => b.netBalance > 0.01).sort((a, b) => b.netBalance - a.netBalance);

  let i = 0, j = 0;
  while (i < debtors.length && j < creditors.length) {
    const debtor = debtors[i];
    const creditor = creditors[j];
    if (!debtor || !creditor) break;

    const amount = Math.min(Math.abs(debtor.netBalance), creditor.netBalance);
    suggestions.push({
      from_member_id: debtor.memberId,
      to_member_id: creditor.memberId,
      amount: Math.round(amount * 100) / 100,
      currency_code: currencyCode,
    });

    debtor.netBalance += amount;
    creditor.netBalance -= amount;
    if (Math.abs(debtor.netBalance) < 0.01) i++;
    if (Math.abs(creditor.netBalance) < 0.01) j++;
  }
  return suggestions;
}

const balances = computed(() => {
  const expensesWithSplits: ExpenseWithSplits[] = expenses.value.map((e) => ({
    ...e,
    splits: (splits.value || []).filter((s) => s.expense_id === e.id),
  }));
  return calculateMemberBalances(expensesWithSplits, members.value);
});

const allSettlements = computed(() =>
  generateSettlementSuggestions(balances.value, trip.value?.currency_code || 'PHP'),
);

const userBalance = computed(() =>
  balances.value.find((b: MemberBalance) => b.memberId === currentMember.value?.id),
);

const userSettlements = computed(() =>
  allSettlements.value.filter((s) => s.from_member_id === currentMember.value?.id),
);

const paymentsToReceive = computed(() =>
  allSettlements.value.filter((s) => s.to_member_id === currentMember.value?.id),
);

function getMemberName(memberId: string): string {
  return members.value.find((m) => m.id === memberId)?.name || 'Unknown';
}

function getMemberAvatar(memberId: string): string | undefined {
  return members.value.find((m) => m.id === memberId)?.avatar_url;
}

function getMemberInitial(name: string): string {
  return name ? name.charAt(0).toUpperCase() : '?';
}

function getMemberHue(name: string): string {
  const hues = ['#0D9488', '#7C3AED', '#2563EB', '#D97706', '#DC2626'];
  return hues[name.charCodeAt(0) % hues.length] ?? '#0D9488';
}

function selectPaymentMethod(toMemberId: string, method: PaymentMethod) {
  selectedPaymentMethod.value[toMemberId] = method;
}

function isPaid(settlement: SettlementSuggestion): boolean {
  return existingSettlements.value.some(
    (s) =>
      s.from_member_id === settlement.from_member_id &&
      s.to_member_id === settlement.to_member_id &&
      (s.status === 'paid' || s.status === 'verified'),
  );
}

function markAsPaid(settlement: SettlementSuggestion) {
  currentSettlement.value = settlement;
  showProofDialog.value = true;
}

async function confirmPayment() {
  if (!currentSettlement.value) return;
  submittingPayment.value = true;
  const settlement = currentSettlement.value;

  try {
    let proofUrl = null;
    if (paymentProof.value) {
      const fileName = `${tripId.value}/${Date.now()}_${paymentProof.value.name}`;
      const { error } = await supabase.storage.from('payment-proofs').upload(fileName, paymentProof.value);
      if (error) throw error;
      const { data: urlData } = supabase.storage.from('payment-proofs').getPublicUrl(fileName);
      proofUrl = urlData.publicUrl;
    }

    const { error } = await supabase.from('settlements').insert({
      trip_id: tripId.value,
      from_member_id: settlement.from_member_id,
      to_member_id: settlement.to_member_id,
      amount: settlement.amount,
      currency_code: settlement.currency_code,
      payment_method: selectedPaymentMethod.value[settlement.to_member_id],
      payment_proof_url: proofUrl,
      notes: paymentNotes.value,
      status: 'paid',
      paid_at: new Date().toISOString(),
    });
    if (error) throw error;

    await logActivity({
      trip_id: tripId.value,
      user_id: authStore.user?.id,
      action_type: 'settlement_paid',
      entity_type: 'settlement',
      description: `Marked payment of ${formatCurrency(settlement.amount, settlement.currency_code)} to ${getMemberName(settlement.to_member_id)} as paid`,
      metadata: {
        amount: settlement.amount,
        currency_code: settlement.currency_code,
        payment_method: selectedPaymentMethod.value[settlement.to_member_id],
        from_member_id: settlement.from_member_id,
        to_member_id: settlement.to_member_id,
      },
    });

    $q.notify({ type: 'positive', message: 'Payment marked as paid!', caption: 'Waiting for verification' });
    showCelebration.value = true;
    fireConfetti();
    setTimeout(() => { showCelebration.value = false; }, 2800);
    showProofDialog.value = false;
    paymentNotes.value = '';
    paymentProof.value = null;
    currentSettlement.value = null;
    await fetchData();
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error';
    $q.notify({ type: 'negative', message: 'Failed to mark payment', caption: msg });
  } finally {
    submittingPayment.value = false;
  }
}

function onProofRejected() {
  $q.notify({ type: 'warning', message: 'File must be an image under 5 MB' });
}

async function fetchData() {
  loading.value = true;
  try {
    const { data: tripData, error: tripError } = await supabase.from('trips').select('*').eq('id', tripId.value).single();
    if (tripError) throw tripError;
    trip.value = tripData;

    const { data: memberData, error: memberError } = await supabase.from('members').select('*').eq('trip_id', tripId.value);
    if (memberError) throw memberError;
    members.value = memberData;

    const { data: expenseData, error: expenseError } = await supabase.from('expenses').select('*').eq('trip_id', tripId.value);
    if (expenseError) throw expenseError;
    expenses.value = expenseData;

    const { data: splitData, error: splitError } = await supabase.from('expense_splits').select('*').in('expense_id', expenses.value.map((e) => e.id));
    if (splitError) throw splitError;
    splits.value = splitData;

    const { data: settlementData, error: settlementError } = await supabase.from('settlements').select('*').eq('trip_id', tripId.value);
    if (settlementError) throw settlementError;
    existingSettlements.value = settlementData;
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error';
    $q.notify({ type: 'negative', message: 'Failed to load data', caption: msg });
  } finally {
    loading.value = false;
  }
}

// ─── Orbit ────────────────────────────────────────────────────────────────────
interface OrbitMember extends TripMember {
  x: number;
  y: number;
  youOwe: SettlementSuggestion | null;
  theyOwe: SettlementSuggestion | null;
}

const ORBIT_CX = 170;
const ORBIT_CY = 150;
const ORBIT_R = 100;
const CENTER_R = 32;
const MEMBER_R = 26;

const orbitMembers = computed((): OrbitMember[] => {
  if (!currentMember.value) return [];
  const others = members.value.filter((m) => m.id !== currentMember.value!.id);
  const n = others.length;
  if (n === 0) return [];
  return others.map((member, i) => {
    const angle = (i / n) * 2 * Math.PI - Math.PI / 2;
    const x = ORBIT_CX + ORBIT_R * Math.cos(angle);
    const y = ORBIT_CY + ORBIT_R * Math.sin(angle);
    const youOwe = allSettlements.value.find(
      (s) => s.from_member_id === currentMember.value!.id && s.to_member_id === member.id,
    ) ?? null;
    const theyOwe = allSettlements.value.find(
      (s) => s.to_member_id === currentMember.value!.id && s.from_member_id === member.id,
    ) ?? null;
    return { ...member, x, y, youOwe, theyOwe };
  });
});

const maxDebt = computed(() =>
  Math.max(...orbitMembers.value.map((m) => m.youOwe?.amount ?? m.theyOwe?.amount ?? 0), 1),
);

const selectedOrbitMember = ref<OrbitMember | null>(null);

function orbitLineStart(m: OrbitMember) {
  const dx = m.x - ORBIT_CX;
  const dy = m.y - ORBIT_CY;
  const len = Math.hypot(dx, dy);
  return { x: ORBIT_CX + (dx / len) * (CENTER_R + 3), y: ORBIT_CY + (dy / len) * (CENTER_R + 3) };
}

function orbitLineEnd(m: OrbitMember) {
  const dx = m.x - ORBIT_CX;
  const dy = m.y - ORBIT_CY;
  const len = Math.hypot(dx, dy);
  return { x: m.x - (dx / len) * (MEMBER_R + 3), y: m.y - (dy / len) * (MEMBER_R + 3) };
}

function getLineThickness(m: OrbitMember): number {
  const amount = m.youOwe?.amount ?? m.theyOwe?.amount ?? 0;
  return 1.5 + (amount / maxDebt.value) * 4;
}

function selectOrbitMember(m: OrbitMember) {
  selectedOrbitMember.value = selectedOrbitMember.value?.id === m.id ? null : m;
}

onMounted(async () => {
  currentUserId.value = authStore.user?.id;
  await fetchData();
});
</script>

<style scoped lang="scss">
.settle-page {
  min-height: 100vh;
  background: var(--background);
}

// ─── Back button ────────────────────────────────────────────────────────────────
.settle-back-btn {
  position: absolute;
  top: 16px;
  left: 12px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  color: #fff;
}

// ─── Hero ────────────────────────────────────────────────────────────────────
.settle-hero {
  background: linear-gradient(145deg, #0D9488 0%, #134E4A 100%);
  padding: 72px 24px 44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    width: 300px;
    height: 300px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
    top: -100px;
    right: -60px;
  }
}

.settle-hero__avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  overflow: hidden;
  border: 3px solid rgba(255, 255, 255, 0.3);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span {
    font-family: 'Instrument Serif', Georgia, serif;
    font-size: 1.75rem;
    color: #fff;
  }
}

.settle-hero__label {
  font-size: 0.875rem;
  color: rgba(255, 255, 255, 0.75);
  font-weight: 500;
  margin-bottom: 4px;
}

.settle-hero__amount {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 3rem;
  line-height: 1;

  &--owe { color: #FCA5A5; }
  &--owed { color: #6EE7B7; }
}

.settle-hero__settled {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 1.5rem;
  color: #fff;
}

.settle-hero__settled-sub {
  font-size: 0.9rem;
  color: rgba(255, 255, 255, 0.7);
  margin: 6px 0 0;
}

// ─── Body ────────────────────────────────────────────────────────────────────
.settle-body {
  max-width: 600px;
  margin: 0 auto;
  padding: 24px 16px 80px;
}

.settle-section-label {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 12px;
}

// ─── Cards ────────────────────────────────────────────────────────────────────
.settle-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;

  &--pay {
    border-left: 4px solid #EF4444;
  }

  &--receive {
    border-left: 4px solid #10B981;
  }
}

.settle-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.settle-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span {
    font-weight: 700;
    font-size: 1rem;
    color: #fff;
  }
}

.settle-card__to {
  font-size: 0.875rem;
  color: var(--muted);
  margin-bottom: 2px;
}

.settle-card__amount {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 1.5rem;
  line-height: 1;

  &--owe { color: #EF4444; }
  &--owed { color: #10B981; }
}

// ─── Payment methods ────────────────────────────────────────────────────────────
.settle-methods {
  display: flex;
  gap: 8px;
  padding: 0 16px 14px;
}

.settle-method-pill {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 8px;
  border-radius: var(--gala-radius-md);
  border: 1.5px solid var(--border);
  background: var(--background);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover { border-color: #0D9488; color: #0D9488; }

  &--active {
    border-color: #0D9488;
    background: rgba(13, 148, 136, 0.08);
    color: #0D9488;
    font-weight: 600;
  }
}

.settle-card__hint {
  margin: 0 16px 14px;
  background: var(--background);
  border-radius: var(--gala-radius-md);
  padding: 10px 14px;
  font-size: 0.8125rem;
  color: var(--muted);
  line-height: 1.5;
}

.settle-card__cta {
  border-radius: 0;
  height: 48px;
  font-size: 0.9375rem;
  font-weight: 600;
}

// ─── All settlements panel ────────────────────────────────────────────────────────────
.settle-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

.settle-flow-row__inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
}

.settle-flow-name {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--on-background);
}

.settle-flow-amount {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--on-background);
}

.settle-flow-badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 3px 9px;
  border-radius: var(--gala-radius-pill);

  &--paid {
    background: rgba(16, 185, 129, 0.12);
    color: #059669;
  }

  &--pending {
    background: rgba(107, 114, 128, 0.1);
    color: var(--muted);
  }
}

.settle-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
  padding: 32px;
  color: var(--muted);
  font-size: 0.9rem;

  p { margin: 0; }
}

// ─── Orbit ────────────────────────────────────────────────────────────────────
.settle-orbit-section {
  background: var(--surface);
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  padding: 20px 16px 16px;
}

.settle-orbit-label {
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--muted);
  text-align: center;
  margin-bottom: 2px;
}

.settle-orbit-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.settle-orbit-svg {
  width: 100%;
  max-width: 320px;
  overflow: visible;
}

.orbit-line-label {
  font-size: 9px;
  font-weight: 700;
  font-family: 'Plus Jakarta Sans', sans-serif;
}

.orbit-member-initial {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 1.05rem;
}

.orbit-member-name {
  font-size: 9px;
  font-weight: 600;
  font-family: 'Plus Jakarta Sans', sans-serif;
  fill: var(--muted, #94a3b8);
}

.orbit-center-you {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  fill: #0d9488;
  font-family: 'Plus Jakarta Sans', sans-serif;
}

.orbit-center-amount {
  font-size: 8px;
  fill: #0d9488;
  font-family: 'Plus Jakarta Sans', sans-serif;
  opacity: 0.7;
}

.orbit-center-breathe {
  animation: orbit-breathe 3s ease-in-out infinite;
  transform-origin: 170px 150px;
}

@keyframes orbit-breathe {
  0%, 100% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.06); opacity: 1; }
}

.orbit-selected-ring {
  animation: orbit-ring-spin 6s linear infinite;
  transform-box: fill-box;
  transform-origin: center;
}

@keyframes orbit-ring-spin {
  to { transform: rotate(360deg); }
}

.orbit-legend {
  display: flex;
  gap: 24px;
  margin: 6px 0;
}

.orbit-legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.7rem;
  font-weight: 500;
  color: var(--muted);
}

.orbit-legend-line {
  width: 18px;
  height: 3px;
  border-radius: 2px;
}

.orbit-legend-item--owe .orbit-legend-line { background: #ef4444; }
.orbit-legend-item--owed .orbit-legend-line { background: #10b981; }

.orbit-detail {
  width: 100%;
  max-width: 360px;
  padding: 12px 16px;
  margin-top: 12px;

  &__avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: 'Instrument Serif', Georgia, serif;
    font-size: 1.1rem;
    color: white;
    flex-shrink: 0;
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.orbit-slide-enter-active,
.orbit-slide-leave-active {
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.orbit-slide-enter-from,
.orbit-slide-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

// ─── Celebration overlay ──────────────────────────────────────────────────────
.settle-celebration {
  position: fixed;
  inset: 0;
  z-index: 9998;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(13, 148, 136, 0.12);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  cursor: pointer;

  &__icon {
    font-size: 4rem;
    animation: clink-bounce 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
    margin-bottom: 16px;
  }

  &__text {
    font-size: 2.2rem;
    color: var(--on-background);
    margin-bottom: 8px;
  }

  &__sub {
    font-size: 0.9rem;
    color: var(--muted);
  }
}

@keyframes clink-bounce {
  0%   { transform: scale(0.5) rotate(-15deg); opacity: 0; }
  60%  { transform: scale(1.2) rotate(10deg);  opacity: 1; }
  100% { transform: scale(1)   rotate(0deg);   opacity: 1; }
}

.settle-celebrate-enter-active { transition: opacity 0.3s ease; }
.settle-celebrate-leave-active  { transition: opacity 0.5s ease; }
.settle-celebrate-enter-from,
.settle-celebrate-leave-to      { opacity: 0; }

// ─── Loading ────────────────────────────────────────────────────────────────────
.settle-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}

// ─── Dialog ────────────────────────────────────────────────────────────────────
.settle-dialog {
  background: var(--surface);
  border-radius: var(--gala-radius-lg);
  padding: 24px;
  width: 100%;
  max-width: 400px;
}

.settle-dialog__title {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 1.5rem;
  color: var(--on-background);
  margin-bottom: 20px;
}

.settle-dialog__body {
  margin-bottom: 20px;
}

.settle-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
