<template>
  <q-page class="details-page">
    <!-- ═══ Floating transparent toolbar ═══ -->
    <q-header class="details-toolbar" :class="{ 'details-toolbar--scrolled': titleCollapsed }">
      <q-toolbar>
        <q-btn
          flat round
          icon="arrow_back"
          @click="handleBack"
          aria-label="Go Back"
          class="toolbar-icon-btn"
        />
        <q-toolbar-title class="toolbar-title">
          <transition name="fade">
            <span v-if="titleCollapsed" class="gala-display toolbar-trip-name">{{ trip?.name ?? 'Loading...' }}</span>
          </transition>
        </q-toolbar-title>
        <q-btn
          flat round dense
          icon="monitor_heart"
          @click="router.push(`/trips/${tripId}/pulse`)"
          aria-label="Trip Pulse"
          class="toolbar-icon-btn q-mr-xs"
        >
          <q-tooltip anchor="bottom middle" self="top middle">Trip Pulse</q-tooltip>
        </q-btn>
        <q-btn
          flat round dense
          icon="currency_exchange"
          @click="showConverter = true"
          aria-label="Currency converter"
          class="toolbar-icon-btn q-mr-xs"
        />
        <q-btn
          flat round
          icon="settings"
          @click="openSettingsModal"
          aria-label="Settings"
          class="toolbar-icon-btn"
        />
      </q-toolbar>
    </q-header>

    <!-- ═══ Cover image hero ═══ -->
    <q-scroll-observer @scroll="onPageScroll" />

    <div class="details-hero">
      <img :src="tripCoverImage" :alt="trip?.name ?? ''" class="details-hero__image" />
      <div class="details-hero__overlay" />

      <!-- Title content over image -->
      <div class="details-hero__content">
        <div class="gala-display details-hero__name">{{ trip?.name ?? '' }}</div>
        <div class="row items-center q-gutter-x-sm q-mt-xs">
          <span v-if="trip?.destination" class="details-hero__meta">
            <q-icon name="place" size="12px" />{{ trip.destination }}
          </span>
          <span v-if="!loadingWeather && weather" class="details-hero__meta">
            <q-icon :name="getWeatherIcon(weather.forecast)" size="12px" />
            {{ weather.temperature }}°C
          </span>
          <span v-if="loadingWeather" class="details-hero__meta">
            <q-spinner size="xs" color="white" />
          </span>
        </div>
      </div>
    </div>

    <currency-converter-dialog
      v-model="showConverter"
      :default-from="trip?.currency_code"
      default-to="PHP"
    />

    <!-- ═══ Sticky pill segment tabs ═══ -->
    <div class="details-tabs-wrap">
      <div class="details-tabs gala-scroll">
        <div
          v-for="t in tabItems"
          :key="t.name"
          class="details-tab"
          :class="{ 'details-tab--active': tab === t.name }"
          @click="tab = t.name"
        >
          <q-icon :name="t.icon" size="14px" />
          {{ t.label }}
        </div>
      </div>
    </div>

    <!-- Tab Panels -->
    <q-tab-panels
      v-model="tab"
      animated
      :transition-prev="tabTransition === 'slide-left' ? 'slide-right' : 'slide-left'"
      :transition-next="tabTransition"
      class="bg-surface"
    >
      <!-- Itinerary Tab Panel -->
      <q-tab-panel name="itinerary" class="q-pa-md">
        <itinerary-tab
          v-model="itineraryItems"
          :trip-id="trip?.id ?? ''"
          :trip-members="members"
          :trip-start-date="trip?.start_date"
          :trip-end-date="trip?.end_date"
        />
      </q-tab-panel>

      <!-- Expenses Tab Panel -->
      <q-tab-panel name="expenses" class="q-pa-md">
        <div class="row items-center justify-between q-mb-md">
          <div class="text-h6">Expenses</div>
          <q-btn
            unelevated
            color="primary"
            icon="add"
            label="Add Expense"
            size="sm"
            @click="goToAddExpense"
          />
        </div>

        <div v-if="loadingExpenses" class="q-pa-sm">
          <q-item v-for="n in 4" :key="n" class="q-mb-xs">
            <q-item-section avatar>
              <q-skeleton type="QAvatar" size="36px" />
            </q-item-section>
            <q-item-section>
              <q-skeleton type="text" width="55%" class="q-mb-xs" />
              <q-skeleton type="text" width="30%" />
            </q-item-section>
            <q-item-section side>
              <q-skeleton type="text" width="50px" />
            </q-item-section>
          </q-item>
        </div>

        <empty-state
          v-else-if="expenses.length === 0"
          icon="receipt_long"
          title="No expenses yet"
          subtitle="Track your group spending by adding the first expense."
          action-label="Add the first one"
          @action="goToAddExpense"
        />

        <div v-else class="expense-list">
          <q-slide-item
            v-for="expense in expenses"
            :key="expense.id"
            right-color="negative"
            @right="({ reset }) => confirmDeleteExpense(expense, reset)"
            @click="goToEditExpense(expense.id)"
            class="expense-slide-item"
          >
            <template #right>
              <q-icon name="delete" />
            </template>
            <div class="expense-item" :class="`expense-item--${getCategoryClass(expense.category)}`">
              <div class="expense-item__icon" :style="{ background: getCategoryBg(expense.category) }">
                <q-icon :name="getCategoryIcon(expense.category)" :style="{ color: getCategoryHex(expense.category) }" size="18px" />
              </div>
              <div class="expense-item__body col">
                <div class="expense-item__desc">{{ expense.description }}</div>
                <div class="expense-item__meta">{{ expense.category }} · {{ formatDate(expense.date) }}</div>
              </div>
              <div class="expense-item__right">
                <div class="expense-item__amount">{{ formatAmount(expense.amount, trip?.currency_code) }}</div>
                <div class="expense-item__payer">{{ getMemberName(expense.paid_by_id) }}</div>
              </div>
            </div>
          </q-slide-item>
        </div>

        <div v-if="expenses.length > 0" class="expense-total q-mt-sm">
          <div class="row items-center justify-between">
            <span class="text-body2 text-grey-6">Total expenses</span>
            <span class="text-subtitle1 text-weight-bold text-primary">
              {{ formatAmount(totalExpenses, trip?.currency_code) }}
            </span>
          </div>

          <template v-if="trip?.budget_amount">
            <div class="row items-center justify-between q-mt-xs">
              <span class="text-caption text-grey-6">
                Budget: {{ formatAmount(trip.budget_amount, trip.currency_code) }}
              </span>
              <span
                class="text-caption text-weight-medium"
                :class="budgetPct >= 100 ? 'text-negative' : budgetPct >= 75 ? 'text-warning' : 'text-positive'"
              >
                {{ budgetPct }}% used
              </span>
            </div>
            <q-linear-progress
              :value="Math.min(budgetPct / 100, 1)"
              :color="budgetPct >= 100 ? 'negative' : budgetPct >= 75 ? 'warning' : 'positive'"
              rounded
              size="6px"
              class="q-mt-xs"
            />
          </template>
        </div>

        <!-- My Budget -->
        <div v-if="currentMemberId && expenses.length > 0" class="q-mt-sm q-px-xs">
          <div class="text-caption text-grey-6 q-mb-xs">My Budget</div>
          <div class="row items-center justify-between q-mb-xs">
            <span class="text-caption text-grey-6">My share</span>
            <span class="text-caption text-weight-medium">
              {{ formatAmount(myShare, trip?.currency_code) }}
            </span>
          </div>
          <div class="row items-center q-gutter-sm">
            <q-input
              v-model.number="myBudgetInput"
              type="number"
              dense
              outlined
              :prefix="trip?.currency_code ?? ''"
              placeholder="Set budget"
              class="col"
              style="min-width: 0"
            />
            <q-btn
              dense
              flat
              color="primary"
              icon="save"
              :loading="savingMyBudget"
              @click="saveMyBudget"
            />
          </div>
          <template v-if="myBudget">
            <div class="row items-center justify-between q-mt-xs">
              <span class="text-caption text-grey-6">
                Budget: {{ formatAmount(myBudget, trip?.currency_code) }}
              </span>
              <span
                class="text-caption text-weight-medium"
                :class="myBudgetPct >= 100 ? 'text-negative' : myBudgetPct >= 75 ? 'text-warning' : 'text-positive'"
              >
                {{ myBudgetPct }}% used
              </span>
            </div>
            <q-linear-progress
              :value="Math.min(myBudgetPct / 100, 1)"
              :color="myBudgetPct >= 100 ? 'negative' : myBudgetPct >= 75 ? 'warning' : 'positive'"
              rounded
              size="6px"
              class="q-mt-xs"
            />
          </template>
        </div>

        <!-- Spending Insights -->
        <q-expansion-item
          v-if="expenses.length > 0"
          v-model="showInsights"
          icon="insights"
          label="Spending Insights"
          class="q-mt-sm rounded-borders"
          header-class="text-primary text-weight-medium"
          dense
          expand-separator
        >
          <q-card flat bordered class="q-mx-xs q-mb-xs">
            <q-card-section class="q-pt-sm q-pb-sm">
              <!-- Donut + category legend -->
              <div class="row items-center q-gutter-sm">
                <canvas
                  ref="chartCanvas"
                  width="140"
                  height="140"
                  style="flex-shrink: 0;"
                />
                <div class="col" style="min-width: 0;">
                  <div
                    v-for="item in categoryBreakdown"
                    :key="item.category"
                    class="row items-center q-mb-xs"
                  >
                    <span
                      class="q-mr-sm"
                      style="width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0;"
                      :style="{ background: getCategoryHex(item.category) }"
                    />
                    <span class="col text-caption ellipsis">{{ item.category }}</span>
                    <span class="text-caption text-weight-medium q-ml-xs">{{ item.pct }}%</span>
                  </div>
                </div>
              </div>

              <!-- Per-member spending -->
              <q-separator class="q-my-sm" />
              <div class="text-caption text-grey-6 q-mb-sm">Paid by member</div>
              <div v-for="m in memberSpending" :key="m.name" class="q-mb-sm">
                <div class="row items-center justify-between q-mb-xs">
                  <span class="text-caption">{{ m.name }}</span>
                  <span class="text-caption text-weight-medium">
                    {{ formatAmount(m.amount, trip?.currency_code) }}
                    <span class="text-grey-5">({{ m.pct }}%)</span>
                  </span>
                </div>
                <q-linear-progress :value="m.pct / 100" color="primary" rounded size="4px" />
              </div>
            </q-card-section>
          </q-card>
        </q-expansion-item>
      </q-tab-panel>

      <!-- Settlement Tab Panel -->
      <q-tab-panel name="settlement" class="q-pa-none">
        <div v-if="loading" class="q-pa-md">
          <q-skeleton type="rect" height="80px" class="q-mb-md rounded-borders" />
          <q-skeleton type="text" width="40%" class="q-mb-sm" />
          <q-card flat bordered class="q-mb-sm" v-for="n in 3" :key="n">
            <q-card-section class="row items-center q-gutter-sm">
              <q-skeleton type="QAvatar" size="32px" />
              <div class="col">
                <q-skeleton type="text" width="50%" class="q-mb-xs" />
                <q-skeleton type="text" width="30%" />
              </div>
              <q-skeleton type="QBtn" width="70px" />
            </q-card-section>
          </q-card>
        </div>
        <empty-state
          v-else-if="members.length <= 1"
          icon="group_add"
          title="No members yet"
          subtitle="Add your barkada to start splitting expenses."
          action-label="Add Members"
          @action="tab = 'members'"
        />
        <empty-state
          v-else-if="expenses.length === 0"
          icon="account_balance_wallet"
          title="No expenses to settle"
          subtitle="Add expenses and we'll calculate who owes who."
          action-label="Add an Expense"
          @action="goToAddExpense"
        />
        <settlement-view
          v-else
          :expenses="expensesWithSplits"
          :members="members"
          :current-member-id="currentMemberId ?? ''"
          :currency-code="trip!.currency_code"
          :trip-id="trip!.id"
        />
      </q-tab-panel>

      <!-- Members Tab -->
      <q-tab-panel name="members" class="q-pa-md">
        <div class="text-h6 q-mb-md">Trip Members</div>
        <empty-state
          v-if="members.length <= 1"
          icon="people"
          title="Just you so far"
          subtitle="Invite your barkada to join the trip and split expenses together."
          action-label="Add Members"
          @action="addMemberDialog = true"
          class="q-mb-md"
        />
        <q-list bordered separator rounded>
          <q-item v-for="member in members" :key="member.id">
            <q-item-section avatar>
              <q-avatar :color="member.is_owner ? 'primary' : getMemberColor(member.name)" text-color="white">
                {{ member.name.charAt(0).toUpperCase() }}
              </q-avatar>
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-medium">{{ member.name }}</q-item-label>
              <q-item-label caption v-if="member.is_owner">Trip Owner</q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-icon name="star" color="amber" v-if="member.is_owner" />
              <q-btn
                v-else
                flat
                round
                dense
                icon="remove_circle_outline"
                color="negative"
                size="sm"
                @click.stop="confirmRemoveMember(member)"
              />
            </q-item-section>
          </q-item>

          <!-- Add Member Button -->
          <q-item clickable @click="addMemberDialog = true">
            <q-item-section avatar>
              <q-avatar color="grey-3" text-color="grey-7" icon="add" />
            </q-item-section>
            <q-item-section>
              <q-item-label class="text-grey-7">Add Member</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-tab-panel>

      <!-- Activity Tab -->
      <q-tab-panel name="activity" class="q-pa-md">
        <div class="row items-center justify-between q-mb-sm">
          <div class="text-h6">Recent Activity</div>
        </div>

        <q-btn-toggle
          v-model="activityFilter"
          unelevated
          rounded
          dense
          no-caps
          size="sm"
          class="q-mb-md"
          :options="[
            { label: 'All', value: 'all' },
            { label: 'Expenses', value: 'expense' },
            { label: 'Members', value: 'member' },
            { label: 'Settlement', value: 'settlement' },
          ]"
          color="primary"
          text-color="primary"
          toggle-color="primary"
          toggle-text-color="white"
        />

        <div v-if="loadingActivity" class="q-pa-sm">
          <div v-for="n in 4" :key="n" class="row items-start q-mb-lg q-gutter-sm">
            <q-skeleton type="QAvatar" size="28px" />
            <div class="col">
              <q-skeleton type="text" width="65%" class="q-mb-xs" />
              <q-skeleton type="text" width="35%" />
            </div>
          </div>
        </div>

        <empty-state
          v-else-if="filteredActivityLog.length === 0"
          icon="timeline"
          title="No activity here"
          subtitle="Try a different filter or check back after some actions."
        />
        <q-timeline v-else color="primary">
          <q-timeline-entry
            v-for="entry in filteredActivityLog"
            :key="entry.id"
            :title="entry.description"
            :subtitle="formatDate(entry.created_at)"
            :icon="getActivityIcon(entry.action_type)"
          />
          <!-- Always show trip creation at the bottom -->
          <q-timeline-entry
            title="Trip Created"
            :subtitle="formatDate(trip?.created_at || '')"
            icon="add_circle"
          >
            <div>{{ trip?.name }} was created</div>
          </q-timeline-entry>
        </q-timeline>
      </q-tab-panel>
    </q-tab-panels>

    <!-- Settings Modal -->
    <q-dialog v-model="showSettings" persistent>
      <q-card style="width: 100%; max-width: 450px">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">Trip Settings</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section class="q-gutter-y-md q-pt-md">
          <q-input
            v-model="editForm.name"
            label="Trip Name"
            outlined
            dense
            :rules="[(val: string) => !!val || 'Required']"
          >
            <template v-slot:prepend><q-icon name="flight_takeoff" /></template>
          </q-input>

          <q-input
            v-model="editForm.description"
            label="Description (optional)"
            outlined
            dense
            type="textarea"
            rows="2"
          />

          <q-input
            v-model="editForm.destination"
            label="Destination (optional)"
            outlined
            dense
          >
            <template v-slot:prepend><q-icon name="place" /></template>
          </q-input>

          <div class="row q-gutter-sm">
            <q-input
              v-model="editForm.start_date"
              label="Start Date"
              mask="date"
              :rules="['date']"
              outlined
              dense
              class="col"
            >
              <template v-slot:append>
                <q-icon name="event" class="cursor-pointer">
                  <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                    <q-date v-model="editForm.start_date" />
                  </q-popup-proxy>
                </q-icon>
              </template>
            </q-input>
            <q-input
              v-model="editForm.end_date"
              label="End Date"
              mask="date"
              :rules="['date']"
              outlined
              dense
              class="col"
            >
              <template v-slot:append>
                <q-icon name="event" class="cursor-pointer">
                  <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                    <q-date v-model="editForm.end_date" :options="(d: string) => d >= editForm.start_date" />
                  </q-popup-proxy>
                </q-icon>
              </template>
            </q-input>
          </div>

          <div class="row q-gutter-sm">
            <q-select
              v-model="editForm.status"
              :options="statusOptions"
              label="Status"
              outlined
              dense
              emit-value
              map-options
              class="col"
            >
              <template v-slot:prepend><q-icon name="flag" /></template>
            </q-select>

            <q-input
              v-model.number="editForm.budget_amount"
              label="Budget (optional)"
              type="number"
              outlined
              dense
              class="col"
              :prefix="trip?.currency_code ?? 'PHP'"
            />
          </div>
        </q-card-section>

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat no-caps label="Cancel" v-close-popup />
          <q-btn
            unelevated
            no-caps
            color="primary"
            label="Save Changes"
            :loading="savingTrip"
            @click="saveTrip"
          />
        </q-card-actions>

        <q-separator />

        <q-card-actions class="q-pa-md">
          <q-btn flat color="primary" icon="download" label="Export CSV" @click="exportExpensesCSV" />
          <q-space />
          <q-btn flat color="negative" label="Delete Trip" icon="delete" @click="confirmDeleteTrip" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Add Member Dialog -->
    <q-dialog v-model="addMemberDialog">
      <q-card style="min-width: 350px">
        <q-card-section>
          <div class="text-h6">Add Member</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <q-input
            v-model="newMemberName"
            label="Member Name"
            outlined
            dense
            autofocus
            @keyup.enter="addMember"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat no-caps label="Cancel" v-close-popup />
          <q-btn flat no-caps label="Add" color="primary" @click="addMember" :disable="!newMemberName" />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Quick Expense FAB (hidden on itinerary tab — it has its own controls) -->
    <q-page-sticky v-if="tab !== 'itinerary'" position="bottom-right" :offset="[18, 18]">
      <q-btn
        fab
        icon="add"
        color="primary"
        aria-label="Add expense"
        @click="goToAddExpense"
      >
        <q-tooltip anchor="top middle" self="bottom middle">Add expense</q-tooltip>
      </q-btn>
    </q-page-sticky>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue';
