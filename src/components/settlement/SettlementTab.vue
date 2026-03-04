<template>
  <div class="settlement-tab q-pa-md">
    <!-- Loading State -->
    <div v-if="loading" class="flex flex-center q-pa-xl">
      <q-spinner color="primary" size="lg" />
    </div>

    <!-- Content -->
    <div v-else>
      <!-- Overall Summary -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-h6 text-weight-bold q-mb-sm">Trip Summary</div>
          <div class="row q-col-gutter-md">
            <div class="col-6">
              <div class="text-caption text-grey-7">Total Expenses</div>
              <div class="text-h5 text-weight-bold text-primary">
                {{ formatCurrency(balanceData.totalExpenses, trip?.currency_code) }}
              </div>
            </div>
            <div class="col-6">
              <div class="text-caption text-grey-7">Settlement Status</div>
              <div
                class="text-h6 text-weight-bold"
                :class="balanceData.allSettled ? 'text-positive' : 'text-warning'"
              >
                {{ balanceData.allSettled ? 'All Settled ✓' : 'Pending' }}
              </div>
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Your Balance -->
      <q-card flat bordered class="q-mb-md" v-if="currentMemberBalance">
        <q-card-section class="bg-grey-1">
          <div class="text-overline text-grey-7">YOUR BALANCE</div>

          <div class="row items-center justify-between q-mt-sm">
            <div>
              <div class="text-caption text-grey-7">You paid</div>
              <div class="text-h6 text-weight-bold">
                {{ formatCurrency(currentMemberBalance.totalPaid, trip?.currency_code) }}
              </div>
            </div>

            <q-icon name="arrow_forward" size="sm" color="grey-5" />

            <div>
              <div class="text-caption text-grey-7">Your share</div>
              <div class="text-h6 text-weight-bold">
                {{ formatCurrency(currentMemberBalance.totalOwed, trip?.currency_code) }}
              </div>
            </div>
          </div>

          <q-separator class="q-my-md" />

          <!-- Net Balance Display -->
          <div class="text-center">
            <div class="text-caption text-grey-7">Net Balance</div>
            <div
              class="text-h4 text-weight-bold q-mt-xs"
              :class="getBalanceColor(currentMemberBalance.netBalance)"
            >
              {{ formatCurrency(Math.abs(currentMemberBalance.netBalance), trip?.currency_code) }}
            </div>
            <div
              class="text-subtitle2 q-mt-xs"
              :class="getBalanceColor(currentMemberBalance.netBalance)"
            >
              {{ getBalanceStatus(currentMemberBalance.netBalance) }}
            </div>
          </div>
        </q-card-section>
      </q-card>

      <!-- Who You Owe -->
      <div v-if="currentSettlements.owes.length > 0" class="q-mb-md">
        <div class="text-subtitle1 text-weight-bold q-mb-sm">You Owe</div>
        <q-card
          v-for="settlement in currentSettlements.owes"
          :key="`owe-${settlement.to}`"
          flat
          bordered
          class="q-mb-sm"
          clickable
          @click="openPaymentDialog(settlement)"
        >
          <q-card-section class="row items-center">
            <q-avatar color="negative" text-color="white" size="48px">
              <q-icon name="person" />
            </q-avatar>

            <div class="col q-ml-md">
              <div class="text-body1 text-weight-medium">{{ settlement.toName }}</div>
              <div class="text-caption text-grey-7">Tap to record payment</div>
            </div>

            <div class="text-right">
              <div class="text-h6 text-negative text-weight-bold">
                {{ formatCurrency(settlement.amount, trip?.currency_code) }}
              </div>
              <q-icon name="chevron_right" color="grey-5" />
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- Who Owes You -->
      <div v-if="currentSettlements.owed.length > 0" class="q-mb-md">
        <div class="text-subtitle1 text-weight-bold q-mb-sm">Owes You</div>
        <q-card
          v-for="settlement in currentSettlements.owed"
          :key="`owed-${settlement.from}`"
          flat
          bordered
          class="q-mb-sm"
        >
          <q-card-section class="row items-center">
            <q-avatar color="positive" text-color="white" size="48px">
              <q-icon name="person" />
            </q-avatar>

            <div class="col q-ml-md">
              <div class="text-body1 text-weight-medium">{{ settlement.fromName }}</div>
              <div class="text-caption text-grey-7">Waiting for payment</div>
            </div>

            <div class="text-h6 text-positive text-weight-bold">
              {{ formatCurrency(settlement.amount, trip?.currency_code) }}
            </div>
          </q-card-section>
        </q-card>
      </div>

      <!-- All Settled Message -->
      <q-card
        v-if="currentSettlements.owes.length === 0 && currentSettlements.owed.length === 0"
        flat
        bordered
        class="q-mb-md bg-positive text-white"
      >
        <q-card-section class="text-center">
          <q-icon name="check_circle" size="xl" class="q-mb-sm" />
          <div class="text-h6 text-weight-bold">All Settled Up!</div>
          <div class="text-body2 q-mt-xs">You don't owe anyone and nobody owes you</div>
        </q-card-section>
      </q-card>

      <!-- All Balances (Expandable) -->
      <q-expansion-item
        icon="people"
        label="View All Member Balances"
        header-class="bg-grey-2"
        expand-icon-class="text-primary"
      >
        <q-card>
          <q-list separator>
            <q-item v-for="balance in balanceData.memberBalances" :key="balance.memberId">
              <q-item-section avatar>
                <q-avatar
                  :color="
                    balance.netBalance > 0.01
                      ? 'positive'
                      : balance.netBalance < -0.01
                        ? 'negative'
                        : 'grey-5'
                  "
                  text-color="white"
                >
                  {{ balance.memberName.charAt(0).toUpperCase() }}
                </q-avatar>
              </q-item-section>

              <q-item-section>
                <q-item-label class="text-weight-medium">{{ balance.memberName }}</q-item-label>
                <q-item-label caption>
                  Paid {{ formatCurrency(balance.totalPaid, trip?.currency_code) }} • Owes
                  {{ formatCurrency(balance.totalOwed, trip?.currency_code) }}
                </q-item-label>
              </q-item-section>

              <q-item-section side>
                <q-item-label class="text-weight-bold" :class="getBalanceColor(balance.netBalance)">
                  {{ balance.netBalance > 0 ? '+' : ''
                  }}{{ formatCurrency(balance.netBalance, trip?.currency_code) }}
                </q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card>
      </q-expansion-item>
    </div>

    <!-- Payment Recording Dialog -->
    <q-dialog v-model="showPaymentDialog" persistent>
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Record Payment</div>
        </q-card-section>

        <q-card-section class="q-pt-none" v-if="selectedSettlement">
          <div class="text-body1 q-mb-md">
            You're paying
            <span class="text-weight-bold">{{ selectedSettlement.toName }}</span>
          </div>

          <q-input
            v-model.number="paymentAmount"
            type="number"
            label="Amount"
            :prefix="trip?.currency_code || 'PHP'"
            outlined
            :max="selectedSettlement.amount"
            :rules="[
              (val: number) => val > 0 || 'Must be positive',
              (val: number) =>
                val <= (selectedSettlement?.amount || 0) || 'Cannot exceed owed amount',
            ]"
          />

          <q-select
            v-model="paymentMethod"
            :options="paymentMethodOptions"
            label="Payment Method"
            outlined
            class="q-mt-md"
          />

          <q-input
            v-model="paymentNotes"
            type="textarea"
            label="Notes (Optional)"
            outlined
            rows="2"
            class="q-mt-md"
          />
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-7" v-close-popup />
          <q-btn
            flat
            label="Record Payment"
            color="primary"
            @click="recordPayment"
            :loading="recordingPayment"
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
import { useAuthStore } from 'src/stores/authStore';
import type { Trip } from 'src/types/expense';
import type { Expense, TripMember, ExpenseSplit } from 'src/types/expense';
import {
  calculateBalanceBreakdown,
  getSettlementsForMember,
  formatCurrency,
  getBalanceColor,
  getBalanceStatus,
  type BalanceBreakdown,
  type Settlement,
} from 'src/utils/settlementCalculator';

