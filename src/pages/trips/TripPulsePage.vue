<template>
  <q-page class="pulse-page gala-mesh-bg">

    <!-- ═══ Floating toolbar ═══ -->
    <div class="pulse-toolbar">
      <q-btn flat round icon="arrow_back" class="pulse-back-btn" @click="router.back()" aria-label="Go back" />
      <div class="pulse-toolbar__pill gala-panel" v-if="trip">
        <q-icon name="monitor_heart" size="14px" style="color: var(--primary)" />
        <span class="gala-display pulse-toolbar__name">{{ trip.name }}</span>
      </div>
    </div>

    <!-- ═══ Loading ═══ -->
    <div v-if="loading" class="pulse-loading">
      <div class="pulse-loading__ring">
        <svg viewBox="0 0 80 80" class="pulse-loading__svg">
          <circle cx="40" cy="40" r="30" fill="none" stroke="var(--border)" stroke-width="4" />
          <circle cx="40" cy="40" r="30" fill="none" stroke="#0d9488" stroke-width="4"
            stroke-linecap="round" stroke-dasharray="50 140"
            transform="rotate(-90 40 40)" class="pulse-loading__arc" />
        </svg>
      </div>
      <p class="q-mt-md text-caption" style="color: var(--muted)">Reading trip pulse...</p>
    </div>

    <template v-else-if="trip">
      <div class="pulse-content q-px-md q-pb-xl">

        <!-- ═══ Hero ═══ -->
        <div class="pulse-hero q-mb-lg">
          <div class="text-caption text-weight-medium q-mb-xs pulse-hero__eyebrow">Trip Pulse</div>
          <div class="gala-display pulse-hero__name">{{ trip.name }}</div>
          <div class="row items-center q-gutter-sm q-mt-sm">
            <div class="pulse-chip pulse-chip--day" v-if="dayProgress">
              <q-icon name="calendar_today" size="11px" />
              Day {{ dayProgress.current }} of {{ dayProgress.total }}
            </div>
            <div class="pulse-chip pulse-chip--countdown" v-if="countdown !== null && countdown >= 0">
              <q-icon name="timer" size="11px" />
              {{ countdownLabel }}
            </div>
            <div class="pulse-chip pulse-chip--live">
              <span class="pulse-live-dot" />
              Live
            </div>
          </div>
        </div>

        <!-- ═══ Spending Rhythm Wave ═══ -->
        <div class="gala-panel pulse-wave-card q-pa-lg q-mb-md">
          <div class="row items-center justify-between q-mb-md">
            <div>
              <div class="text-subtitle2 text-weight-bold">Spending Rhythm</div>
              <div class="text-caption" style="color: var(--muted)">Daily expense pattern</div>
            </div>
            <q-icon name="show_chart" size="20px" style="color: #0d9488; opacity: 0.6" />
          </div>

          <!-- SVG Wave -->
          <div class="pulse-wave-container">
            <svg
              class="pulse-wave-svg"
              viewBox="0 0 400 100"
              preserveAspectRatio="none"
              aria-hidden="true"
            >
              <defs>
                <linearGradient id="wave-fill-grad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#0d9488" stop-opacity="0.25" />
                  <stop offset="100%" stop-color="#0d9488" stop-opacity="0.0" />
                </linearGradient>
                <filter id="glow">
                  <feGaussianBlur stdDeviation="2" result="blur" />
                  <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
                </filter>
              </defs>

              <!-- Flat baseline when no data -->
              <line
                v-if="wavePoints.length < 2"
                x1="20" y1="80" x2="380" y2="80"
                stroke="var(--border)" stroke-width="2" stroke-dasharray="4 4"
              />

              <!-- Fill under wave -->
              <path
                v-if="waveFillPath"
                :d="waveFillPath"
                fill="url(#wave-fill-grad)"
                class="wave-fill"
              />

              <!-- Wave line -->
              <path
                v-if="waveLinePath"
                :d="waveLinePath"
                fill="none"
                stroke="#0d9488"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
                class="wave-line"
              />

              <!-- Data points -->
              <circle
                v-for="(pt, i) in wavePoints"
                :key="i"
                :cx="pt.x"
                :cy="pt.y"
                r="3"
                :fill="pt.isToday ? '#f59e0b' : '#0d9488'"
                :opacity="pt.isToday ? 1 : 0.5"
              />

              <!-- Today dot (pulsing) -->
              <circle
                v-if="todayPoint"
                :cx="todayPoint.x"
                :cy="todayPoint.y"
                r="6"
                fill="#f59e0b"
                fill-opacity="0.25"
                class="today-pulse-ring"
              />
              <circle
                v-if="todayPoint"
                :cx="todayPoint.x"
                :cy="todayPoint.y"
                r="4"
                fill="#f59e0b"
                filter="url(#glow)"
              />
            </svg>

            <!-- Day labels -->
            <div class="pulse-wave-labels" v-if="waveDayLabels.length > 1">
              <span
                v-for="(label, i) in waveDayLabels"
                :key="i"
                class="wave-label"
                :class="{ 'wave-label--today': label.isToday }"
              >{{ label.text }}</span>
            </div>
          </div>

          <!-- Stats row -->
          <div class="row q-gutter-md q-mt-md">
            <div class="col pulse-stat">
              <div class="pulse-stat__label">Today</div>
              <div class="pulse-stat__value gala-mono" :class="todayTotal > 0 ? 'text-primary' : ''">
                {{ formatAmount(todayTotal) }}
              </div>
            </div>
            <div class="col pulse-stat">
              <div class="pulse-stat__label">Daily avg</div>
              <div class="pulse-stat__value gala-mono">{{ formatAmount(dailyAvg) }}</div>
            </div>
            <div class="col pulse-stat">
              <div class="pulse-stat__label">Total</div>
              <div class="pulse-stat__value gala-mono text-weight-bold">{{ formatAmount(totalExpenses) }}</div>
            </div>
          </div>
        </div>

        <!-- ═══ Budget Arc ═══ -->
        <div class="gala-panel pulse-budget-card q-pa-lg q-mb-md" v-if="trip.budget_amount">
          <div class="row items-center q-gutter-lg">
            <div class="pulse-arc-wrap">
              <svg viewBox="0 0 88 88" class="pulse-arc-svg">
                <!-- Track -->
                <circle cx="44" cy="44" r="36" fill="none" stroke="var(--border)" stroke-width="7" />
                <!-- Progress -->
                <circle
                  cx="44" cy="44" r="36"
                  fill="none"
                  :stroke="budgetArcColor"
                  stroke-width="7"
                  stroke-linecap="round"
                  :stroke-dasharray="`${budgetArcLength} 226`"
                  transform="rotate(-90 44 44)"
                  class="arc-progress"
                />
                <!-- Center text -->
                <text x="44" y="40" text-anchor="middle" class="arc-pct" :fill="budgetArcColor">
                  {{ budgetPct }}%
                </text>
                <text x="44" y="55" text-anchor="middle" class="arc-sub" fill="var(--muted-color, #94a3b8)">
                  used
                </text>
              </svg>
            </div>
            <div class="col">
              <div class="text-caption q-mb-xs" style="color: var(--muted)">Budget tracker</div>
              <div class="gala-display pulse-budget__spent">{{ formatAmount(totalExpenses) }}</div>
              <div class="text-caption q-mb-sm" style="color: var(--muted)">
                of {{ formatAmount(trip.budget_amount) }}
              </div>
              <q-badge
                :color="budgetPct >= 100 ? 'negative' : budgetPct >= 75 ? 'warning' : 'positive'"
                class="pulse-budget__badge"
              >{{ budgetStatusLabel }}</q-badge>
            </div>
          </div>
        </div>

        <!-- ═══ Barkada Status ═══ -->
        <div class="text-subtitle2 text-weight-bold q-mb-sm q-mt-md">Your Barkada</div>
        <div class="pulse-members-grid q-mb-md" v-if="memberStats.length">
          <div
            v-for="member in memberStats"
            :key="member.id"
            class="pulse-member-card gala-panel"
          >
            <div class="pulse-member-card__avatar" :style="{ background: getMemberGradient(member.name) }">
              <img v-if="member.avatar_url" :src="member.avatar_url" :alt="member.name" />
              <span v-else>{{ member.name.charAt(0).toUpperCase() }}</span>
            </div>
            <div class="pulse-member-card__name text-weight-medium">
              {{ member.name.split(' ')[0] }}
            </div>
            <div class="pulse-member-card__amount gala-mono">
              {{ formatAmount(member.totalPaid) }}
            </div>
            <div class="pulse-member-card__cat">
              <q-icon
                :name="member.lastCategoryIcon"
                size="13px"
                :style="{ color: member.lastCategoryColor }"
              />
            </div>
          </div>
        </div>

        <!-- ═══ Trip Insights ═══ -->
        <div class="gala-panel pulse-insights-card q-pa-lg q-mb-lg" v-if="insights.length">
          <div class="row items-center q-gutter-xs q-mb-md">
            <q-icon name="auto_awesome" size="16px" color="amber" />
            <div class="text-subtitle2 text-weight-bold">Trip Insights</div>
          </div>
          <div
            v-for="(insight, i) in insights"
            :key="i"
            class="pulse-insight"
            :class="{ 'pulse-insight--last': i === insights.length - 1 }"
          >
            <div class="pulse-insight__icon-wrap" :style="{ background: insight.color + '18' }">
              <q-icon :name="insight.icon" size="15px" :style="{ color: insight.color }" />
            </div>
            <span class="text-body2 pulse-insight__text">{{ insight.text }}</span>
          </div>
        </div>

        <!-- Bottom spacer for FAB -->
        <div style="height: 80px" />
      </div>
    </template>

    <!-- No trip -->
    <div v-else class="pulse-loading">
      <q-icon name="search_off" size="48px" style="color: var(--muted)" />
      <p class="q-mt-sm text-caption" style="color: var(--muted)">Trip not found</p>
    </div>

    <!-- ═══ Quick Expense FAB ═══ -->
    <q-page-sticky position="bottom-right" :offset="[18, 18]">
      <q-btn
        fab
        icon="add"
        color="primary"
        aria-label="Log expense"
        @click="goToAddExpense"
      >
        <q-tooltip anchor="top middle" self="bottom middle">Log expense</q-tooltip>
      </q-btn>
    </q-page-sticky>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';