import { Chart, DoughnutController, ArcElement, Tooltip, Legend } from 'chart.js';
Chart.register(DoughnutController, ArcElement, Tooltip, Legend);
import { useRoute, useRouter } from 'vue-router';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import type { Trip } from 'src/types/expense';
import type { TripMember } from 'src/types/expense';
import { useTripStore } from 'src/stores/tripStore';
import SettlementView from 'src/components/settlement/SettlementView.vue';
import ItineraryTab from 'src/components/itinerary/ItineraryTab.vue';
import EmptyState from 'src/components/shared/EmptyState.vue';
import type { ItineraryItem } from 'src/components/itinerary/itinerary.model';
import { logActivity } from 'src/utils/activityLogger';
import { fetchWeather } from 'src/utils/weatherService';
import { calculateMemberBalances } from 'src/utils/settlementCalculator';
import CurrencyConverterDialog from 'src/components/shared/CurrencyConverterDialog.vue';

const route = useRoute();
const router = useRouter();
const $q = useQuasar();
const authStore = useAuthStore();
const tripStore = useTripStore();

// Tab items for the pill segment control
const tabItems = [
  { name: 'itinerary',  icon: 'event_note',          label: 'Itinerary' },
  { name: 'expenses',   icon: 'receipt_long',         label: 'Expenses' },
  { name: 'settlement', icon: 'account_balance_wallet', label: 'Settle' },
  { name: 'members',    icon: 'people',               label: 'Members' },
  { name: 'activity',   icon: 'timeline',             label: 'Activity' },
];

