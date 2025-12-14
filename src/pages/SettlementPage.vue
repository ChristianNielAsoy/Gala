<template>
  <q-page class="q-pb-xl bg-surface">
    <q-header elevated class="bg-primary text-white">
      <q-toolbar>
        <q-btn flat round icon="arrow_back" @click="router.back()" aria-label="Go Back" />
        <q-toolbar-title>Settlement</q-toolbar-title>
      </q-toolbar>
    </q-header>

    <!-- Loading State -->
    <div v-if="loading" class="flex flex-center q-pa-xl">
      <q-spinner color="primary" size="lg" />
    </div>

    <!-- Content -->
    <div v-else class="q-pa-md">
      <!-- User's Balance Summary Card -->
      <q-card class="q-mb-lg shadow-4 text-center">
        <q-card-section class="bg-gradient-primary text-white q-py-xl">
          <q-avatar size="80px" class="q-mb-md">
            <img
              v-if="currentMember?.avatar_url"
              :src="currentMember.avatar_url"
              alt="User avatar"
            />
            <q-icon v-else name="person" size="40px" />
          </q-avatar>

          <div v-if="userBalance">
            <div v-if="Math.abs(userBalance.netBalance) < 0.01" class="text-h5 q-mb-sm">
              <q-icon name="check_circle" size="md" class="q-mr-sm" />
              All Settled!
            </div>
            <div v-else-if="userBalance.netBalance < 0">
              <div class="text-subtitle2 text-grey-3">You owe</div>
              <div class="text-h3 text-weight-bold text-negative">
                {{ formatCurrency(Math.abs(userBalance.netBalance), trip?.currency_code || 'PHP') }}
              </div>
            </div>
            <div v-else>
              <div class="text-subtitle2 text-grey-3">You are owed</div>
              <div class="text-h3 text-weight-bold text-positive">
                {{ formatCurrency(userBalance.netBalance, trip?.currency_code || 'PHP') }}
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Settlements to Make (if user owes money) -->
      <div v-if="userBalance && userBalance.netBalance < -0.01">
        <div class="text-h6 q-mb-md">Your Payments</div>

        <q-card
          v-for="suggestion in userSettlements"
          :key="suggestion.to_member_id"
          class="q-mb-md shadow-2"
        >
          <q-card-section>
            <div class="row items-center q-mb-md">
              <q-avatar size="50px" class="q-mr-md">
                <img
                  v-if="getMemberAvatar(suggestion.to_member_id)"
                  :src="getMemberAvatar(suggestion.to_member_id) || ''"
                  alt="Member avatar"
                />
                <q-icon v-else name="person" size="24px" />
              </q-avatar>
              <div class="col">
                <div class="text-subtitle1 text-weight-medium">
                  Pay {{ getMemberName(suggestion.to_member_id) }}
                </div>
                <div class="text-h5 text-negative text-weight-bold">
                  {{ formatCurrency(suggestion.amount, suggestion.currency_code) }}
                </div>
              </div>
            </div>

            <!-- Payment Method Selection -->
            <div class="text-subtitle2 text-grey-7 q-mb-sm">Payment Method</div>
            <div class="row q-col-gutter-sm q-mb-md">
              <div class="col-4">
                <q-btn
                  outline
                  :color="
                    selectedPaymentMethod[suggestion.to_member_id] === 'cash' ? 'primary' : 'grey-5'
                  "
                  class="full-width"
                  padding="md"
                  @click="selectPaymentMethod(suggestion.to_member_id, 'cash')"
                >
                  <div class="column items-center">
                    <q-icon name="payments" size="sm" />
                    <div class="text-caption q-mt-xs">Cash</div>
                  </div>
                </q-btn>
              </div>
              <div class="col-4">
                <q-btn
                  outline
                  :color="
                    selectedPaymentMethod[suggestion.to_member_id] === 'gcash'
                      ? 'primary'
                      : 'grey-5'
                  "
                  class="full-width"
                  padding="md"
                  @click="selectPaymentMethod(suggestion.to_member_id, 'gcash')"
                >
                  <div class="column items-center">
                    <q-icon name="phone_android" size="sm" />
                    <div class="text-caption q-mt-xs">GCash</div>
                  </div>
                </q-btn>
              </div>
              <div class="col-4">
                <q-btn
                  outline
                  :color="
                    selectedPaymentMethod[suggestion.to_member_id] === 'card' ? 'primary' : 'grey-5'
                  "
                  class="full-width"
                  padding="md"
                  @click="selectPaymentMethod(suggestion.to_member_id, 'card')"
                >
                  <div class="column items-center">
                    <q-icon name="credit_card" size="sm" />
                    <div class="text-caption q-mt-xs">Card</div>
                  </div>
                </q-btn>
              </div>
            </div>

            <!-- Payment Details -->
            <div class="bg-surface rounded-borders q-pa-md q-mb-md">
              <div class="text-caption text-grey-7">
                Prepare the exact amount for
                {{ selectedPaymentMethod[suggestion.to_member_id] || 'your chosen method' }} payment
                upon meeting {{ getMemberName(suggestion.to_member_id) }}.
              </div>
            </div>

            <!-- Mark as Paid Button -->
            <q-btn
              color="primary"
              class="full-width"
              label="Mark as Paid"
              size="lg"
              @click="markAsPaid(suggestion)"
              :loading="markingPaid[suggestion.to_member_id]"
              :disable="!selectedPaymentMethod[suggestion.to_member_id]"
            />
          </q-card-section>
        </q-card>
      </div>

      <!-- Settlements to Receive (if user is owed money) -->
      <div v-if="userBalance && userBalance.netBalance > 0.01">
        <div class="text-h6 q-mb-md">Payments You'll Receive</div>

        <q-card
          v-for="suggestion in paymentsToReceive"
          :key="suggestion.from_member_id"
          class="q-mb-md shadow-2"
        >
          <q-card-section>
            <div class="row items-center">
              <q-avatar size="50px" class="q-mr-md">
                <img
                  v-if="getMemberAvatar(suggestion.from_member_id)"
                  :src="getMemberAvatar(suggestion.from_member_id) || ''"
                  alt="Member avatar"
                />
                <q-icon v-else name="person" size="24px" />
              </q-avatar>
              <div class="col">
                <div class="text-subtitle1 text-weight-medium">
                  From {{ getMemberName(suggestion.from_member_id) }}
                </div>
                <div class="text-h5 text-positive text-weight-bold">
                  {{ formatCurrency(suggestion.amount, suggestion.currency_code) }}
                </div>
              </div>
              <q-icon name="check_circle" color="positive" size="md" v-if="isPaid(suggestion)" />
              <q-icon name="schedule" color="grey-5" size="md" v-else />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- All Settlements (For Reference) -->
      <div class="q-mt-xl">
        <div class="text-h6 q-mb-md">All Trip Settlements</div>

        <q-list bordered separator>
          <q-item
            v-for="suggestion in allSettlements"
            :key="`${suggestion.from_member_id}-${suggestion.to_member_id}`"
          >
            <q-item-section avatar>
              <q-avatar>
                <img
                  v-if="getMemberAvatar(suggestion.from_member_id)"
                  :src="getMemberAvatar(suggestion.from_member_id) || ''"
                  alt="Member avatar"
                />
                <q-icon v-else name="person" />
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label>
                {{ getMemberName(suggestion.from_member_id) }}
                <q-icon name="arrow_forward" size="xs" class="q-mx-xs" />
                {{ getMemberName(suggestion.to_member_id) }}
              </q-item-label>
              <q-item-label caption>
                {{ formatCurrency(suggestion.amount, suggestion.currency_code) }}
              </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-badge :color="isPaid(suggestion) ? 'positive' : 'grey-5'">
                {{ isPaid(suggestion) ? 'Paid' : 'Pending' }}
              </q-badge>
            </q-item-section>
          </q-item>
        </q-list>
      </div>
    </div>

    <!-- Upload Payment Proof Dialog -->
    <q-dialog v-model="showProofDialog">
      <q-card style="width: 100%; max-width: 400px">
        <q-card-section>
          <div class="text-h6">Payment Confirmation</div>
        </q-card-section>

        <q-card-section>
          <div class="text-body2 q-mb-md">Add a note or upload proof of payment (optional):</div>

          <q-input
            v-model="paymentNotes"
            label="Notes"
            type="textarea"
            outlined
            rows="3"
            class="q-mb-md"
          />

          <q-file
            v-model="paymentProof"
            label="Upload Receipt/Screenshot"
            outlined
            accept="image/*"
            max-file-size="5242880"
            @rejected="onProofRejected"
          >
            <template v-slot:prepend>
              <q-icon name="attach_file" />
            </template>
          </q-file>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" v-close-popup />
          <q-btn
            flat
            label="Confirm Payment"
            color="primary"
            @click="confirmPayment"
            :loading="submittingPayment"
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
import { logActivity, getCurrentUserId } from 'src/utils/activityLogger';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';
import type { Expense, ExpenseSplit, TripMember } from 'src/types/expense';
import {
  calculateMemberBalances,
  formatCurrency,
  type MemberBalance,
} from 'src/utils/settlementCalculator';
import type { ExpenseWithSplits } from 'src/utils/settlementCalculator';