interface Props {
  tripId: string;
  trip: Trip | null;
}

const props = defineProps<Props>();
const $q = useQuasar();
const authStore = useAuthStore();

// State
const loading = ref(true);
const members = ref<TripMember[]>([]);
const expenses = ref<Expense[]>([]);
const splits = ref<ExpenseSplit[]>([]);
const currentUserId = ref<string>('');
const balanceData = ref<BalanceBreakdown>({
  memberBalances: [],
  settlements: [],
  totalExpenses: 0,
  allSettled: true,
});

// Payment dialog
const showPaymentDialog = ref(false);
const selectedSettlement = ref<Settlement | null>(null);
const paymentAmount = ref(0);
const paymentMethod = ref('Cash');
const paymentNotes = ref('');
const recordingPayment = ref(false);

const paymentMethodOptions = ['Cash', 'GCash', 'Bank Transfer', 'PayMaya', 'Other'];

// Computed
const currentMemberBalance = computed(() => {
  const currentMember = members.value.find((m) => m.user_id === currentUserId.value);
  if (!currentMember) return null;

  return balanceData.value.memberBalances.find((b) => b.memberId === currentMember.id);
});

const currentSettlements = computed(() => {
  const currentMember = members.value.find((m) => m.user_id === currentUserId.value);
  if (!currentMember) return { owes: [], owed: [] };

  return getSettlementsForMember(balanceData.value.settlements, currentMember.id);
});