// Cover image derived from trip name
const tripCoverImages = [
  'https://images.unsplash.com/photo-1559827260-dc66d52bef19?w=800&h=360&fit=crop',
  'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&h=360&fit=crop',
  'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&h=360&fit=crop',
  'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&h=360&fit=crop',
  'https://images.unsplash.com/photo-1530841377377-3ff06c0ca713?w=800&h=360&fit=crop',
];

const tripCoverImage = computed(() => {
  const name = trip.value?.name ?? '';
  return tripCoverImages[Math.abs((name.charCodeAt(0) || 0) % tripCoverImages.length)]!;
});

// Category helper for CSS class
function getCategoryClass(category: string): string {
  const map: Record<string, string> = {
    'Food & Drinks': 'food',
    Accommodation:   'hotel',
    Transportation:  'transport',
    Activities:      'activity',
    Groceries:       'grocery',
  };
  return map[category] ?? 'other';
}

function getCategoryBg(category: string): string {
  const map: Record<string, string> = {
    'Food & Drinks': 'rgba(245, 158, 11, 0.1)',
    Accommodation:   'rgba(167, 139, 250, 0.1)',
    Transportation:  'rgba(56, 189, 248, 0.1)',
    Activities:      'rgba(251, 113, 133, 0.1)',
    Groceries:       'rgba(163, 230, 53, 0.1)',
  };
  return map[category] ?? 'rgba(100, 116, 139, 0.1)';
}

