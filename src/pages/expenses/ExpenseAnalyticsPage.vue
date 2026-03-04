<template>
  <q-page class="bg-surface">
    <!-- Header -->
    <div class="q-pa-md bg-surface">
      <div class="text-h5 text-weight-bold">Expense Analytics</div>
      <div class="text-body2 text-grey-6 q-mt-xs">Spending breakdown for your trips</div>
    </div>

    <div class="q-pa-md q-pt-none">
      <!-- Trip Selector -->
      <q-card flat bordered class="q-mb-md">
        <q-card-section class="q-pb-sm">
          <q-select
            v-model="selectedTripId"
            :options="tripOptions"
            label="Select Trip"
            outlined
            dense
            emit-value
            map-options
          />
        </q-card-section>
      </q-card>

      <!-- Loading -->
      <div v-if="loading" class="flex flex-center q-pa-xl">
        <q-spinner color="primary" size="lg" />
      </div>

      <!-- No trip selected -->
      <div v-else-if="!selectedTripId" class="text-center q-pa-xl">
        <q-icon name="analytics" size="56px" color="grey-4" />
        <div class="text-h6 text-grey-6 q-mt-md">Select a trip to view analytics</div>
        <div class="text-body2 text-grey-5 q-mt-xs">Spending breakdowns and member insights will appear here</div>
      </div>

      <!-- Empty expenses -->
      <div v-else-if="expenses.length === 0" class="text-center q-pa-xl">
        <q-icon name="receipt_long" size="56px" color="grey-4" />
        <div class="text-h6 text-grey-6 q-mt-md">No expenses recorded</div>
        <div class="text-body2 text-grey-5 q-mt-xs">Add expenses to this trip to see analytics</div>
      </div>

      <!-- Analytics Content -->
      <template v-else>
        <!-- Summary Stats -->
        <div class="row q-col-gutter-md q-mb-md">
          <div class="col-12 col-sm-4">
            <q-card flat bordered class="stat-card q-pa-md">
              <div class="row items-center no-wrap">
                <div class="stat-icon-wrap stat-icon-wrap--primary q-mr-md">
                  <q-icon name="receipt_long" size="22px" color="primary" />
                </div>
                <div>
                  <div class="text-h5 text-weight-bold stat-number">{{ formatAmount(totalSpent) }}</div>
                  <div class="text-caption text-grey-6">Total Spent</div>
                </div>
              </div>
            </q-card>
          </div>
          <div class="col-12 col-sm-4">
            <q-card flat bordered class="stat-card q-pa-md">
              <div class="row items-center no-wrap">
                <div class="stat-icon-wrap stat-icon-wrap--accent q-mr-md">
                  <q-icon name="format_list_numbered" size="22px" color="accent" />
                </div>
                <div>
                  <div class="text-h5 text-weight-bold stat-number">{{ expenses.length }}</div>
                  <div class="text-caption text-grey-6">Expenses</div>
                </div>
              </div>
            </q-card>
          </div>
          <div class="col-12 col-sm-4">
            <q-card flat bordered class="stat-card q-pa-md">
              <div class="row items-center no-wrap">
                <div class="stat-icon-wrap stat-icon-wrap--positive q-mr-md">
                  <q-icon name="star" size="22px" color="positive" />
                </div>
                <div>
                  <div class="text-h5 text-weight-bold stat-number text-truncate" style="max-width: 120px">
                    {{ topCategory }}
                  </div>
                  <div class="text-caption text-grey-6">Top Category</div>
                </div>
              </div>
            </q-card>
          </div>
        </div>

        <!-- Expenses by Category (Pie) -->
        <q-card flat bordered class="q-mb-md">
          <q-card-section>
            <div class="text-subtitle1 text-weight-bold q-mb-sm">Spending by Category</div>
            <!-- Category breakdown list -->
            <div class="q-mb-md">
              <div
                v-for="(item, index) in categoryBreakdown"
                :key="item.label"
                class="row items-center q-mb-sm"
              >
                <div class="col-auto q-mr-sm">
                  <div class="category-dot" :style="{ background: brandColors[index % brandColors.length] }" />
                </div>
                <div class="col text-body2">{{ item.label }}</div>
                <div class="col-auto text-body2 text-weight-medium q-mr-sm">{{ formatAmount(item.value) }}</div>
                <div class="col-auto" style="width: 80px">
                  <q-linear-progress
                    :value="item.value / totalSpent"
                    :style="{ '--q-color-primary': brandColors[index % brandColors.length] }"
                    color="primary"
                    track-color="grey-3"
                    rounded
                    size="6px"
                  />
                </div>
                <div class="col-auto text-caption text-grey-6 q-ml-sm" style="min-width: 34px; text-align: right">
                  {{ Math.round((item.value / totalSpent) * 100) }}%
                </div>
              </div>
            </div>
            <canvas ref="categoryChart" height="180"></canvas>
          </q-card-section>
        </q-card>

        <!-- Expenses by Member (Bar) -->
        <q-card flat bordered class="q-mb-md">
          <q-card-section>
            <div class="text-subtitle1 text-weight-bold q-mb-sm">Amount Paid by Member</div>
            <canvas ref="memberChart" height="180"></canvas>
          </q-card-section>
        </q-card>
      </template>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, nextTick } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useTripStore } from 'src/stores/tripStore';