import type { Trip, TripMember } from 'src/types/expense';

const route = useRoute();
const router = useRouter();

const tripId = ref(route.params.tripId as string);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const loading = ref(true);

interface PulseExpense {
  id: string;
  description: string;
  amount: number;
  category: string;
  date: string;
  paid_by_id: string;
}

const expenses = ref<PulseExpense[]>([]);

// ── Date helpers ──────────────────────────────────────────────────────
const today = new Date();
today.setHours(0, 0, 0, 0);
const todayStr = today.toISOString().slice(0, 10);

const dayProgress = computed(() => {
  if (!trip.value) return null;
  const start = new Date(trip.value.start_date);
  start.setHours(0, 0, 0, 0);
  const end = new Date(trip.value.end_date);
  end.setHours(0, 0, 0, 0);
  const total = Math.round((end.getTime() - start.getTime()) / 86400000) + 1;
  const current = Math.min(
    Math.max(Math.round((today.getTime() - start.getTime()) / 86400000) + 1, 1),
    total,
  );
  return { current, total };
});

const countdown = computed(() => {
  if (!trip.value) return null;
  const start = new Date(trip.value.start_date);
  start.setHours(0, 0, 0, 0);
  return Math.round((start.getTime() - today.getTime()) / 86400000);
});

const countdownLabel = computed(() => {
  const c = countdown.value;
  if (c === null || c < 0) return '';
  if (c === 0) return 'Starts today!';
  return `${c} day${c !== 1 ? 's' : ''} away`;
});