// Methods
async function fetchData() {
  loading.value = true;

  try {
    // Get current user
    const user = authStore.user;
    if (user) currentUserId.value = user.id;

    // Fetch members
    const { data: memberData } = await supabase
      .from('members')
      .select('*')
      .eq('trip_id', props.tripId);

    members.value = (memberData as TripMember[]) || [];

    // Fetch expenses
    const { data: expenseData } = await supabase
      .from('expenses')
      .select('*')
      .eq('trip_id', props.tripId);

    expenses.value = (expenseData as Expense[]) || [];

    // Fetch splits
    if (expenses.value.length > 0) {
      const expenseIds = expenses.value.map((e) => e.id);
      const { data: splitData } = await supabase
        .from('expense_splits')
        .select('*')
        .in('expense_id', expenseIds);

      splits.value = (splitData as ExpenseSplit[]) || [];
    }

    // Calculate balances
    const expensesWithSplits = expenses.value.map((expense) => ({
      id: expense.id,
      paid_by_id: expense.paid_by_id,
      amount: expense.amount,
      splits: splits.value
        .filter((split) => split.expense_id === expense.id)
        .map((split) => ({
          member_id: split.member_id,
          share_amount: split.share_amount,
        })),
    }));

    balanceData.value = calculateBalanceBreakdown(expensesWithSplits, members.value);
  } catch (error) {
    console.error('Error fetching settlement data:', error);
    $q.notify({ type: 'negative', message: 'Failed to load settlement data' });
  } finally {
    loading.value = false;
  }
}

function openPaymentDialog(settlement: Settlement) {
  selectedSettlement.value = settlement;
  paymentAmount.value = settlement.amount;
  paymentMethod.value = 'Cash';
  paymentNotes.value = '';
  showPaymentDialog.value = true;
}

async function recordPayment() {
  if (!selectedSettlement.value || paymentAmount.value <= 0) return;

  recordingPayment.value = true;

  try {
    const { error } = await supabase.from('settlements').insert({
      trip_id: props.tripId,
      from_member_id: selectedSettlement.value.from,
      to_member_id: selectedSettlement.value.to,
      amount: paymentAmount.value,
      currency_code: props.trip?.currency_code ?? 'PHP',
      payment_method: paymentMethod.value,
      notes: paymentNotes.value || null,
      status: 'pending',
      paid_at: new Date().toISOString(),
    });

    if (error) throw error;

    $q.notify({
      type: 'positive',
      message: `Payment of ${formatCurrency(paymentAmount.value, props.trip?.currency_code)} recorded!`,
      caption: 'Settlement updated',
    });

    showPaymentDialog.value = false;
    await fetchData();
  } catch (error) {
    console.error('Error recording payment:', error);
    $q.notify({ type: 'negative', message: 'Failed to record payment' });
  } finally {
    recordingPayment.value = false;
  }
}

// Lifecycle
onMounted(() => {
  void fetchData();
});

// Export for parent component
defineExpose({ refresh: fetchData });
</script>

<style scoped>
.settlement-tab {
  min-height: 400px;
}
</style>