import Chart from 'chart.js/auto';
import type { Chart as ChartType } from 'chart.js';
import type { Expense, TripMember } from 'src/types/expense';

const $q = useQuasar();
const tripStore = useTripStore();

const loading = ref(false);
const selectedTripId = ref<string | null>(null);
const expenses = ref<Expense[]>([]);
const members = ref<TripMember[]>([]);

const categoryChart = ref<HTMLCanvasElement | null>(null);
const memberChart = ref<HTMLCanvasElement | null>(null);
let categoryChartInstance: ChartType | null = null;
let memberChartInstance: ChartType | null = null;

// Brand-aligned color palette
const brandColors = [
  '#0D9488', // teal (primary)
  '#F97316', // coral (accent)
  '#16A34A', // green (positive)
  '#0EA5E9', // sky (info)
  '#D97706', // amber (warning)
  '#64748B', // slate (secondary)
  '#14B8A6', // teal-500 (tertiary)
  '#DC2626', // red (negative)
  '#8B5CF6', // purple
  '#EC4899', // pink
];

const tripOptions = computed(() => tripStore.trips.map((t) => ({ label: t.name, value: t.id })));

const totalSpent = computed(() => expenses.value.reduce((sum, e) => sum + e.amount, 0));

const categoryBreakdown = computed(() => {
  const totals: Record<string, number> = {};
  for (const exp of expenses.value) {
    totals[exp.category] = (totals[exp.category] || 0) + exp.amount;
  }
  return Object.entries(totals)
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value);
});

const topCategory = computed(() => categoryBreakdown.value[0]?.label ?? '—');

// Use trip currency if available, fallback to PHP
const selectedTrip = computed(() => tripStore.trips.find((t) => t.id === selectedTripId.value));

function formatAmount(amount: number): string {
  const symbols: Record<string, string> = {
    PHP: '₱', USD: '$', EUR: '€', JPY: '¥', GBP: '£',
    AUD: 'A$', CAD: 'C$', SGD: 'S$', THB: '฿',
  };
  const code = selectedTrip.value?.currency_code ?? 'PHP';
  const symbol = symbols[code] ?? code + ' ';
  return `${symbol}${amount.toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

async function fetchTripData(tripId: string) {
  loading.value = true;
  try {
    const [{ data: expenseData }, { data: memberData }] = await Promise.all([
      supabase.from('expenses').select('*').eq('trip_id', tripId),
      supabase.from('members').select('*').eq('trip_id', tripId),
    ]);
    expenses.value = (expenseData as Expense[]) || [];
    members.value = (memberData as TripMember[]) || [];
    await nextTick();
    renderCharts();
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load trip data.' });
  } finally {
    loading.value = false;
  }
}

function renderCharts() {
  if (categoryChartInstance) categoryChartInstance.destroy();
  if (memberChartInstance) memberChartInstance.destroy();

  // Expenses by Category — Doughnut
  const categoryLabels = categoryBreakdown.value.map((c) => c.label);
  const categoryData = categoryBreakdown.value.map((c) => c.value);
  const categoryColors = categoryLabels.map((_, i) => brandColors[i % brandColors.length]!);

  if (categoryChart.value) {
    categoryChartInstance = new Chart(categoryChart.value, {
      type: 'doughnut',
      data: {
        labels: categoryLabels,
        datasets: [{ data: categoryData, backgroundColor: categoryColors, borderWidth: 2 }],
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${formatAmount(ctx.raw as number)}`,
            },
          },
        },
      },
    });
  }

  // Expenses by Member — Horizontal Bar
  const memberTotals: Record<string, number> = {};
  for (const exp of expenses.value) {
    const member = members.value.find((m) => m.id === exp.paid_by_id);
    const name = member ? member.name : 'Unknown';
    memberTotals[name] = (memberTotals[name] || 0) + exp.amount;
  }
  const memberLabels = Object.keys(memberTotals);
  const memberData = Object.values(memberTotals);
  const memberColors = memberLabels.map((_, i) => brandColors[i % brandColors.length]!);

  if (memberChart.value) {
    memberChartInstance = new Chart(memberChart.value, {
      type: 'bar',
      data: {
        labels: memberLabels,
        datasets: [{
          label: 'Amount Paid',
          data: memberData,
          backgroundColor: memberColors,
          borderRadius: 6,
        }],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx) => ` ${formatAmount(ctx.raw as number)}`,
            },
          },
        },
        scales: {
          x: { grid: { display: false }, ticks: { display: false } },
          y: { grid: { display: false } },
        },
      },
    });
  }
}

watch(selectedTripId, (tripId) => {
  if (tripId) void fetchTripData(tripId);
});

onMounted(async () => {
  try {
    await tripStore.fetchTrips();
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load trips.' });
  }
  if (tripStore.trips.length > 0 && !selectedTripId.value) {
    selectedTripId.value = tripStore.trips[0]!.id;
  }
});
</script>

<style scoped lang="scss">
.stat-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--gala-shadow-md);
  }
}

.stat-number {
  color: #1e293b;
  line-height: 1.1;
}

.stat-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: var(--gala-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--primary  { background: rgba(13, 148, 136, 0.1); }
  &--accent   { background: rgba(249, 115, 22, 0.1); }
  &--positive { background: rgba(22, 163, 74, 0.1); }
}

.category-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}
</style>
