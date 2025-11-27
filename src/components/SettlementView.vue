<template>
  <div class="settlement-view q-pa-md">
    <!-- Loading State -->
    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner color="primary" size="lg" />
      <p class="q-mt-md text-grey-7">Calculating settlements...</p>
    </div>

    <!-- Settlement Content -->
    <div v-else>
      <!-- Your Balance Card -->
      <q-card class="q-mb-md shadow-2" v-if="currentMemberId">
        <q-card-section class="bg-primary text-white">
          <div class="text-subtitle2">Your Balance</div>
          <div class="text-h4 text-weight-bold q-mt-sm">
            <span v-if="yourBalance > 0">+</span>{{ currencyCode }} {{ Math.abs(yourBalance).toFixed(2) }}
          </div>
          <div class="text-subtitle1 q-mt-xs">
            <span v-if="yourBalance > 0">You are owed</span>
            <span v-else-if="yourBalance < 0">You owe</span>
            <span v-else>You're all settled up! 🎉</span>
          </div>
        </q-card-section>
      </q-card>

      <!-- Settlements to Make (You Owe) -->
      <q-card v-if="settlementsToPay.length > 0" class="q-mb-md shadow-2">
        <q-card-section>
          <div class="text-h6 text-negative q-mb-md">
            <q-icon name="arrow_upward" /> You Need to Pay
          </div>

          <q-list separator>
            <q-item
              v-for="settlement in settlementsToPay"
              :key="settlement.to"
              class="q-py-md"
            >
              <q-item-section avatar>
                <q-avatar color="negative" text-color="white" size="48px">
                  {{ settlement.toName.charAt(0).toUpperCase() }}
                </q-avatar>
              </q-item-section>

              <q-item-section>
                <q-item-label class="text-weight-bold">
                  Pay {{ settlement.toName }}
                </q-item-label>
                <q-item-label caption>
                  Settle your share of expenses
                </q-item-label>
              </q-item-section>

              <q-item-section side>
                <q-item-label class="text-h6 text-negative text-weight-bold">
                  {{ currencyCode }} {{ settlement.amount.toFixed(2) }}
                </q-item-label>
                <q-btn
                  flat
                  dense
                  color="primary"
                  label="Mark Paid"
                  size="sm"
                  @click="markAsPaid(settlement)"
                  class="q-mt-xs"
                />
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>

      <!-- Settlements to Receive (You Are Owed) -->
      <q-card v-if="settlementsToReceive.length > 0" class="q-mb-md shadow-2">
        <q-card-section>
          <div class="text-h6 text-positive q-mb-md">
            <q-icon name="arrow_downward" /> You Will Receive
          </div>

          <q-list separator>
            <q-item
              v-for="settlement in settlementsToReceive"
              :key="settlement.from"
              class="q-py-md"
            >
              <q-item-section avatar>
                <q-avatar color="positive" text-color="white" size="48px">
                  {{ settlement.fromName.charAt(0).toUpperCase() }}
                </q-avatar>
              </q-item-section>

              <q-item-section>
                <q-item-label class="text-weight-bold">
                  {{ settlement.fromName }} owes you
                </q-item-label>
                <q-item-label caption>
                  Waiting for payment
                </q-item-label>
              </q-item-section>

              <q-item-section side>
                <q-item-label class="text-h6 text-positive text-weight-bold">
                  {{ currencyCode }} {{ settlement.amount.toFixed(2) }}
                </q-item-label>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>

      <!-- All Trip Settlements -->
      <q-card v-if="allSettlements.length > 0" class="shadow-2">
        <q-card-section>
          <div class="text-h6 q-mb-md">
            All Trip Settlements
          </div>

          <q-list separator>
            <q-item
              v-for="(settlement, index) in allSettlements"
              :key="index"
              class="q-py-sm"
            >
              <q-item-section>
                <div class="row items-center">
                  <q-avatar size="32px" color="grey-4" text-color="grey-8" class="q-mr-sm">
                    {{ settlement.fromName.charAt(0) }}
                  </q-avatar>
                  <span class="text-weight-medium">{{ settlement.fromName }}</span>

                  <q-icon name="arrow_forward" class="q-mx-md" color="grey-5" />

                  <q-avatar size="32px" color="grey-4" text-color="grey-8" class="q-mr-sm">
                    {{ settlement.toName.charAt(0) }}
                  </q-avatar>
                  <span class="text-weight-medium">{{ settlement.toName }}</span>

                  <q-space />

                  <span class="text-weight-bold">
                    {{ currencyCode }} {{ settlement.amount.toFixed(2) }}
                  </span>
                </div>
              </q-item-section>
            </q-item>
          </q-list>
        </q-card-section>
      </q-card>

      <!-- All Settled State -->
      <q-card v-if="allSettlements.length === 0" class="shadow-2">
        <q-card-section class="text-center q-py-xl">
          <q-icon name="check_circle" size="xl" color="positive" />
          <div class="text-h6 q-mt-md">All Settled!</div>
          <div class="text-grey-7 q-mt-sm">
            Everyone has paid their share. No settlements needed.
          </div>
        </q-card-section>
      </q-card>

      <!-- Member Balances Summary (Expandable) -->
      <q-expansion-item
        icon="people"
        label="Member Balances"
        caption="See detailed balance for each member"
        class="q-mt-md"
      >
        <q-card>
          <q-card-section>
            <q-list separator>
              <q-item v-for="balance in memberBalances" :key="balance.memberId">
                <q-item-section avatar>
                  <q-avatar
                    :color="balance.balance > 0 ? 'positive' : balance.balance < 0 ? 'negative' : 'grey'"
                    text-color="white"
                  >
                    {{ balance.memberName.charAt(0).toUpperCase() }}
                  </q-avatar>
                </q-item-section>

                <q-item-section>
                  <q-item-label>{{ balance.memberName }}</q-item-label>
                </q-item-section>

                <q-item-section side>
                  <q-item-label
                    class="text-weight-bold"
                    :class="getBalanceColorClass(balance.balance)"
                  >
                    <span v-if="balance.balance > 0">+</span>
                    {{ currencyCode }} {{ Math.abs(balance.balance).toFixed(2) }}
                  </q-item-label>
                  <q-item-label caption>
                    <span v-if="balance.balance > 0">is owed</span>
                    <span v-else-if="balance.balance < 0">owes</span>
                    <span v-else>settled</span>
                  </q-item-label>
                </q-item-section>
              </q-item>
            </q-list>
          </q-card-section>
        </q-card>
      </q-expansion-item>
    </div>

    <!-- Mark as Paid Dialog -->
    <q-dialog v-model="showPaymentDialog">
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Record Payment</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <p>
            Confirm payment of <strong>{{ currencyCode }} {{ selectedSettlement?.amount.toFixed(2) }}</strong>
            to <strong>{{ selectedSettlement?.toName }}</strong>?
          </p>

          <q-select
            v-model="paymentMethod"
            :options="paymentMethods"
            label="Payment Method"
            outlined
            dense
            class="q-mt-md"
          />
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-7" v-close-popup />
          <q-btn
            flat
            label="Confirm Payment"
            color="primary"
            @click="confirmPayment"
            :loading="processingPayment"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useQuasar } from 'quasar';