// ── Expense aggregates ────────────────────────────────────────────────
const totalExpenses = computed(() => expenses.value.reduce((s, e) => s + e.amount, 0));

const todayTotal = computed(() =>
  expenses.value.filter((e) => e.date === todayStr).reduce((s, e) => s + e.amount, 0),
);

const dailyExpenseMap = computed(() => {
  const map = new Map<string, number>();
  for (const e of expenses.value) {
    map.set(e.date, (map.get(e.date) ?? 0) + e.amount);
  }
  return map;
});

const dailyAvg = computed(() => {
  const activeDays = dailyExpenseMap.value.size;
  return activeDays > 0 ? totalExpenses.value / activeDays : 0;
});

// ── Budget ────────────────────────────────────────────────────────────
const budgetPct = computed(() => {
  if (!trip.value?.budget_amount) return 0;
  return Math.min(Math.round((totalExpenses.value / trip.value.budget_amount) * 100), 100);
});

// Circumference for r=36: 2π×36 ≈ 226
const budgetArcLength = computed(() => (budgetPct.value / 100) * 226);

const budgetArcColor = computed(() => {
  if (budgetPct.value >= 100) return '#ef4444';
  if (budgetPct.value >= 75) return '#f59e0b';
  return '#10b981';
});

const budgetStatusLabel = computed(() => {
  if (!trip.value?.budget_amount) return '';
  const remaining = trip.value.budget_amount - totalExpenses.value;
  if (remaining < 0) return `${formatAmount(Math.abs(remaining))} over budget`;
  if (budgetPct.value >= 75) return `${formatAmount(remaining)} left`;
  return 'On track';
});

