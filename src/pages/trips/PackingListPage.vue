<template>
  <q-page class="packing-page">

    <!-- ═══ Header ═══ -->
    <div class="packing-header gala-mesh-bg">
      <div class="packing-header__inner">
        <div class="packing-header__eyebrow">Trip Prep</div>
        <h1 class="packing-header__title">Packing List</h1>
        <p class="packing-header__sub">Never forget a thing.</p>

        <!-- Progress pill -->
        <div v-if="items.length > 0" class="packing-progress-pill">
          <div class="packing-progress-pill__bar">
            <div
              class="packing-progress-pill__fill"
              :style="{ width: packingPct + '%' }"
            />
          </div>
          <span class="packing-progress-pill__label">
            {{ packedCount }} / {{ items.length }} packed
          </span>
        </div>
      </div>
    </div>

    <div class="packing-body">

      <!-- Trip selector -->
      <div class="packing-panel q-mb-md">
        <q-select
          v-model="selectedTripId"
          :options="tripOptions"
          label="Select Trip"
          outlined dense
          emit-value map-options
          class="packing-select"
        >
          <template #prepend><q-icon name="flight_takeoff" color="primary" /></template>
        </q-select>
      </div>

      <!-- Add item row -->
      <div class="packing-panel packing-add-row q-mb-md">
        <q-input
          v-model="newItemText"
          placeholder="Add a packing item…"
          borderless dense
          class="packing-add-input col"
          @keyup.enter="addItem"
        />
        <q-btn
          round unelevated
          icon="add"
          color="primary"
          size="sm"
          :disable="!newItemText.trim() || !selectedTripId"
          @click="addItem"
        />
      </div>

      <!-- Category filter pills -->
      <div class="packing-cats q-mb-md">
        <button
          v-for="cat in categories"
          :key="cat.value"
          class="packing-cat-pill"
          :class="{ 'packing-cat-pill--active': activeCategory === cat.value }"
          @click="activeCategory = cat.value"
        >
          <q-icon :name="cat.icon" size="14px" />
          {{ cat.label }}
        </button>
      </div>

      <!-- Items list -->
      <div v-if="displayedItems.length > 0" class="packing-panel">
        <div
          v-for="(item, idx) in displayedItems"
          :key="item.id"
          class="packing-item"
          :class="{ 'packing-item--packed': item.is_packed }"
        >
          <q-separator v-if="idx > 0" />
          <div class="packing-item__row">
            <q-checkbox
              :model-value="item.is_packed"
              color="primary"
              @update:model-value="() => toggleItem(item.id)"
              class="packing-item__check"
            />
            <div class="packing-item__text col">
              <span class="packing-item__name">{{ item.item_name }}</span>
              <span v-if="item.quantity > 1" class="packing-item__qty">×{{ item.quantity }}</span>
            </div>
            <!-- Category badge -->
            <span class="packing-item__cat-badge">
              <q-icon :name="getCatIcon(item.category)" size="12px" />
            </span>
            <!-- Delete -->
            <q-btn
              flat round dense
              icon="close"
              size="xs"
              color="grey-5"
              @click="deleteItem(item.id)"
            />
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="!loading" class="packing-empty">
        <q-icon name="luggage" size="48px" color="grey-4" />
        <p class="packing-empty__text">
          {{ selectedTripId ? 'No items yet. Add something above!' : 'Select a trip to get started.' }}
        </p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="packing-loading">
        <q-spinner color="primary" size="32px" />
      </div>

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useTripStore } from 'src/stores/tripStore';
import PackingListItems from 'src/components/packing/PackingListItems.vue';

void PackingListItems; // imported for potential slot use

const $q = useQuasar();
const tripStore = useTripStore();

interface PackingItem {
  id: string;
  trip_id: string;
  item_name: string;
  category: string;
  is_packed: boolean;
  quantity: number;
  notes?: string;
  assigned_to_member_id?: string | null;
  created_at: string;
}

interface Member {
  id: string;
  name: string;
}

const items = ref<PackingItem[]>([]);
const members = ref<Member[]>([]);
const selectedTripId = ref<string>('');
const newItemText = ref('');
const activeCategory = ref('all');
const loading = ref(false);

const categories = [
  { value: 'all', label: 'All', icon: 'checklist' },
  { value: 'clothes', label: 'Clothes', icon: 'checkroom' },
  { value: 'toiletries', label: 'Toiletries', icon: 'soap' },
  { value: 'electronics', label: 'Electronics', icon: 'devices' },
  { value: 'documents', label: 'Docs', icon: 'description' },
  { value: 'other', label: 'Other', icon: 'category' },
];

const tripOptions = computed(() =>
  tripStore.trips.map((t) => ({ label: t.name, value: t.id })),
);

const displayedItems = computed(() =>
  activeCategory.value === 'all'
    ? items.value
    : items.value.filter((i) => i.category === activeCategory.value),
);

const packedCount = computed(() => items.value.filter((i) => i.is_packed).length);
const packingPct = computed(() =>
  items.value.length ? (packedCount.value / items.value.length) * 100 : 0,
);

function getCatIcon(category: string): string {
  return categories.find((c) => c.value === category)?.icon ?? 'category';
}

async function fetchMembers() {
  if (!selectedTripId.value) { members.value = []; return; }
  const { data } = await supabase
    .from('members').select('id, name').eq('trip_id', selectedTripId.value);
  members.value = (data as Member[]) || [];
}