import {
  calculateMemberBalances,
  simplifySettlements,
  getBalanceColorClass,
  type MemberBalance,
  type Settlement,
  type ExpenseWithSplits
} from 'src/utils/settlementCalculator';

const props = defineProps<{
  expenses: ExpenseWithSplits[];
  members: { id: string; name: string }[];
  currentMemberId?: string;
  currencyCode: string;
  loading?: boolean;
}>();

const $q = useQuasar();

// Payment dialog state
const showPaymentDialog = ref(false);
const selectedSettlement = ref<Settlement | null>(null);
const paymentMethod = ref('Cash');
const processingPayment = ref(false);

const paymentMethods = ['Cash', 'GCash', 'Bank Transfer', 'PayMaya', 'Other'];

// Computed settlements
const memberBalances = computed((): MemberBalance[] => {
  return calculateMemberBalances(props.expenses, props.members);
});

const allSettlements = computed((): Settlement[] => {
  return simplifySettlements(memberBalances.value);
});

const yourBalance = computed((): number => {
  if (!props.currentMemberId) return 0;
  const balance = memberBalances.value.find(b => b.memberId === props.currentMemberId);
  return balance?.balance || 0;
});

const settlementsToPay = computed((): Settlement[] => {
  if (!props.currentMemberId) return [];
  return allSettlements.value.filter(s => s.from === props.currentMemberId);
});

const settlementsToReceive = computed((): Settlement[] => {
  if (!props.currentMemberId) return [];
  return allSettlements.value.filter(s => s.to === props.currentMemberId);
});

// Methods
function markAsPaid(settlement: Settlement) {
  selectedSettlement.value = settlement;
  showPaymentDialog.value = true;
}

async function confirmPayment() {
  processingPayment.value = true;

  try {
    // TODO: Save payment record to database
    // For now, just show success message
    await new Promise(resolve => setTimeout(resolve, 500));

    $q.notify({
      type: 'positive',
      message: `Payment of ${props.currencyCode} ${selectedSettlement.value?.amount.toFixed(2)} recorded!`,
      icon: 'check_circle'
    });

    showPaymentDialog.value = false;
    selectedSettlement.value = null;

  } catch (error) {
    $q.notify({
      type: 'negative',
      message: error.message || 'Failed to record payment'
    });
  } finally {
    processingPayment.value = false;
  }
}
</script>

<style scoped>
.settlement-view {
  max-width: 800px;
  margin: 0 auto;
}
</style>