// State
const tab = ref('itinerary');
const tabOrder = ['itinerary', 'expenses', 'settlement', 'members', 'activity'];
const tabTransition = ref<'slide-left' | 'slide-right'>('slide-left');
watch(tab, (next, prev) => {
  tabTransition.value = tabOrder.indexOf(next) > tabOrder.indexOf(prev) ? 'slide-left' : 'slide-right';
});
const titleCollapsed = ref(false);
const loading = ref(true);
const loadingExpenses = ref(false);
const loadingActivity = ref(false);
const savingTrip = ref(false);
const trip = ref<Trip | null>(null);
const members = ref<TripMember[]>([]);
const showSettings = ref(false);
const addMemberDialog = ref(false);
const newMemberName = ref('');
const tripId = ref(route.params.tripId as string);

interface ItineraryEvent {
  id: string;
  trip_id: string;
  event_date: string;
  event_time?: string;
  title: string;
  location?: string;
  description?: string;
  icon?: string;
  display_order: number;
  created_at: string;
}

interface Expense {
  id: string;
  description: string;
  amount: number;
  category: string;
  date: string;
  paid_by_id: string;
  split_type?: string;
  splits: { member_id: string; share_amount: number }[];
}

interface ActivityEntry {
  id: string;
  action_type: string;
  description: string;
  created_at: string;
}