// ── Wave visualization ────────────────────────────────────────────────
const SVG_W = 400;
const SVG_H = 100;
const PAD_X = 20;
const PAD_Y = 15;

const tripDays = computed(() => {
  if (!trip.value) return [];
  const start = new Date(trip.value.start_date);
  start.setHours(0, 0, 0, 0);
  const end = new Date(trip.value.end_date);
  end.setHours(0, 0, 0, 0);
  const days: string[] = [];
  const cur = new Date(start);
  while (cur <= end) {
    days.push(cur.toISOString().slice(0, 10));
    cur.setDate(cur.getDate() + 1);
  }
  return days;
});

const wavePoints = computed(() => {
  const days = tripDays.value;
  if (days.length === 0) return [];
  const maxAmt = Math.max(...days.map((d) => dailyExpenseMap.value.get(d) ?? 0), 1);
  const n = days.length;
  return days.map((d, i) => {
    const amount = dailyExpenseMap.value.get(d) ?? 0;
    const x = n === 1 ? SVG_W / 2 : PAD_X + (i / (n - 1)) * (SVG_W - PAD_X * 2);
    // Higher spending = lower y (closer to top); flat baseline at 80
    const y = amount > 0
      ? SVG_H - PAD_Y - (amount / maxAmt) * (SVG_H - PAD_Y * 2 - 10)
      : SVG_H - PAD_Y;
    return { x, y, amount, isToday: d === todayStr };
  });
});

function buildSmoothPath(pts: { x: number; y: number }[]): string {
  if (pts.length < 2) return '';
  let d = `M ${pts[0]!.x} ${pts[0]!.y}`;
  for (let i = 1; i < pts.length; i++) {
    const p0 = pts[i - 1]!;
    const p1 = pts[i]!;
    const cpx = (p0.x + p1.x) / 2;
    d += ` C ${cpx} ${p0.y}, ${cpx} ${p1.y}, ${p1.x} ${p1.y}`;
  }
  return d;
}

const waveLinePath = computed(() => {
  const pts = wavePoints.value;
  if (pts.length < 2) return '';
  return buildSmoothPath(pts);
});

const waveFillPath = computed(() => {
  const pts = wavePoints.value;
  if (pts.length < 2) return '';
  const line = buildSmoothPath(pts);
  const last = pts[pts.length - 1]!;
  const first = pts[0]!;
  return `${line} L ${last.x} ${SVG_H} L ${first.x} ${SVG_H} Z`;
});

const todayPoint = computed(() => wavePoints.value.find((p) => p.isToday) ?? null);

const waveDayLabels = computed(() => {
  const days = tripDays.value;
  if (days.length === 0) return [];
  // Show at most 7 evenly-spaced labels
  const step = Math.max(1, Math.ceil(days.length / 7));
  const result: { text: string; isToday: boolean }[] = [];
  for (let i = 0; i < days.length; i += step) {
    const d = days[i]!;
    const dt = new Date(d);
    result.push({
      text: dt.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
      isToday: d === todayStr,
    });
  }
  // Always include the last day
  const lastDay = days[days.length - 1]!;
  if (result.at(-1)?.text !== new Date(lastDay).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })) {
    result.push({
      text: new Date(lastDay).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
      isToday: lastDay === todayStr,
    });
  }
  return result;
});