async function fetchPackingItems() {
  if (!selectedTripId.value) { items.value = []; return; }
  loading.value = true;
  try {
    const { data, error } = await supabase
      .from('packing_items').select('*')
      .eq('trip_id', selectedTripId.value).order('created_at');
    if (error) throw error;
    items.value = (data as PackingItem[]) || [];
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load packing list' });
  } finally {
    loading.value = false;
  }
}

async function addItem() {
  if (!newItemText.value.trim() || !selectedTripId.value) return;
  try {
    const { data, error } = await supabase.from('packing_items').insert({
      trip_id: selectedTripId.value,
      item_name: newItemText.value.trim(),
      category: activeCategory.value === 'all' ? 'other' : activeCategory.value,
      is_packed: false,
      quantity: 1,
      assigned_to_member_id: null,
    }).select().single();
    if (error) throw error;
    items.value.push(data as PackingItem);
    newItemText.value = '';
    $q.notify({ type: 'positive', message: 'Item added' });
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to add item' });
  }
}

async function toggleItem(itemId: string) {
  const item = items.value.find((i) => i.id === itemId);
  if (!item) return;
  try {
    const { error } = await supabase
      .from('packing_items').update({ is_packed: !item.is_packed }).eq('id', itemId);
    if (error) throw error;
    item.is_packed = !item.is_packed;
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to update item' });
  }
}

async function deleteItem(itemId: string) {
  try {
    const { error } = await supabase.from('packing_items').delete().eq('id', itemId);
    if (error) throw error;
    items.value = items.value.filter((i) => i.id !== itemId);
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to remove item' });
  }
}

watch(selectedTripId, () => {
  void fetchPackingItems();
  void fetchMembers();
});

onMounted(async () => {
  await tripStore.fetchTrips();
  if (!selectedTripId.value && tripStore.trips.length > 0) {
    selectedTripId.value = tripStore.trips[0]!.id;
  }
  if (selectedTripId.value) {
    await Promise.all([fetchPackingItems(), fetchMembers()]);
  }
});
</script>

<style scoped lang="scss">
.packing-page {
  min-height: 100vh;
  background: var(--background);
}

// ─── Header ────────────────────────────────────────────────────────────────────
.packing-header {
  padding: 48px 24px 40px;
  position: relative;
}

.packing-header__inner {
  max-width: 600px;
  margin: 0 auto;
}

.packing-header__eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 6px;
}

.packing-header__title {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2.25rem;
  line-height: 1.1;
  color: var(--on-background);
  margin: 0 0 6px;
}

.packing-header__sub {
  font-size: 0.9375rem;
  color: var(--muted);
  margin: 0 0 20px;
}

// Progress pill
.packing-progress-pill {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-pill);
  padding: 6px 14px 6px 8px;
}

.packing-progress-pill__bar {
  width: 80px;
  height: 6px;
  background: var(--border);
  border-radius: 99px;
  overflow: hidden;
}

.packing-progress-pill__fill {
  height: 100%;
  background: #0D9488;
  border-radius: 99px;
  transition: width 0.4s ease;
}

.packing-progress-pill__label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--on-background);
}

// ─── Body ────────────────────────────────────────────────────────────────────
.packing-body {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px 16px 80px;
}

// ─── Panel ────────────────────────────────────────────────────────────────────
.packing-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

.packing-select {
  :deep(.q-field__control) {
    border-radius: var(--gala-radius-md);
    border: none;
    background: transparent;
  }
  :deep(.q-field--outlined .q-field__control::before) {
    border: none;
  }
}

// ─── Add row ────────────────────────────────────────────────────────────────────
.packing-add-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 16px;
}

.packing-add-input {
  :deep(.q-field__native) {
    font-size: 0.9375rem;
    color: var(--on-background);

    &::placeholder {
      color: var(--muted);
    }
  }
}

// ─── Category pills ────────────────────────────────────────────────────────────
.packing-cats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.packing-cat-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 14px;
  border-radius: var(--gala-radius-pill);
  border: 1px solid var(--border);
  background: var(--surface);
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--muted);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: #0D9488;
    color: #0D9488;
  }

  &--active {
    background: #0D9488;
    border-color: #0D9488;
    color: #fff;
  }
}

// ─── Items ────────────────────────────────────────────────────────────────────
.packing-item__row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px 10px 4px;
}

.packing-item__check {
  flex-shrink: 0;
}

.packing-item__text {
  display: flex;
  align-items: center;
  gap: 6px;
}

.packing-item__name {
  font-size: 0.9375rem;
  color: var(--on-background);
  transition: color 0.2s, text-decoration 0.2s;
}

.packing-item--packed .packing-item__name {
  color: var(--muted);
  text-decoration: line-through;
}

.packing-item__qty {
  font-size: 0.75rem;
  color: var(--muted);
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-pill);
  padding: 1px 7px;
}

.packing-item__cat-badge {
  color: var(--muted);
  flex-shrink: 0;
}

// ─── Empty / Loading ────────────────────────────────────────────────────────────
.packing-empty {
  text-align: center;
  padding: 48px 24px;
  color: var(--muted);
}

.packing-empty__text {
  font-size: 0.9375rem;
  margin: 12px 0 0;
}

.packing-loading {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