const itineraryItems = ref<ItineraryItem[]>([]);
const expenses = ref<Expense[]>([]);
const activityLog = ref<ActivityEntry[]>([]);
const activityFilter = ref('all');
const weather = ref<{ forecast: string; temperature: number } | null>(null);
const loadingWeather = ref(false);

const filteredActivityLog = computed(() => {
  if (activityFilter.value === 'all') return activityLog.value;
  return activityLog.value.filter((e) => e.action_type.startsWith(activityFilter.value));
});

const statusOptions = [
  { label: 'Planning', value: 'planning' },
  { label: 'Active', value: 'active' },
  { label: 'Completed', value: 'completed' },
  { label: 'Archived', value: 'archived' },
];

// Edit form for settings modal
const editForm = ref({
  name: '',
  description: '',
  destination: '',
  start_date: '',
  end_date: '',
  status: 'planning' as string,
  budget_amount: null as number | null,
});

// Expenses formatted for settlement calculator
const expensesWithSplits = computed(() => expenses.value);

const totalExpenses = computed(() =>
  expenses.value.reduce((sum, e) => sum + e.amount, 0)
);

const budgetPct = computed(() => {
  if (!trip.value?.budget_amount) return 0;
  return Math.round((totalExpenses.value / trip.value.budget_amount) * 100);
});

const showConverter = ref(false);
const myBudget = ref<number | null>(null);
const myBudgetInput = ref<number | null>(null);
const savingMyBudget = ref(false);

const myShare = computed(() => {
  if (!currentMemberId.value) return 0;
  const balances = calculateMemberBalances(expenses.value, members.value);
  return balances.find((b) => b.memberId === currentMemberId.value)?.totalOwed ?? 0;
});

const myBudgetPct = computed(() => {
  if (!myBudget.value) return 0;
  return Math.round((myShare.value / myBudget.value) * 100);
});

// ── Expense Insights ────────────────────────────────────────────────────────
const showInsights = ref(false);
const chartCanvas = ref<HTMLCanvasElement | null>(null);
let chartInstance: Chart | null = null;

const CATEGORY_HEX: Record<string, string> = {
  'Food & Drinks': '#e64a19',
  Accommodation:   '#3949ab',
  Transportation:  '#00838f',
  Activities:      '#7b1fa2',
  Groceries:       '#388e3c',
  Shopping:        '#e91e63',
  Health:          '#d32f2f',
  Other:           '#546e7a',
};

function getCategoryHex(category: string): string {
  return CATEGORY_HEX[category] ?? '#0D9488';
}

const categoryBreakdown = computed(() => {
  const map = new Map<string, number>();
  for (const e of expenses.value) {
    map.set(e.category, (map.get(e.category) ?? 0) + e.amount);
  }
  const total = totalExpenses.value || 1;
  return Array.from(map.entries())
    .sort((a, b) => b[1] - a[1])
    .map(([category, amount]) => ({
      category,
      amount,
      pct: Math.round((amount / total) * 100),
    }));
});

const memberSpending = computed(() => {
  const map = new Map<string, number>();
  for (const e of expenses.value) {
    const name = getMemberName(e.paid_by_id);
    map.set(name, (map.get(name) ?? 0) + e.amount);
  }
  const total = totalExpenses.value || 1;
  return Array.from(map.entries())
    .sort((a, b) => b[1] - a[1])
    .map(([name, amount]) => ({ name, amount, pct: Math.round((amount / total) * 100) }));
});

function renderChart() {
  if (!chartCanvas.value || categoryBreakdown.value.length === 0) return;
  chartInstance?.destroy();
  chartInstance = new Chart(chartCanvas.value, {
    type: 'doughnut',
    data: {
      labels: categoryBreakdown.value.map((c) => c.category),
      datasets: [{
        data: categoryBreakdown.value.map((c) => c.amount),
        backgroundColor: categoryBreakdown.value.map((c) => getCategoryHex(c.category)),
        borderWidth: 0,
        hoverOffset: 4,
      }],
    },
    options: {
      responsive: false,
      cutout: '68%',
      plugins: {
        legend: { display: false },
        tooltip: {
          callbacks: {
            label: (ctx) => ` ${formatAmount(ctx.raw as number, trip.value?.currency_code)}`,
          },
        },
      },
    },
  });
}

watch(showInsights, (v) => { if (v) void nextTick(renderChart); });
watch(expenses, () => { if (showInsights.value) void nextTick(renderChart); });

async function saveMyBudget(): Promise<void> {
  if (!currentMemberId.value) return;
  savingMyBudget.value = true;
  const { error } = await supabase
    .from('members')
    .update({ personal_budget: myBudgetInput.value })
    .eq('id', currentMemberId.value);
  if (error) {
    $q.notify({ type: 'negative', message: 'Failed to save budget' });
  } else {
    myBudget.value = myBudgetInput.value;
    $q.notify({ type: 'positive', message: 'Personal budget saved', icon: 'check_circle' });
  }
  savingMyBudget.value = false;
}

const currentUserId = ref<string | undefined>(undefined);

const currentMemberId = computed(
  () => members.value.find((m: TripMember) => m.user_id === currentUserId.value)?.id,
);

// Helpers
function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function formatAmount(amount: number, currency = 'PHP'): string {
  return new Intl.NumberFormat('en-PH', { style: 'currency', currency }).format(amount);
}

function getMemberName(memberId: string): string {
  return members.value.find((m) => m.id === memberId)?.name || 'Unknown';
}