// ── Member stats ──────────────────────────────────────────────────────
const CATEGORY_META: Record<string, { icon: string; color: string }> = {
  'Food & Drinks': { icon: 'dinner_dining', color: '#f59e0b' },
  Accommodation:   { icon: 'bed', color: '#a78bfa' },
  Transportation:  { icon: 'flight_takeoff', color: '#38bdf8' },
  Activities:      { icon: 'local_activity', color: '#fb7185' },
  Groceries:       { icon: 'shopping_cart', color: '#a3e635' },
  Shopping:        { icon: 'shopping_bag', color: '#e91e63' },
  Health:          { icon: 'medical_services', color: '#ef4444' },
  Other:           { icon: 'category', color: '#64748b' },
};

const memberStats = computed(() => {
  return members.value.map((m) => {
    const myExp = expenses.value.filter((e) => e.paid_by_id === m.id);
    const totalPaid = myExp.reduce((s, e) => s + e.amount, 0);
    const lastExp = [...myExp].sort((a, b) => b.date.localeCompare(a.date))[0];
    const cat = lastExp?.category ?? 'Other';
    const meta = CATEGORY_META[cat] ?? { icon: 'receipt', color: '#64748b' };
    return {
      ...m,
      totalPaid,
      lastCategoryIcon: meta.icon,
      lastCategoryColor: meta.color,
    };
  }).sort((a, b) => b.totalPaid - a.totalPaid);
});

const MEMBER_GRADIENTS = [
  'linear-gradient(135deg, #0d9488, #134e4a)',
  'linear-gradient(135deg, #6366f1, #4338ca)',
  'linear-gradient(135deg, #f59e0b, #b45309)',
  'linear-gradient(135deg, #ec4899, #be185d)',
  'linear-gradient(135deg, #10b981, #047857)',
  'linear-gradient(135deg, #3b82f6, #1d4ed8)',
];

function getMemberGradient(name: string): string {
  return MEMBER_GRADIENTS[name.charCodeAt(0) % MEMBER_GRADIENTS.length]!;
}

// ── Insights ──────────────────────────────────────────────────────────
interface Insight { icon: string; color: string; text: string }

const insights = computed((): Insight[] => {
  const result: Insight[] = [];
  const dp = dayProgress.value;

  // Budget insight
  if (trip.value?.budget_amount) {
    const remaining = trip.value.budget_amount - totalExpenses.value;
    const daysLeft = dp ? dp.total - dp.current : 0;
    if (remaining > 0) {
      result.push({
        icon: 'savings',
        color: '#10b981',
        text: `Group is ${formatAmount(remaining)} under budget${daysLeft > 0 ? ` with ${daysLeft} day${daysLeft !== 1 ? 's' : ''} to go` : ''}.`,
      });
    } else {
      result.push({
        icon: 'warning',
        color: '#ef4444',
        text: `Budget exceeded by ${formatAmount(Math.abs(remaining))}.`,
      });
    }
    // Projected spend
    if (dailyAvg.value > 0 && dp && dp.current < dp.total) {
      const daysLeft2 = dp.total - dp.current;
      const projected = totalExpenses.value + dailyAvg.value * daysLeft2;
      if (projected > trip.value.budget_amount) {
        result.push({
          icon: 'trending_up',
          color: '#f59e0b',
          text: `At this rate, you'll reach ${formatAmount(projected)} total — ${formatAmount(projected - trip.value.budget_amount)} over.`,
        });
      }
    }
  }

  // Today's spending
  if (todayTotal.value > 0) {
    result.push({
      icon: 'today',
      color: '#3b82f6',
      text: `${formatAmount(todayTotal.value)} spent today.`,
    });
  } else if (dp && dp.current >= 1 && dp.current <= dp.total) {
    result.push({
      icon: 'today',
      color: '#f59e0b',
      text: 'No expenses logged today yet.',
    });
  }

  // Top spender
  const top = memberStats.value[0];
  if (top && memberStats.value.length > 1 && top.totalPaid > 0) {
    result.push({
      icon: 'emoji_events',
      color: '#f59e0b',
      text: `${top.name} has paid the most — ${formatAmount(top.totalPaid)}.`,
    });
  }

  return result;
});