// Define types locally
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

// State
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

// Payment proof dialog
const showProofDialog = ref(false);
const currentSettlement = ref<SettlementSuggestion | null>(null);
const paymentNotes = ref('');
const paymentProof = ref<File | null>(null);
const submittingPayment = ref(false);

// Current user
const currentMember = computed(() => members.value.find((m) => m.user_id === currentUserId.value));

// Generate settlement suggestions from balances
function generateSettlementSuggestions(
  balances: MemberBalance[],
  currencyCode: string,
): SettlementSuggestion[] {
  const suggestions: SettlementSuggestion[] = [];

  // Separate debtors and creditors
  const debtors = balances
    .filter((b) => b.netBalance < -0.01)
    .sort((a, b) => a.netBalance - b.netBalance);
  const creditors = balances
    .filter((b) => b.netBalance > 0.01)
    .sort((a, b) => b.netBalance - a.netBalance);

  let i = 0,
    j = 0;

  while (i < debtors.length && j < creditors.length) {
    const debtor = debtors[i]; // ✅ Safe access
    const creditor = creditors[j]; // ✅ Safe access

    if (!debtor || !creditor) break; // ✅ Guard clause

    const debt = Math.abs(debtor.netBalance);
    const credit = creditor.netBalance;
    const amount = Math.min(debt, credit);

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

// Calculations
const balances = computed(() => {
  // Map expenses to ExpenseWithSplits shape (add empty splits if missing)
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
  allSettlements.value.filter(
    (s: SettlementSuggestion) => s.from_member_id === currentMember.value?.id,
  ),
);

const paymentsToReceive = computed(() =>
  allSettlements.value.filter(
    (s: SettlementSuggestion) => s.to_member_id === currentMember.value?.id,
  ),
);

// Helper Functions
function getMemberName(memberId: string): string {
  return members.value.find((m) => m.id === memberId)?.name || 'Unknown';
}

function getMemberAvatar(memberId: string): string | undefined {
  return members.value.find((m) => m.id === memberId)?.avatar_url;
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

    // Upload payment proof if provided
    if (paymentProof.value) {
      const fileName = `${tripId.value}/${Date.now()}_${paymentProof.value.name}`;
      const { error } = await supabase.storage
        .from('payment-proofs')
        .upload(fileName, paymentProof.value);

      if (error) throw error;

      const { data: urlData } = supabase.storage.from('payment-proofs').getPublicUrl(fileName);

      proofUrl = urlData.publicUrl;
    }

    // Create settlement record
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

    // Log activity
    const userId = await getCurrentUserId();
    await logActivity({
      trip_id: tripId.value,
      user_id: userId,
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

    $q.notify({
      type: 'positive',
      message: 'Payment marked as paid!',
      caption: 'Waiting for verification from receiver',
    });

    // Reset dialog
    showProofDialog.value = false;
    paymentNotes.value = '';
    paymentProof.value = null;
    currentSettlement.value = null;

    // Reload data
    await fetchData();
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error('Error marking payment:', error);
    $q.notify({
      type: 'negative',
      message: 'Failed to mark payment',
      caption: errorMessage,
    });
  } finally {
    submittingPayment.value = false;
  }
}

function onProofRejected() {
  $q.notify({
    type: 'warning',
    message: 'File must be an image under 5MB',
  });
}

// Data Fetching
async function fetchData() {
  loading.value = true;

  try {
    // Fetch trip
    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .select('*')
      .eq('id', tripId.value)
      .single();

    if (tripError) throw tripError;
    trip.value = tripData;

    // Fetch members
    const { data: memberData, error: memberError } = await supabase
      .from('members')
      .select('*')
      .eq('trip_id', tripId.value);

    if (memberError) throw memberError;
    members.value = memberData;

    // Fetch expenses
    const { data: expenseData, error: expenseError } = await supabase
      .from('expenses')
      .select('*')
      .eq('trip_id', tripId.value);

    if (expenseError) throw expenseError;
    expenses.value = expenseData;

    // Fetch splits
    const { data: splitData, error: splitError } = await supabase
      .from('expense_splits')
      .select('*')
      .in(
        'expense_id',
        expenses.value.map((e) => e.id),
      );

    if (splitError) throw splitError;
    splits.value = splitData;

    // Fetch existing settlements
    const { data: settlementData, error: settlementError } = await supabase
      .from('settlements')
      .select('*')
      .eq('trip_id', tripId.value);

    if (settlementError) throw settlementError;
    existingSettlements.value = settlementData;
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : 'Unknown error';
    console.error('Error fetching data:', error);
    $q.notify({
      type: 'negative',
      message: 'Failed to load data',
      caption: errorMessage,
    });
  } finally {
    loading.value = false;
  }
}

// Get current user ID on mount
async function initializeUser() {
  const { data } = await supabase.auth.getUser();
  currentUserId.value = data.user?.id;
}

onMounted(async () => {
  await initializeUser();
  await fetchData();
});
</script>

<style scoped>
.bg-gradient-primary {
  background: linear-gradient(135deg, #1976d2 0%, #42a5f5 100%);
}
</style>