function getCategoryIcon(category: string): string {
  const icons: Record<string, string> = {
    'Food & Drinks': 'dinner_dining',
    Accommodation:   'bed',
    Transportation:  'flight_takeoff',
    Activities:      'local_activity',
    Groceries:       'shopping_cart',
    Shopping:        'shopping_bag',
    Health:          'medical_services',
    Other:           'category',
  };
  return icons[category] ?? 'receipt';
}

function getMemberColor(name: string): string {
  const colors = ['secondary', 'accent', 'info', 'warning', 'positive'];
  return colors[name.charCodeAt(0) % colors.length] ?? 'secondary';
}

function getActivityIcon(actionType: string): string {
  const icons: Record<string, string> = {
    expense_added: 'add_circle',
    expense_updated: 'edit',
    expense_deleted: 'delete',
    member_joined: 'person_add',
    member_left: 'person_remove',
    settlement_created: 'account_balance_wallet',
    settlement_paid: 'check_circle',
    settlement_verified: 'verified',
    trip_created: 'flight_takeoff',
    trip_updated: 'edit',
    trip_completed: 'check_circle',
  };
  return icons[actionType] || 'circle';
}

// Scroll behavior
function onPageScroll(info: { position: { top: number } }): void {
  titleCollapsed.value = info.position.top > 72;
}

// Navigation
function goToAddExpense(): void {
  void router.push(`/trips/${tripId.value}/expenses/new`);
}

function goToEditExpense(expenseId: string): void {
  void router.push(`/trips/${tripId.value}/expenses/${expenseId}`);
}

function confirmDeleteExpense(expense: Expense, reset: () => void): void {
  $q.dialog({
    title: 'Delete Expense',
    message: `Delete "${expense.description}"? This cannot be undone.`,
    cancel: true,
    persistent: true,
    ok: { label: 'Delete', color: 'negative', flat: true },
  }).onOk(() => {
    void doDeleteExpense(expense.id);
  }).onCancel(reset).onDismiss(reset);
}

async function doDeleteExpense(expenseId: string): Promise<void> {
  try {
    const { error } = await supabase.from('expenses').delete().eq('id', expenseId);
    if (error) throw error;
    expenses.value = expenses.value.filter((e) => e.id !== expenseId);
    $q.notify({ type: 'positive', message: 'Expense deleted.' });
    void logActivity({
      trip_id: tripId.value,
      user_id: authStore.user?.id,
      action_type: 'expense_deleted',
      entity_type: 'expense',
      entity_id: expenseId,
      description: 'Expense deleted',
    });
  } catch (error) {
    console.error('Error deleting expense:', error);
    $q.notify({ type: 'negative', message: 'Failed to delete expense' });
  }
}

// Data fetching
async function fetchTripData(): Promise<void> {
  loading.value = true;
  const id = tripId.value;
  if (!id) { loading.value = false; return; }

  try {
    // 0. Current user (from cached store — no extra network call)
    currentUserId.value = authStore.user?.id;

    // 1. Trip
    const { data: tripData, error: tripError } = await supabase
      .from('trips').select('*').eq('id', id).single();
    if (tripError || !tripData) throw new Error('Trip not found or access denied.');
    trip.value = { ...tripData, currencyCode: tripData.currency_code } as Trip;

    if (tripData.destination) {
      loadingWeather.value = true;
      fetchWeather(tripData.destination as string)
        .then((data) => { weather.value = data; })
        .catch(() => { weather.value = null; })
        .finally(() => { loadingWeather.value = false; });
    }

    // 2. Members
    const { data: memberData, error: memberError } = await supabase
      .from('members').select('*').eq('trip_id', id).order('name');
    if (memberError || !memberData) throw new Error('Could not load trip members.');
    members.value = memberData as TripMember[];
    const myMember = (memberData as TripMember[]).find((m) => m.user_id === authStore.user?.id);
    myBudget.value = myMember?.personal_budget ?? null;
    myBudgetInput.value = myMember?.personal_budget ?? null;

    // 3. Itinerary events
    const { data: itineraryData, error: itineraryError } = await supabase
      .from('itinerary_events').select('*').eq('trip_id', id)
      .order('date', { ascending: true, nullsFirst: false })
      .order('created_at', { ascending: true });

    if (!itineraryError && itineraryData) {
      itineraryItems.value = (itineraryData as ItineraryEvent[]).map((event) => {
        const e = event as ItineraryEvent & {
          date?: string; time?: string; phase?: string; type?: string; notes?: string;
        };
        return {
          id: e.id,
          phase: (e.phase ?? 'on') as 'before' | 'on' | 'after',
          title: e.title,
          type: (e.type ?? 'text') as 'text' | 'checklist' | 'expense',
          date: e.date ?? e.event_date,
          ...(( e.time || e.event_time) && { time: e.time ?? e.event_time }),
          ...(e.location && { location: e.location }),
          ...(( e.notes || e.description) && { notes: e.notes ?? e.description }),
        };
      });
    }
  } catch (error) {
    const msg = error instanceof Error ? error.message : 'Error fetching trip data.';
    console.error('Error fetching trip data:', error);
    $q.notify({ type: 'negative', message: msg });
    trip.value = null;
    members.value = [];
  } finally {
    loading.value = false;
  }
}

async function fetchExpenses(): Promise<void> {
  loadingExpenses.value = true;
  try {
    const { data, error } = await supabase
      .from('expenses')
      .select('id, description, amount, category, date, paid_by_id, split_type, expense_splits(member_id, share_amount)')
      .eq('trip_id', tripId.value)
      .order('date', { ascending: false });

    if (error) throw error;
    expenses.value = (data || []).map((e) => ({
      id: e.id,
      description: e.description,
      amount: e.amount,
      category: e.category,
      date: e.date,
      paid_by_id: e.paid_by_id,
      split_type: e.split_type,
      splits: (e.expense_splits || []) as { member_id: string; share_amount: number }[],
    }));
  } catch (error) {
    console.error('Error fetching expenses:', error);
    $q.notify({ type: 'negative', message: 'Failed to load expenses' });
  } finally {
    loadingExpenses.value = false;
  }
}