// ── Helpers ───────────────────────────────────────────────────────────
function formatAmount(amount: number): string {
  const currency = trip.value?.currency_code ?? 'PHP';
  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency,
    maximumFractionDigits: 0,
  }).format(amount);
}

function goToAddExpense() {
  void router.push(`/trips/${tripId.value}/expenses/new`);
}

// ── Data fetching + realtime ──────────────────────────────────────────
let realtimeChannel: ReturnType<typeof supabase.channel> | null = null;

async function fetchExpenses(): Promise<void> {
  const { data } = await supabase
    .from('expenses')
    .select('id, description, amount, category, date, paid_by_id')
    .eq('trip_id', tripId.value)
    .order('date', { ascending: false });
  expenses.value = (data as PulseExpense[]) ?? [];
}

async function fetchData(): Promise<void> {
  loading.value = true;
  try {
    const { data: tripData, error: tripError } = await supabase
      .from('trips').select('*').eq('id', tripId.value).single();
    if (tripError || !tripData) throw new Error('Trip not found');
    trip.value = tripData as Trip;

    const { data: memberData } = await supabase
      .from('members').select('*').eq('trip_id', tripId.value).order('name');
    members.value = (memberData as TripMember[]) ?? [];

    await fetchExpenses();
  } catch (err) {
    console.error('TripPulse fetch error:', err);
  } finally {
    loading.value = false;
  }
}

function subscribeRealtime(): void {
  realtimeChannel = supabase
    .channel(`pulse-${tripId.value}`)
    .on(
      'postgres_changes',
      { event: '*', schema: 'public', table: 'expenses', filter: `trip_id=eq.${tripId.value}` },
      () => { void fetchExpenses(); },
    )
    .subscribe();
}

onMounted(() => {
  void fetchData().then(() => subscribeRealtime());
});

onUnmounted(() => {
  if (realtimeChannel) void supabase.removeChannel(realtimeChannel);
});
</script>

<style scoped lang="scss">
// ─── Page ─────────────────────────────────────────────────────────────
.pulse-page {
  min-height: 100vh;
  padding-top: 72px;
}

// ─── Toolbar ─────────────────────────────────────────────────────────
.pulse-toolbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  padding-top: max(var(--space-sm), env(safe-area-inset-top));
  background: transparent;

  &__pill {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    border-radius: var(--gala-radius-pill);
    font-size: 0.875rem;
    max-width: 220px;
    overflow: hidden;
  }

  &__name {
    font-size: 0.95rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.pulse-back-btn {
  background: var(--gala-glass-bg);
  backdrop-filter: var(--gala-glass-blur);
  -webkit-backdrop-filter: var(--gala-glass-blur);
  border: 1px solid var(--gala-glass-border);
  border-radius: 50%;
}

// ─── Loading ──────────────────────────────────────────────────────────
.pulse-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;

  &__ring {
    width: 80px;
    height: 80px;
  }

  &__svg {
    width: 100%;
    height: 100%;
  }

  &__arc {
    animation: spin-arc 1.4s linear infinite;
    transform-origin: 40px 40px;
  }
}

@keyframes spin-arc {
  to { transform: rotate(360deg); }
}

// ─── Content ─────────────────────────────────────────────────────────
.pulse-content {
  max-width: 640px;
  margin: 0 auto;
}

// ─── Hero ────────────────────────────────────────────────────────────
.pulse-hero {
  padding-top: var(--space-sm);

  &__eyebrow {
    color: var(--muted);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    font-size: 0.7rem;
  }

  &__name {
    font-size: clamp(1.8rem, 5vw, 2.4rem);
    line-height: 1.1;
    color: var(--on-background);
  }
}

// Chips
.pulse-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: var(--gala-radius-pill);
  font-size: 0.7rem;
  font-weight: 600;

  &--day {
    background: rgba(13, 148, 136, 0.12);
    color: #0d9488;
    border: 1px solid rgba(13, 148, 136, 0.2);
  }

  &--countdown {
    background: rgba(245, 158, 11, 0.12);
    color: #f59e0b;
    border: 1px solid rgba(245, 158, 11, 0.2);
  }

  &--live {
    background: rgba(239, 68, 68, 0.1);
    color: #ef4444;
    border: 1px solid rgba(239, 68, 68, 0.2);
  }
}

