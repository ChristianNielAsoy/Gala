<template>
  <div class="packing-tab">

    <!-- Progress pill -->
    <div v-if="items.length > 0" class="packing-progress-pill q-mb-md">
      <div class="packing-progress-pill__bar">
        <div class="packing-progress-pill__fill" :style="{ width: packingPct + '%' }" />
      </div>
      <span class="packing-progress-pill__label">{{ packedCount }} / {{ items.length }} packed</span>
    </div>

    <!-- Add item row -->
    <div class="packing-panel packing-add-row q-mb-md">
      <q-btn-dropdown
        flat dense
        :icon="getCatIcon(newItemCategory)"
        size="sm"
        color="grey-7"
        class="packing-cat-picker"
        no-caps
      >
        <q-list dense>
          <q-item
            v-for="cat in itemCategories"
            :key="cat.value"
            clickable v-close-popup
            @click="newItemCategory = cat.value"
            :active="newItemCategory === cat.value"
          >
            <q-item-section avatar><q-icon :name="cat.icon" size="18px" /></q-item-section>
            <q-item-section>{{ cat.label }}</q-item-section>
          </q-item>
        </q-list>
      </q-btn-dropdown>
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
        :disable="!newItemText.trim()"
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
            <!-- Inline editing -->
            <q-input
              v-if="editingItemId === item.id"
              v-model="editingText"
              dense borderless autofocus
              class="packing-item__edit-input"
              input-class="packing-item__edit-native"
              @keyup.enter="saveInlineEdit(item)"
              @keyup.escape="cancelInlineEdit"
              @blur="saveInlineEdit(item)"
            />
            <template v-else>
              <span class="packing-item__name" @click="startInlineEdit(item)">{{ item.item_name }}</span>
              <span v-if="item.quantity > 1" class="packing-item__qty">×{{ item.quantity }}</span>
            </template>
          </div>
          <span class="packing-item__cat-badge">
            <q-icon :name="getCatIcon(item.category)" size="12px" />
          </span>
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
      <p class="packing-empty__text">No items yet. Add something above!</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="packing-loading">
      <q-spinner color="primary" size="32px" />
    </div>

    <!-- Undo bar -->
    <div v-if="canUndo" class="undo-bar row items-center justify-between q-px-md q-py-sm">
      <div class="row items-center q-gutter-sm">
        <q-icon name="delete_outline" color="white" size="18px" />
        <span class="text-white text-body2">Item removed</span>
      </div>
      <q-btn flat no-caps dense label="Undo" color="white" @click="undoDelete" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';

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

const props = defineProps<{
  tripId: string;
  members?: { id: string; name: string; user_id?: string }[];
}>();

const $q = useQuasar();

const items = ref<PackingItem[]>([]);
const newItemText = ref('');
const newItemCategory = ref('other');
const activeCategory = ref('all');
const loading = ref(false);

// Inline editing
const editingItemId = ref<string | null>(null);
const editingText = ref('');

// Undo
const canUndo = ref(false);
const deletedItems = ref<PackingItem[]>([]);
const undoTimeout = ref<ReturnType<typeof setTimeout> | null>(null);

const categories = [
  { value: 'all', label: 'All', icon: 'checklist' },
  { value: 'clothes', label: 'Clothes', icon: 'checkroom' },
  { value: 'toiletries', label: 'Toiletries', icon: 'soap' },
  { value: 'electronics', label: 'Electronics', icon: 'devices' },
  { value: 'documents', label: 'Docs', icon: 'description' },
  { value: 'other', label: 'Other', icon: 'category' },
];

const itemCategories = categories.filter((c) => c.value !== 'all');

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

async function fetchPackingItems() {
  if (!props.tripId) return;
  loading.value = true;
  try {
    const { data, error } = await supabase
      .from('packing_items').select('*')
      .eq('trip_id', props.tripId).order('created_at');
    if (error) throw error;
    items.value = (data as PackingItem[]) || [];
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load packing list' });
  } finally {
    loading.value = false;
  }
}