async function fetchActivity(): Promise<void> {
  loadingActivity.value = true;
  try {
    const { data, error } = await supabase
      .from('activity_log')
      .select('id, action_type, description, created_at')
      .eq('trip_id', tripId.value)
      .order('created_at', { ascending: false })
      .limit(30);

    if (error) throw error;
    activityLog.value = (data || []) as ActivityEntry[];
  } catch (error) {
    const msg = (error as { message?: string })?.message ?? String(error);
    console.warn('Activity log unavailable:', msg);
    activityLog.value = [];
  } finally {
    loadingActivity.value = false;
  }
}

// Member management
async function addMember(): Promise<void> {
  if (!newMemberName.value.trim()) return;
  try {
    const { error } = await supabase.from('members').insert({
      trip_id: tripId.value,
      name: newMemberName.value.trim(),
      is_owner: false,
    });
    if (error) throw error;
    $q.notify({ type: 'positive', message: 'Member added!' });
    newMemberName.value = '';
    addMemberDialog.value = false;
    await fetchTripData();
  } catch (error) {
    console.error('Error adding member:', error);
    $q.notify({ type: 'negative', message: 'Failed to add member' });
  }
}

function confirmRemoveMember(member: TripMember): void {
  $q.dialog({
    title: 'Remove Member',
    message: `Remove ${member.name} from this trip?`,
    cancel: true,
    persistent: true,
    color: 'negative',
  }).onOk(() => void removeMember(member.id));
}

async function removeMember(memberId: string): Promise<void> {
  try {
    const { error } = await supabase.from('members').delete().eq('id', memberId);
    if (error) throw error;
    $q.notify({ type: 'positive', message: 'Member removed.' });
    await fetchTripData();
  } catch (error) {
    console.error('Error removing member:', error);
    $q.notify({ type: 'negative', message: 'Failed to remove member' });
  }
}

// Trip settings
function openSettingsModal(): void {
  if (trip.value) {
    editForm.value = {
      name: trip.value.name,
      description: trip.value.description || '',
      destination: trip.value.destination || '',
      start_date: trip.value.start_date,
      end_date: trip.value.end_date,
      status: trip.value.status ?? 'planning',
      budget_amount: trip.value.budget_amount ?? null,
    };
  }
  showSettings.value = true;
}

async function saveTrip(): Promise<void> {
  if (!editForm.value.name.trim()) {
    $q.notify({ type: 'warning', message: 'Trip name is required.' });
    return;
  }
  savingTrip.value = true;
  try {
    const { error } = await supabase
      .from('trips')
      .update({
        name: editForm.value.name.trim(),
        description: editForm.value.description || null,
        destination: editForm.value.destination || null,
        start_date: editForm.value.start_date,
        end_date: editForm.value.end_date,
        status: editForm.value.status,
        budget_amount: editForm.value.budget_amount || null,
      })
      .eq('id', tripId.value);
    if (error) throw error;
    $q.notify({ type: 'positive', message: 'Trip updated!' });
    showSettings.value = false;
    await fetchTripData();
    if (trip.value) tripStore.updateTrip(trip.value);
  } catch (error) {
    console.error('Error saving trip:', error);
    $q.notify({ type: 'negative', message: 'Failed to save trip.' });
  } finally {
    savingTrip.value = false;
  }
}

function confirmDeleteTrip(): void {
  $q.dialog({
    title: 'Delete Trip',
    message: `Permanently delete "${trip.value?.name}"? This cannot be undone.`,
    cancel: true,
    persistent: true,
    color: 'negative',
  }).onOk(() => void deleteTrip());
}

async function deleteTrip(): Promise<void> {
  try {
    const { error } = await supabase.from('trips').delete().eq('id', tripId.value);
    if (error) throw error;
    tripStore.removeTrip(tripId.value);
    $q.notify({ type: 'positive', message: 'Trip deleted.' });
    showSettings.value = false;
    void router.replace('/trips');
  } catch (error) {
    console.error('Error deleting trip:', error);
    $q.notify({ type: 'negative', message: 'Failed to delete trip.' });
  }
}

function getWeatherIcon(forecast: string): string {
  if (/clear/i.test(forecast)) return 'wb_sunny';
  if (/snow/i.test(forecast)) return 'ac_unit';
  if (/thunder/i.test(forecast)) return 'thunderstorm';
  if (/rain|drizzle|shower/i.test(forecast)) return 'umbrella';
  if (/fog/i.test(forecast)) return 'foggy';
  if (/cloud|overcast/i.test(forecast)) return 'cloud';
  return 'wb_cloudy';
}

