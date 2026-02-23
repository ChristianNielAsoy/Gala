<template>
  <q-page class="q-pa-md bg-surface">
    <q-card flat bordered>
      <q-card-section>
        <div class="text-h6">Expense Analytics</div>
        <q-select
          v-model="selectedTripId"
          :options="tripOptions"
          label="Select Trip"
          outlined
          dense
          emit-value
          map-options
          class="q-mb-md"
        />
        <div v-if="loading" class="flex flex-center q-pa-xl">
          <q-spinner color="primary" size="lg" />
        </div>
        <div v-else-if="!selectedTripId">
          <div class="text-grey-6">Select a trip to view analytics.</div>
        </div>
        <div v-else>
          <div v-if="expenses.length === 0" class="text-center q-pa-xl">
            <q-icon name="receipt_long" size="48px" color="grey-4" />
            <div class="text-grey-6 q-mt-sm">No expenses recorded for this trip yet.</div>
          </div>
          <template v-else>
            <q-card flat bordered class="q-mb-md">
              <q-card-section>
                <div class="text-subtitle1 q-mb-sm">Expenses by Category</div>
                <canvas ref="categoryChart" height="200"></canvas>
              </q-card-section>
            </q-card>
            <q-card flat bordered class="q-mb-md">
              <q-card-section>
                <div class="text-subtitle1 q-mb-sm">Expenses by Member</div>
                <canvas ref="memberChart" height="200"></canvas>
              </q-card-section>
            </q-card>
          </template>
        </div>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import Chart from 'chart.js/auto';
import type { Chart as ChartType } from 'chart.js';
import type { Trip } from 'src/types/trip';
import type { Expense, TripMember } from 'src/types/expense';

const $q = useQuasar();
const loading = ref(false);
const trips = ref<Trip[]>([]);
const tripOptions = ref<{ label: string; value: string }[]>([]);
const selectedTripId = ref<string | null>(null);
const expenses = ref<Expense[]>([]);
const members = ref<TripMember[]>([]);

const categoryChart = ref<HTMLCanvasElement | null>(null);
const memberChart = ref<HTMLCanvasElement | null>(null);
let categoryChartInstance: ChartType | null = null;
let memberChartInstance: ChartType | null = null;

async function fetchTrips() {
  loading.value = true;
  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) return;

    const { data: memberData } = await supabase
      .from('members')
      .select('trip_id')
      .eq('user_id', user.id);

    const tripIds = memberData?.map((m) => m.trip_id) || [];

    if (tripIds.length > 0) {
      const { data: tripsData } = await supabase
        .from('trips')
        .select('*')
        .in('id', tripIds)
        .order('start_date', { ascending: false });

      trips.value = (tripsData as Trip[]) || [];
      tripOptions.value = trips.value.map((t) => ({ label: t.name, value: t.id }));

      // Auto-select first trip if available
      if (trips.value.length > 0 && !selectedTripId.value) {
        selectedTripId.value = trips.value[0]!.id;
      }
    } else {
      trips.value = [];
      tripOptions.value = [];
    }
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load trips.' });
  } finally {
    loading.value = false;
  }
}

async function fetchTripData(tripId: string) {
  loading.value = true;
  try {
    const { data: expenseData } = await supabase.from('expenses').select('*').eq('trip_id', tripId);
    expenses.value = (expenseData as Expense[]) || [];
    const { data: memberData } = await supabase.from('members').select('*').eq('trip_id', tripId);
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
  // Destroy previous charts
  if (categoryChartInstance) categoryChartInstance.destroy();
  if (memberChartInstance) memberChartInstance.destroy();

  // Expenses by Category
  const categoryTotals: Record<string, number> = {};
  for (const exp of expenses.value) {
    categoryTotals[exp.category] = (categoryTotals[exp.category] || 0) + exp.amount;
  }
  const categoryLabels = Object.keys(categoryTotals);
  const categoryData = Object.values(categoryTotals);
  if (categoryChart.value) {
    categoryChartInstance = new Chart(categoryChart.value, {
      type: 'pie',
      data: {
        labels: categoryLabels,
        datasets: [
          {
            data: categoryData,
            backgroundColor: [
              '#42A5F5',
              '#66BB6A',
              '#FFA726',
              '#AB47BC',
              '#FF7043',
              '#26A69A',
              '#D4E157',
              '#8D6E63',
              '#789262',
              '#EC407A',
            ],
          },
        ],
      },
      options: { responsive: true, plugins: { legend: { position: 'bottom' } } },
    });
  }

  // Expenses by Member
  const memberTotals: Record<string, number> = {};
  for (const exp of expenses.value) {
    const member = members.value.find((m) => m.id === exp.paid_by_id);
    const name = member ? member.name : 'Unknown';
    memberTotals[name] = (memberTotals[name] || 0) + exp.amount;
  }
  const memberLabels = Object.keys(memberTotals);
  const memberData = Object.values(memberTotals);
  if (memberChart.value) {
    memberChartInstance = new Chart(memberChart.value, {
      type: 'bar',
      data: {
        labels: memberLabels,
        datasets: [{ data: memberData, backgroundColor: '#42A5F5' }],
      },
      options: { responsive: true, plugins: { legend: { display: false } } },
    });
  }
}

watch(selectedTripId, (tripId) => {
  if (tripId) void fetchTripData(tripId);
});

onMounted(() => {
  void fetchTrips();
});
</script>