async function addItem() {
  if (!newItemText.value.trim() || !props.tripId) return;
  try {
    const { data, error } = await supabase.from('packing_items').insert({
      trip_id: props.tripId,
      item_name: newItemText.value.trim(),
      category: newItemCategory.value,
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

// Soft delete with undo
function deleteItem(itemId: string) {
  const item = items.value.find((i) => i.id === itemId);
  if (!item) return;
  deletedItems.value.push(item);
  items.value = items.value.filter((i) => i.id !== itemId);
  canUndo.value = true;
  if (undoTimeout.value) clearTimeout(undoTimeout.value);
  undoTimeout.value = setTimeout(() => {
    deletedItems.value.forEach((i) => {
      void supabase.from('packing_items').delete().eq('id', i.id);
    });
    canUndo.value = false;
    deletedItems.value = [];
  }, 5000);
}

function undoDelete() {
  const item = deletedItems.value.pop();
  if (item) items.value.push(item);
  if (deletedItems.value.length === 0) {
    canUndo.value = false;
    if (undoTimeout.value) { clearTimeout(undoTimeout.value); undoTimeout.value = null; }
  }
}

// Inline editing
function startInlineEdit(item: PackingItem) {
  editingItemId.value = item.id;
  editingText.value = item.item_name;
}

async function saveInlineEdit(item: PackingItem) {
  if (!editingText.value.trim() || editingText.value.trim() === item.item_name) {
    cancelInlineEdit();
    return;
  }
  try {
    const { error } = await supabase
      .from('packing_items')
      .update({ item_name: editingText.value.trim() })
      .eq('id', item.id);
    if (error) throw error;
    item.item_name = editingText.value.trim();
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to rename item' });
  }
  editingItemId.value = null;
}

function cancelInlineEdit() {
  editingItemId.value = null;
}

onMounted(() => {
  if (props.tripId) void fetchPackingItems();
});

onUnmounted(() => {
  // Commit pending deletes immediately on unmount
  if (undoTimeout.value) {
    clearTimeout(undoTimeout.value);
    deletedItems.value.forEach((i) => {
      void supabase.from('packing_items').delete().eq('id', i.id);
    });
    deletedItems.value = [];
    canUndo.value = false;
  }
});
</script>

<style scoped lang="scss">
// ─── Progress pill ──────────────────────────────────────────────────────────
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

// ─── Panel ────────────────────────────────────────────────────────────────────
.packing-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

// ─── Add row ────────────────────────────────────────────────────────────────────
.packing-add-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px 6px 6px;
}

.packing-cat-picker {
  flex-shrink: 0;
}

.packing-add-input {
  :deep(.q-field__native) {
    font-size: 0.9375rem;
    color: var(--on-background);
    &::placeholder { color: var(--muted); }
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

  &:hover { border-color: #0D9488; color: #0D9488; }
  &--active { background: #0D9488; border-color: #0D9488; color: #fff; }
}

// ─── Items ────────────────────────────────────────────────────────────────────
.packing-item__row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px 10px 4px;
}

.packing-item__check { flex-shrink: 0; }

.packing-item__text {
  display: flex;
  align-items: center;
  gap: 6px;
}

.packing-item__name {
  font-size: 0.9375rem;
  color: var(--on-background);
  cursor: pointer;
  transition: color 0.2s, text-decoration 0.2s;

  &:hover { color: $primary; }
}

.packing-item--packed .packing-item__name {
  color: var(--muted);
  text-decoration: line-through;
}

.packing-item__edit-input {
  :deep(.q-field__control) { padding: 0; min-height: unset; }
  :deep(.q-field__bottom) { display: none; }
}

.packing-item__edit-native {
  font-size: 0.9375rem;
  color: var(--on-background);
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

// ─── Undo bar ───────────────────────────────────────────────────────────────────
.undo-bar {
  position: fixed;
  bottom: 64px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 999;
  background: #1e293b;
  border-radius: var(--gala-radius-pill);
  padding: 8px 16px;
  min-width: 260px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
}
</style>