.pulse-live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ef4444;
  animation: live-pulse 1.5s ease-in-out infinite;
}

@keyframes live-pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.7); }
}

// ─── Wave Card ────────────────────────────────────────────────────────
.pulse-wave-card {
  overflow: hidden;
}

.pulse-wave-container {
  width: 100%;
}

.pulse-wave-svg {
  width: 100%;
  height: 120px;
  display: block;
  overflow: visible;
}

.wave-fill {
  animation: wave-fade-in 0.8s var(--ease-out-expo) forwards;
  opacity: 0;
}

.wave-line {
  animation: wave-draw 1.2s var(--ease-out-expo) forwards;
  stroke-dasharray: 1200;
  stroke-dashoffset: 1200;
}

@keyframes wave-draw {
  to { stroke-dashoffset: 0; }
}

@keyframes wave-fade-in {
  to { opacity: 1; }
}

.today-pulse-ring {
  animation: ring-pulse 2s ease-in-out infinite;
}

@keyframes ring-pulse {
  0%, 100% { r: 6; opacity: 0.25; }
  50% { r: 10; opacity: 0.1; }
}

.pulse-wave-labels {
  display: flex;
  justify-content: space-between;
  padding: 4px var(--space-xs) 0;
  margin-top: 4px;
}

.wave-label {
  font-size: 0.6rem;
  color: var(--muted);
  font-weight: 500;

  &--today {
    color: #f59e0b;
    font-weight: 700;
  }
}

// Stat row
.pulse-stat {
  &__label {
    font-size: 0.65rem;
    color: var(--muted);
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.06em;
    margin-bottom: 2px;
  }

  &__value {
    font-size: 0.95rem;
    font-weight: 600;
    color: var(--on-background);
  }
}

// ─── Budget Arc ───────────────────────────────────────────────────────
.pulse-arc-wrap {
  width: 88px;
  height: 88px;
  flex-shrink: 0;
}

.pulse-arc-svg {
  width: 100%;
  height: 100%;
}

.arc-progress {
  transition: stroke-dasharray 1s cubic-bezier(0.16, 1, 0.3, 1);
}

.arc-pct {
  font-size: 16px;
  font-weight: 700;
  font-family: 'Plus Jakarta Sans', sans-serif;
}

.arc-sub {
  font-size: 10px;
  font-family: 'Plus Jakarta Sans', sans-serif;
}

.pulse-budget {
  &__spent {
    font-size: 1.5rem;
    line-height: 1.1;
    margin-bottom: 2px;
  }

  &__badge {
    font-size: 0.65rem;
    font-weight: 600;
    border-radius: var(--gala-radius-pill);
    padding: 3px 10px;
  }
}

// ─── Member Grid ──────────────────────────────────────────────────────
.pulse-members-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: var(--space-sm);
}

.pulse-member-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-sm) var(--space-xs) var(--space-sm);
  text-align: center;
  gap: 4px;
  transition: transform var(--duration-fast) var(--ease-spring);

  &:hover {
    transform: translateY(-2px);
  }

  &__avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-weight: 700;
    font-size: 1.1rem;
    overflow: hidden;
    margin-bottom: 2px;
    flex-shrink: 0;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  &__name {
    font-size: 0.7rem;
    line-height: 1.2;
    color: var(--on-background);
    max-width: 72px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__amount {
    font-size: 0.65rem;
    color: var(--muted);
    font-weight: 600;
  }

  &__cat {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--border);
    margin-top: 2px;
  }
}

// ─── Insights ─────────────────────────────────────────────────────────
.pulse-insights-card {
  // inherits gala-panel
}

.pulse-insight {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--border);

  &--last {
    border-bottom: none;
    padding-bottom: 0;
  }

  &__icon-wrap {
    width: 28px;
    height: 28px;
    border-radius: var(--gala-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__text {
    flex: 1;
    line-height: 1.4;
    font-size: 0.85rem;
    color: var(--on-surface);
    padding-top: 5px;
  }
}
</style>
