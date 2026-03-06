<template>
  <q-page class="analytics-page gala-mesh-bg">

    <!-- ═══ Page Header ═══ -->
    <div class="q-px-lg q-pt-lg q-pb-md">
      <p class="text-caption text-weight-bold analytics-eyebrow q-mb-xs">Trip Insights</p>
      <h1 class="gala-display analytics-title q-mb-none">Spending<br>Analytics</h1>
    </div>

    <div class="q-px-lg q-pb-xl">
      <!-- Trip Selector -->
      <q-select
        v-model="selectedTripId"
        :options="tripOptions"
        label="Select Trip"
        outlined
        dense
        emit-value
        map-options
        class="analytics-trip-select q-mb-lg"
        bg-color="background"
      />

      <!-- Loading -->
      <div v-if="loading" class="flex flex-center q-py-xl">
        <q-spinner color="primary" size="lg" />
      </div>

      <!-- No trip selected -->
      <empty-state
        v-else-if="!selectedTripId"
        icon="analytics"
        title="Select a trip to view analytics"
        subtitle="Spending breakdowns and member insights will appear here"
      />

      <!-- Empty expenses -->
      <empty-state
        v-else-if="expenses.length === 0"
        icon="receipt_long"
        title="No expenses recorded"
        subtitle="Add expenses to this trip to see analytics"
      />

      <!-- ═══ Analytics Content ═══ -->
      <template v-else>

        <!-- Summary Stat Tiles -->
        <div class="stats-row q-mb-lg">
          <div class="stat-tile">
            <div class="stat-tile__icon" style="background: rgba(13,148,136,0.1)">
              <q-icon name="receipt_long" color="primary" size="20px" />
            </div>
            <div class="gala-display stat-tile__number">{{ formatAmount(totalSpent) }}</div>
            <div class="stat-tile__label">Total Spent</div>
          </div>
          <div class="stat-tile">
            <div class="stat-tile__icon" style="background: rgba(245,158,11,0.1)">
              <q-icon name="format_list_numbered" color="warning" size="20px" />
            </div>
            <div class="gala-display stat-tile__number">{{ expenses.length }}</div>
            <div class="stat-tile__label">Expenses</div>
          </div>
          <div class="stat-tile">
            <div class="stat-tile__icon" style="background: rgba(16,185,129,0.1)">
              <q-icon name="star" color="positive" size="20px" />
            </div>
            <div class="gala-display stat-tile__number stat-tile__number--sm">{{ topCategory }}</div>
            <div class="stat-tile__label">Top Category</div>
          </div>
        </div>

        <!-- Category Breakdown -->
        <div class="section-label q-mb-sm">Spending by Category</div>
        <div class="analytics-panel q-mb-lg">
          <!-- Category bars -->
          <div
            v-for="(item, index) in categoryBreakdown"
            :key="item.label"
            class="cat-row"
          >
            <div class="cat-row__dot" :style="{ background: brandColors[index % brandColors.length] }" />
            <div class="cat-row__label">{{ item.label }}</div>
            <div class="cat-row__bar-wrap">
              <div
                class="cat-row__bar"
                :style="{
                  width: `${(item.value / totalSpent) * 100}%`,
                  background: brandColors[index % brandColors.length]
                }"
              />
            </div>
            <div class="cat-row__amount">{{ formatAmount(item.value) }}</div>
            <div class="cat-row__pct">{{ Math.round((item.value / totalSpent) * 100) }}%</div>
          </div>

          <!-- Doughnut chart -->
          <div class="chart-wrap q-mt-md">
            <canvas ref="categoryChart" height="180" />
          </div>
        </div>

        <!-- Member Spending -->
        <div class="section-label q-mb-sm">Paid by Member</div>
        <div class="analytics-panel">
          <canvas ref="memberChart" height="180" />
        </div>

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
import EmptyState from 'src/components/shared/EmptyState.vue';

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
.analytics-page {
  min-height: 100vh;
  background-color: var(--surface);
}

.analytics-eyebrow {
  color: $primary;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.analytics-title {
  font-size: clamp(2.25rem, 8vw, 3.5rem);
  line-height: 1.05;
  color: var(--on-background);
}

.analytics-trip-select {
  :deep(.q-field__control) {
    border-radius: var(--gala-radius-pill);
  }
}

// ─── Stats ────────────────────────────────────────────────────────────────────
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.stat-tile {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  &__icon {
    width: 36px;
    height: 36px;
    border-radius: var(--gala-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__number {
    font-size: 1.4rem;
    line-height: 1;
    color: var(--on-background);
    font-variant-numeric: tabular-nums;

    &--sm {
      font-size: 0.85rem;
      font-family: 'Plus Jakarta Sans', sans-serif;
      font-weight: 700;
      line-height: 1.3;
    }
  }

  &__label {
    font-size: 0.72rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: var(--muted);
  }
}

// ─── Section label ────────────────────────────────────────────────────────────
.section-label {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--muted);
}

// ─── Analytics panel ─────────────────────────────────────────────────────────
.analytics-panel {
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  padding: 16px;
}

// ─── Category rows ────────────────────────────────────────────────────────────
.cat-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  &__dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  &__label {
    font-size: 0.8125rem;
    font-weight: 500;
    color: var(--on-background);
    min-width: 80px;
    flex-shrink: 0;
  }

  &__bar-wrap {
    flex: 1;
    height: 6px;
    background: var(--surface);
    border-radius: 3px;
    overflow: hidden;
  }

  &__bar {
    height: 100%;
    border-radius: 3px;
    transition: width 0.6s var(--ease-out-expo);
    min-width: 4px;
  }

  &__amount {
    font-size: 0.78rem;
    font-weight: 600;
    color: var(--on-background);
    text-align: right;
    min-width: 64px;
    font-variant-numeric: tabular-nums;
    flex-shrink: 0;
  }

  &__pct {
    font-size: 0.72rem;
    font-weight: 600;
    color: var(--muted);
    min-width: 32px;
    text-align: right;
    flex-shrink: 0;
  }
}

.chart-wrap {
  border-top: 1px solid var(--border);
  padding-top: 12px;
}
</style>
