<template>
  <div class="itinerary-timeline q-pb-xl">
    <!-- FAB -->
    <q-page-sticky position="bottom-right" :offset="[18, 18]">
      <q-btn fab icon="add" color="primary" @click="openAddModal()" aria-label="Add item" />
    </q-page-sticky>

    <!-- Undo snackbar -->
    <div v-if="canUndo" class="undo-bar row items-center justify-between q-px-md q-py-sm">
      <div class="row items-center q-gutter-sm">
        <q-icon name="delete_outline" color="white" size="18px" />
        <span class="text-white text-body2">Item removed</span>
      </div>
      <q-btn flat no-caps dense label="Undo" color="white" @click="undoDelete" />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center q-pa-xl">
      <q-spinner color="primary" size="lg" />
    </div>

    <!-- Empty state (only when no date range to show) -->
    <div v-else-if="itineraryItems.length === 0 && !tripStartDate" class="text-center q-pa-xl q-mt-lg">
      <q-icon name="map" size="64px" color="grey-3" />
      <div class="text-h6 text-grey-5 q-mt-md">No itinerary yet</div>
      <div class="text-body2 text-grey-6 q-mb-lg">Plan activities, notes, and checklists for your trip</div>
      <q-btn label="Add first item" color="primary" no-caps unelevated icon="add" @click="openAddModal()" />
    </div>

    <!-- Day groups -->
    <template v-else>
      <div v-for="group in dayGroups" :key="group.key" class="day-section">
        <!-- Day header -->
        <div class="day-header q-px-md q-py-sm row items-center">
          <div class="day-dot-wrap q-mr-sm">
            <div
              class="day-dot"
              :class="group.isToday ? 'day-dot--today' : 'day-dot--normal'"
            />
          </div>
          <div class="col">
            <div
              class="text-subtitle2 text-weight-bold"
              :class="group.isToday ? 'text-primary' : ''"
            >
              {{ group.label }}
              <q-badge v-if="group.isToday" label="Today" color="primary" class="q-ml-xs" rounded />
            </div>
            <div class="text-caption text-grey-6">{{ group.dateLabel }}</div>
          </div>
          <q-btn
            flat round dense icon="add" size="sm" color="grey-7"
            @click="openAddModal(group.date ?? undefined)"
            aria-label="Add item to this day"
          />
        </div>

        <!-- Items -->
        <div class="items-container q-px-md q-pb-sm">
          <q-card
            v-for="item in group.items"
            :key="item.id"
            flat bordered
            class="item-card q-mb-sm"
            :class="`item-${item.type}`"
          >
            <q-card-section class="q-py-sm q-px-md">
              <div class="row items-start no-wrap">
                <!-- Type icon -->
                <q-icon
                  :name="getItemIcon(item.type)"
                  :color="getItemColor(item.type)"
                  size="16px"
                  class="q-mt-xs q-mr-sm flex-shrink-0"
                />

                <!-- Content -->
                <div class="col">
                  <div class="row items-center no-wrap q-gutter-x-xs">
                    <div class="text-body2 text-weight-medium col text-dark">{{ item.title }}</div>
                    <q-chip
                      v-if="item.time"
                      dense :label="item.time"
                      color="grey-2" text-color="grey-8"
                      size="xs" class="q-ml-xs q-mr-none flex-shrink-0"
                    />
                  </div>

                  <div v-if="item.location" class="row items-center q-mt-xs text-caption text-grey-6">
                    <q-icon name="place" size="12px" class="q-mr-xs" />
                    <span>{{ item.location }}</span>
                  </div>

                  <div v-if="item.notes" class="text-caption text-grey-7 q-mt-xs">{{ item.notes }}</div>

                  <q-chip
                    v-if="item.estimatedCost"
                    dense icon="savings"
                    :label="`~${currencyCode} ${item.estimatedCost}`"
                    color="blue-grey-1" text-color="blue-grey-8"
                    size="xs" class="q-mt-xs q-ml-none"
                  />

                  <!-- Checklist progress -->
                  <div v-if="item.checklist?.length" class="q-mt-sm">
                    <div class="row items-center no-wrap q-gutter-x-sm">
                      <q-linear-progress
                        :value="checklistProgress(item.checklist)"
                        :color="checklistProgress(item.checklist) === 1 ? 'positive' : 'info'"
                        size="4px" rounded class="col"
                      />
                      <div class="text-caption text-grey-6 flex-shrink-0">
                        {{ item.checklist.filter((c) => c.checked).length }}/{{ item.checklist.length }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 3-dot actions -->
                <q-btn flat round dense icon="more_vert" size="xs" class="q-ml-xs q-mt-xs flex-shrink-0">
                  <q-menu anchor="top right" self="top right" auto-close>
                    <q-list dense style="min-width: 140px">
                      <q-item clickable @click="editItem(item)">
                        <q-item-section avatar>
                          <q-icon name="edit" size="16px" />
                        </q-item-section>
                        <q-item-section>Edit</q-item-section>
                      </q-item>
                      <q-item clickable @click="duplicateItem(item)">
                        <q-item-section avatar>
                          <q-icon name="content_copy" size="16px" />
                        </q-item-section>
                        <q-item-section>Duplicate</q-item-section>
                      </q-item>
                      <q-separator />
                      <q-item clickable @click="deleteItem(item.id)" class="text-negative">
                        <q-item-section avatar>
                          <q-icon name="delete" size="16px" color="negative" />
                        </q-item-section>
                        <q-item-section>Delete</q-item-section>
                      </q-item>
                    </q-list>
                  </q-menu>
                </q-btn>
              </div>
            </q-card-section>
          </q-card>

          <!-- Empty day hint -->
          <div v-if="!group.items.length" class="text-caption text-grey-5 q-py-xs q-pl-xs">
            No activities — tap <q-icon name="add" size="12px" /> to add
          </div>
        </div>
      </div>
    </template>

    <!-- Add / Edit dialog -->
    <q-dialog v-model="showModal" persistent>
      <q-card style="min-width: min(440px, 92vw)">
        <q-card-section class="row items-center q-pb-none">
          <div class="text-h6">{{ isEditing ? 'Edit Item' : 'New Item' }}</div>
          <q-space />
          <q-btn icon="close" flat round dense @click="closeModal" />
        </q-card-section>

        <q-card-section class="q-pt-sm">
          <q-form class="q-gutter-y-sm">
            <!-- Type toggle -->
            <div class="row q-gutter-xs">
              <q-btn
                v-for="opt in itemTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :icon="opt.icon"
                :color="currentItem.type === opt.value ? 'primary' : 'grey-3'"
                :text-color="currentItem.type === opt.value ? 'white' : 'grey-8'"
                no-caps rounded unelevated size="sm"
                @click="currentItem.type = opt.value as ItineraryItemType"
              />
            </div>

            <!-- Title -->
            <q-input
              v-model="currentItem.title"
              label="Title *"
              outlined dense autofocus
            />

            <!-- Date + Time -->
            <div class="row q-gutter-sm">
              <q-input
                v-model="currentItem.date"
                label="Date"
                outlined dense class="col"
                type="date"
              />
              <q-input
                v-model="currentItem.time"
                label="Time"
                outlined dense class="col"
                type="time"
              />
            </div>

            <!-- Location -->
            <div class="row items-center no-wrap q-gutter-xs">
              <q-input
                v-model="currentItem.location"
                label="Location"
                outlined dense class="col"
                readonly
              />
              <q-btn icon="place" flat round dense color="primary" @click="showLocationPicker = true" />
            </div>
            <LocationPickerDialog
              v-model="showLocationPicker"
              :initial-lat="currentItem.lat"
              :initial-lng="currentItem.lng"
              :initial-label="currentItem.location"
              @picked="(loc) => { currentItem.location = loc.label; currentItem.lat = loc.lat; currentItem.lng = loc.lng; }"
            />

            <!-- Notes -->
            <q-input
              v-model="currentItem.notes"
              label="Notes"
              outlined dense
              type="textarea" rows="2"
            />

            <!-- Estimated cost -->
            <q-input
              v-model.number="currentItem.estimatedCost"
              label="Estimated cost (optional)"
              outlined dense
              type="number"
              :prefix="currencyCode"
            />

            <!-- Checklist items -->
            <div v-if="currentItem.type === 'checklist'">
              <div class="text-caption text-grey-7 q-mb-xs">Checklist items</div>
              <div class="row items-center no-wrap q-gutter-xs q-mb-xs">
                <q-input
                  v-model="newChecklistItem"
                  placeholder="Add item and press Enter"
                  outlined dense class="col"
                  @keyup.enter="addChecklistItem"
                />
                <q-btn
                  icon="add" flat round dense color="primary"
                  :disable="!newChecklistItem.trim()"
                  @click="addChecklistItem"
                />
              </div>
              <q-list dense>
                <q-item
                  v-for="(c, idx) in currentItem.checklist"
                  :key="idx" dense class="q-px-none"
                >
                  <q-item-section>
                    <q-checkbox v-model="c.checked" :label="c.text" dense />
                  </q-item-section>
                  <q-item-section side>
                    <q-btn icon="close" flat round dense size="xs" color="grey-6" @click="removeChecklistItem(idx)" />
                  </q-item-section>
                </q-item>
              </q-list>
            </div>
          </q-form>
        </q-card-section>

        <q-card-actions align="right" class="q-px-md q-pb-md">
          <q-btn flat no-caps label="Cancel" color="grey-7" @click="closeModal" />
          <q-btn
            no-caps unelevated
            :label="isEditing ? 'Update' : 'Add'"
            color="primary"
            @click="saveItem"
            :disable="!currentItem.title.trim()"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue';
import { useQuasar } from 'quasar';
import type { ItineraryItem, ItineraryPhase, ItineraryItemType } from './itinerary.model';
import { fetchWeather } from '../../utils/weatherService';
import LocationPickerDialog from './LocationPickerDialog.vue';
import { supabase } from 'boot/supabase';

const props = defineProps<{
  modelValue?: ItineraryItem[];
  tripMembers?: { id: string; name: string; user_id?: string }[];
  tripId?: string | undefined;
  tripStartDate?: string | undefined;
  tripEndDate?: string | undefined;
}>();

const emit = defineEmits<{ 'update:modelValue': [value: ItineraryItem[]] }>();

const $q = useQuasar();

// ── State ──────────────────────────────────────────────────────────────────
const itineraryItems = ref<ItineraryItem[]>(props.modelValue ?? []);
const loading = ref(false);
const showModal = ref(false);
const isEditing = ref(false);
const currencyCode = ref('PHP');
const showLocationPicker = ref(false);
const newChecklistItem = ref('');
const canUndo = ref(false);
const deletedItems = ref<ItineraryItem[]>([]);
const undoTimeout = ref<ReturnType<typeof setTimeout> | null>(null);
const currentItem = ref<ItineraryItem>({ id: '', phase: 'on', title: '', type: 'text' });

const itemTypeOptions = [
  { label: 'Note', value: 'text', icon: 'notes' },
  { label: 'Checklist', value: 'checklist', icon: 'checklist' },
];

// ── Day groups ─────────────────────────────────────────────────────────────
interface DayGroup {
  key: string;
  label: string;
  dateLabel: string;
  date: string | null;
  isToday: boolean;
  phase: ItineraryPhase;
  items: ItineraryItem[];
}

const dayGroups = computed<DayGroup[]>(() => {
  const today = new Date().toISOString().slice(0, 10);
  const groups: DayGroup[] = [];
  const start = props.tripStartDate;
  const end = props.tripEndDate;

  // Before-trip bucket
  const beforeItems = itineraryItems.value.filter(
    (i) => i.phase === 'before' || (start && i.date && i.date < start),
  );
  groups.push({
    key: 'before',
    label: 'Before Trip',
    dateLabel: 'Preparations & planning',
    date: null,
    isToday: false,
    phase: 'before',
    items: beforeItems,
  });

  // Day-by-day
  if (start) {
    const startDate = new Date(start + 'T00:00:00');
    const endDate = end ? new Date(end + 'T00:00:00') : startDate;
    const current = new Date(startDate);
    let dayNum = 1;

    while (current <= endDate) {
      const dateStr = current.toISOString().slice(0, 10);
      groups.push({
        key: dateStr,
        label: `Day ${dayNum}`,
        dateLabel: current.toLocaleDateString('en-US', {
          weekday: 'long',
          month: 'short',
          day: 'numeric',
        }),
        date: dateStr,
        isToday: dateStr === today,
        phase: 'on',
        items: itineraryItems.value.filter((i) => i.date === dateStr),
      });
      current.setDate(current.getDate() + 1);
      dayNum++;
    }
  } else {
    // No dates set — single bucket
    groups.push({
      key: 'on',
      label: 'During Trip',
      dateLabel: 'Activities & events',
      date: null,
      isToday: false,
      phase: 'on',
      items: itineraryItems.value.filter((i) => i.phase === 'on' && !i.date),
    });
  }

  // After-trip bucket
  const afterItems = itineraryItems.value.filter(
    (i) => i.phase === 'after' || (end && i.date && i.date > end),
  );
  groups.push({
    key: 'after',
    label: 'After Trip',
    dateLabel: 'Follow-up & memories',
    date: null,
    isToday: false,
    phase: 'after',
    items: afterItems,
  });

  return groups;
});

// ── DB helpers ─────────────────────────────────────────────────────────────
async function persistItem(item: ItineraryItem) {
  if (!props.tripId) return;
  const { error } = await supabase.from('itinerary_events').upsert({
    id: item.id,
    trip_id: props.tripId,
    phase: item.phase,
    title: item.title,
    type: item.type,
    date: item.date ?? null,
    time: item.time ?? null,
    location: item.location ?? null,
    notes: item.notes ?? null,
    checklist: item.checklist ?? null,
    attachments: item.attachments?.length ? item.attachments : null,
    latitude: item.lat ?? null,
    longitude: item.lng ?? null,
    estimated_cost: item.estimatedCost ?? null,
  });
  if (error) {
    console.error('Failed to save itinerary item:', error);
    $q.notify({ type: 'negative', message: 'Failed to save item' });
  }
}

async function removeItemFromDB(id: string) {
  if (!props.tripId) return;
  const { error } = await supabase.from('itinerary_events').delete().eq('id', id);
  if (error) console.warn('Failed to delete itinerary item:', error);
}

async function fetchItems() {
  if (!props.tripId) return;
  loading.value = true;
  const { data, error } = await supabase
    .from('itinerary_events')
    .select('*')
    .eq('trip_id', props.tripId)
    .order('date', { ascending: true, nullsFirst: true })
    .order('time', { ascending: true, nullsFirst: true });
  loading.value = false;
  if (error) { console.warn('Failed to load itinerary:', error); return; }
  if (data) {
    itineraryItems.value = data.map((row: Record<string, unknown>) => ({
      ...row,
      lat: (row.latitude as number | null) ?? undefined,
      lng: (row.longitude as number | null) ?? undefined,
      estimatedCost: (row.estimated_cost as number | null) ?? undefined,
    })) as ItineraryItem[];
    emit('update:modelValue', itineraryItems.value);
  }
}

// ── Modal ──────────────────────────────────────────────────────────────────
function openAddModal(date?: string) {
  isEditing.value = false;
  const phase: ItineraryPhase =
    date && props.tripStartDate && date < props.tripStartDate
      ? 'before'
      : date && props.tripEndDate && date > props.tripEndDate
        ? 'after'
        : 'on';

  currentItem.value = {
    id: crypto.randomUUID(),
    phase,
    title: '',
    type: 'text',
    ...(date !== undefined && { date }),
  };
  newChecklistItem.value = '';
  showModal.value = true;
}

function editItem(item: ItineraryItem) {
  isEditing.value = true;
  currentItem.value = { ...item };
  newChecklistItem.value = '';
  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
}

function saveItem() {
  if (!currentItem.value.title.trim()) return;

  // Auto-set phase from date
  const d = currentItem.value.date;
  if (d) {
    if (props.tripStartDate && d < props.tripStartDate) currentItem.value.phase = 'before';
    else if (props.tripEndDate && d > props.tripEndDate) currentItem.value.phase = 'after';
    else currentItem.value.phase = 'on';
  }

  if (isEditing.value) {
    const idx = itineraryItems.value.findIndex((i) => i.id === currentItem.value.id);
    if (idx !== -1) itineraryItems.value[idx] = { ...currentItem.value };
  } else {
    itineraryItems.value.push({ ...currentItem.value });
    if (currentItem.value.location) void fetchWeatherForItem(currentItem.value);
  }

  void persistItem(currentItem.value);
  emit('update:modelValue', itineraryItems.value);
  closeModal();
}

function deleteItem(id: string) {
  const item = itineraryItems.value.find((i) => i.id === id);
  if (!item) return;
  deletedItems.value.push(item);
  itineraryItems.value = itineraryItems.value.filter((i) => i.id !== id);
  canUndo.value = true;
  if (undoTimeout.value) clearTimeout(undoTimeout.value);
  undoTimeout.value = setTimeout(() => {
    for (const i of deletedItems.value) void removeItemFromDB(i.id);
    canUndo.value = false;
    deletedItems.value = [];
  }, 5000);
  emit('update:modelValue', itineraryItems.value);
}

function undoDelete() {
  const item = deletedItems.value.pop();
  if (item) itineraryItems.value.push(item);
  canUndo.value = false;
  if (undoTimeout.value) { clearTimeout(undoTimeout.value); undoTimeout.value = null; }
  emit('update:modelValue', itineraryItems.value);
}

function duplicateItem(item: ItineraryItem) {
  const dup: ItineraryItem = { ...item, id: crypto.randomUUID(), title: `${item.title} (Copy)` };
  itineraryItems.value.push(dup);
  void persistItem(dup);
  emit('update:modelValue', itineraryItems.value);
}

// ── Checklist ──────────────────────────────────────────────────────────────
function addChecklistItem() {
  if (!newChecklistItem.value.trim()) return;
  currentItem.value.checklist ??= [];
  currentItem.value.checklist.push({ text: newChecklistItem.value.trim(), checked: false });
  newChecklistItem.value = '';
}

function removeChecklistItem(idx: number) {
  currentItem.value.checklist?.splice(idx, 1);
}

function checklistProgress(checklist: { text: string; checked: boolean }[]): number {
  if (!checklist.length) return 0;
  return checklist.filter((c) => c.checked).length / checklist.length;
}

// ── Icon helpers ───────────────────────────────────────────────────────────
function getItemIcon(type: ItineraryItemType): string {
  if (type === 'checklist') return 'checklist';
  if (type === 'expense') return 'attach_money';
  return 'notes';
}

function getItemColor(type: ItineraryItemType): string {
  if (type === 'checklist') return 'info';
  if (type === 'expense') return 'positive';
  return 'grey-6';
}

// ── Weather ────────────────────────────────────────────────────────────────
async function fetchWeatherForItem(item: ItineraryItem) {
  if (!item.location) return;
  try {
    const w = await fetchWeather(item.location);
    item.weatherForecast = w.forecast;
    item.temperature = w.temperature;
    item.weatherIcon = w.icon;
  } catch {
    /* silent */
  }
}

// ── Watchers & mount ───────────────────────────────────────────────────────
watch(
  () => props.modelValue,
  (v) => { if (v) itineraryItems.value = v; },
  { immediate: true },
);

onMounted(() => { void fetchItems(); });
</script>

<style scoped lang="scss">
.itinerary-timeline {
  max-width: 720px;
  margin: 0 auto;
}

.day-section + .day-section {
  margin-top: 4px;
}

.day-header {
  position: sticky;
  top: 0;
  background: white;
  z-index: 5;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.q-dark .day-header {
  background: var(--q-dark);
}

.day-dot-wrap {
  width: 16px;
  display: flex;
  justify-content: center;
}

.day-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;

  &--today {
    background: $primary;
    box-shadow: 0 0 0 3px rgba($primary, 0.2);
  }

  &--normal {
    background: $grey-4;
  }
}

.items-container {
  padding-left: 36px;
}

.item-card {
  border-left: 3px solid transparent;
  transition: box-shadow 0.15s;

  &:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }

  &.item-text    { border-left-color: $grey-5; }
  &.item-checklist { border-left-color: $info; }
  &.item-expense { border-left-color: $positive; }
}

.undo-bar {
  position: fixed;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  min-width: 260px;
  background: #1e293b;
  border-radius: 12px;
  padding: 10px 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.flex-shrink-0 {
  flex-shrink: 0;
}
</style>