function exportExpensesCSV(): void {
  if (!expenses.value.length) {
    $q.notify({ type: 'warning', message: 'No expenses to export.' });
    return;
  }
  const headers = ['Date', 'Description', 'Category', 'Amount', 'Paid By', 'Split Type'];
  const rows = expenses.value.map((e) => [
    e.date,
    `"${e.description.replace(/"/g, '""')}"`,
    e.category,
    e.amount.toFixed(2),
    getMemberName(e.paid_by_id),
    e.split_type || 'equal',
  ]);
  const csv = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
  const blob = new Blob([csv], { type: 'text/csv' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${trip.value?.name ?? 'trip'}-expenses.csv`;
  a.click();
  URL.revokeObjectURL(url);
  showSettings.value = false;
}

function handleBack(): void {
  void router.back();
}

// Lifecycle
let expensesChannel: ReturnType<typeof supabase.channel> | null = null;
let membersChannel: ReturnType<typeof supabase.channel> | null = null;

onMounted(async () => {
  await fetchTripData();
  void fetchExpenses();
  void fetchActivity();

  const id = tripId.value;
  expensesChannel = supabase
    .channel(`expenses_${id}`)
    .on('postgres_changes', { event: '*', schema: 'public', table: 'expenses', filter: `trip_id=eq.${id}` }, () => {
      void fetchExpenses();
    })
    .subscribe();

  membersChannel = supabase
    .channel(`members_${id}`)
    .on('postgres_changes', { event: '*', schema: 'public', table: 'members', filter: `trip_id=eq.${id}` }, () => {
      void fetchTripData();
    })
    .subscribe();
});

onUnmounted(() => {
  if (expensesChannel) void supabase.removeChannel(expensesChannel);
  if (membersChannel) void supabase.removeChannel(membersChannel);
  chartInstance?.destroy();
});
</script>

<style scoped lang="scss">
// ─── Page ────────────────────────────────────────────────────────────────────
.details-page {
  min-height: 100vh;
  background-color: var(--surface);
  padding-bottom: 80px;
}

// ─── Floating toolbar ────────────────────────────────────────────────────────
.details-toolbar {
  background: transparent !important;
  box-shadow: none !important;
  transition: background var(--duration-normal) var(--ease-out-expo),
              backdrop-filter var(--duration-normal) var(--ease-out-expo);

  &--scrolled {
    background: var(--gala-glass-bg) !important;
    backdrop-filter: blur(20px) saturate(1.8);
    -webkit-backdrop-filter: blur(20px) saturate(1.8);
    border-bottom: 1px solid var(--gala-glass-border);
  }
}

.toolbar-icon-btn {
  color: white;
  transition: color var(--duration-fast) ease;

  .details-toolbar--scrolled & {
    color: var(--on-background);
  }
}

.toolbar-title {
  .details-toolbar--scrolled & {
    color: var(--on-background);
  }
}

.toolbar-trip-name {
  font-size: 1.1rem;
}

// ─── Hero ────────────────────────────────────────────────────────────────────
.details-hero {
  position: relative;
  height: clamp(200px, 50vw, 280px);
  overflow: hidden;

  &__image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(
      to bottom,
      rgba(0, 0, 0, 0.15) 0%,
      rgba(0, 0, 0, 0.3) 40%,
      rgba(0, 0, 0, 0.75) 100%
    );
  }

  &__content {
    position: absolute;
    bottom: 16px;
    left: 16px;
    right: 16px;
  }

  &__name {
    font-size: clamp(1.5rem, 5.5vw, 2.25rem);
    color: white;
    line-height: 1.1;
    text-shadow: 0 1px 8px rgba(0, 0, 0, 0.25);
  }

  &__meta {
    font-size: 0.8rem;
    color: rgba(255, 255, 255, 0.85);
    display: flex;
    align-items: center;
    gap: 3px;
  }
}

// ─── Pill Segment Tabs ────────────────────────────────────────────────────────
.details-tabs-wrap {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--gala-glass-bg);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border-bottom: 1px solid var(--gala-glass-border);
  padding: 8px 16px;
}

.details-tabs {
  display: flex;
  gap: 6px;
  overflow-x: auto;
  scrollbar-width: none;

  &::-webkit-scrollbar { display: none; }
}

.details-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: var(--gala-radius-pill);
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--muted);
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--duration-fast) var(--ease-out-expo);
  user-select: none;

  &:hover {
    color: $primary;
    background: rgba($primary, 0.06);
  }

  &--active {
    background: $primary;
    color: white;
  }
}

// ─── Expense list ─────────────────────────────────────────────────────────────
.expense-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.expense-slide-item {
  border-radius: var(--gala-radius-md);
  overflow: hidden;
}

.expense-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-md);
  border-left-width: 3px;
  position: relative;
  cursor: pointer;
  transition: transform var(--duration-fast) var(--ease-spring),
              box-shadow var(--duration-fast) var(--ease-out-expo);

  &:hover {
    transform: translateX(2px);
    box-shadow: var(--gala-elevation-1);
  }

  // Gradient left-edge via ::before overlay (replaces solid color visually)
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
    pointer-events: none;
    border-radius: var(--gala-radius-md) 0 0 var(--gala-radius-md);
  }

  &--food      { border-left-color: #f59e0b; &::before { background: linear-gradient(to bottom, #f59e0b, #ea580c); } }
  &--hotel     { border-left-color: #a78bfa; &::before { background: linear-gradient(to bottom, #a78bfa, #7c3aed); } }
  &--transport { border-left-color: #38bdf8; &::before { background: linear-gradient(to bottom, #38bdf8, #6366f1); } }
  &--activity  { border-left-color: #fb7185; &::before { background: linear-gradient(to bottom, #fb7185, #e11d48); } }
  &--grocery   { border-left-color: #a3e635; &::before { background: linear-gradient(to bottom, #a3e635, #16a34a); } }
  &--other     { border-left-color: var(--muted); &::before { background: var(--muted); } }

  &__icon {
    width: 38px;
    height: 38px;
    border-radius: var(--gala-radius-sm);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__body {
    min-width: 0;
    flex: 1;
  }

  &__desc {
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--on-background);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    font-size: 0.75rem;
    color: var(--muted);
    margin-top: 1px;
  }

  &__right {
    text-align: right;
    flex-shrink: 0;
  }

  &__amount {
    font-size: 0.9rem;
    font-weight: 700;
    color: $primary;
    font-variant-numeric: tabular-nums;
  }

  &__payer {
    font-size: 0.72rem;
    color: var(--muted);
    margin-top: 1px;
  }
}

// ─── Expense total ────────────────────────────────────────────────────────────
.expense-total {
  padding: 12px 16px;
  background: rgba($primary, 0.04);
  border: 1px solid rgba($primary, 0.12);
  border-radius: var(--gala-radius-md);
}

// ─── Toolbar title fade transition ────────────────────────────────────────────
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-fast) ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
