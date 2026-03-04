<template>
  <q-page class="bg-surface">
    <!-- Header -->
    <div class="q-pa-md bg-surface">
      <div class="text-h5 text-weight-bold">Packing List</div>
      <div class="text-body2 text-grey-6 q-mt-xs">Create and manage your travel packing lists</div>
    </div>

    <div class="q-pa-md q-pt-none">
    <q-card flat bordered>
      <q-card-section>
        <!-- Trip Selector -->
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

        <!-- Add Item Input -->
        <div class="row q-gutter-sm q-mb-md">
          <div class="col">
            <q-input
              v-model="newItemText"
              label="Add packing item"
              outlined
              dense
              @keyup.enter="addItem"
              :rules="[(val: string) => !!val.trim() || 'Item name is required']"
            />
          </div>
          <div class="col-auto">
            <q-btn
              icon="add"
              color="primary"
              flat
              round
              @click="addItem"
              :disable="!newItemText.trim()"
            />
          </div>
        </div>

        <!-- Category Tabs -->
        <q-tabs v-model="activeCategory" class="q-mb-md">
          <q-tab name="all" label="All" />
          <q-tab name="clothes" label="Clothes" />
          <q-tab name="toiletries" label="Toiletries" />
          <q-tab name="electronics" label="Electronics" />
          <q-tab name="documents" label="Documents" />
          <q-tab name="other" label="Other" />
        </q-tabs>

        <q-tab-panels v-model="activeCategory">
          <q-tab-panel name="all">
            <PackingListItems
              :items="filteredItems"
              :members="members"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
              @update-assignee="updateItemAssignee"
            />
          </q-tab-panel>
          <q-tab-panel name="clothes">
            <PackingListItems
              :items="clothesItems"
              :members="members"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
              @update-assignee="updateItemAssignee"
            />
          </q-tab-panel>
          <q-tab-panel name="toiletries">
            <PackingListItems
              :items="toiletriesItems"
              :members="members"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
              @update-assignee="updateItemAssignee"
            />
          </q-tab-panel>
          <q-tab-panel name="electronics">
            <PackingListItems
              :items="electronicsItems"
              :members="members"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
              @update-assignee="updateItemAssignee"
            />
          </q-tab-panel>
          <q-tab-panel name="documents">
            <PackingListItems
              :items="documentsItems"
              :members="members"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
              @update-assignee="updateItemAssignee"
            />
          </q-tab-panel>
          <q-tab-panel name="other">
            <PackingListItems
              :items="otherItems"
              :members="members"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
              @update-assignee="updateItemAssignee"
            />
          </q-tab-panel>
        </q-tab-panels>
      </q-card-section>
    </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useTripStore } from 'src/stores/tripStore';
import PackingListItems from 'src/components/packing/PackingListItems.vue';

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

const tripOptions = computed(() =>
  tripStore.trips.map((t) => ({ label: t.name, value: t.id })),
);

const filteredItems = computed(() => items.value);
const clothesItems = computed(() => items.value.filter((item) => item.category === 'clothes'));
const toiletriesItems = computed(() =>
  items.value.filter((item) => item.category === 'toiletries'),
);
const electronicsItems = computed(() =>
  items.value.filter((item) => item.category === 'electronics'),
);
const documentsItems = computed(() => items.value.filter((item) => item.category === 'documents'));
const otherItems = computed(() => items.value.filter((item) => item.category === 'other'));

async function fetchMembers() {
  if (!selectedTripId.value) {
    members.value = [];
    return;
  }
  const { data } = await supabase
    .from('members')
    .select('id, name')
    .eq('trip_id', selectedTripId.value);
  members.value = (data as Member[]) || [];
}

async function fetchPackingItems() {
  if (!selectedTripId.value) {
    items.value = [];
    return;
  }

  loading.value = true;
  try {
    const { data, error } = await supabase
      .from('packing_items')
      .select('*')
      .eq('trip_id', selectedTripId.value)
      .order('created_at');

    if (error) throw error;
    items.value = (data as PackingItem[]) || [];
  } catch (error) {
    console.error('Error fetching packing items:', error);
    $q.notify({ type: 'negative', message: 'Failed to load packing list' });
  } finally {
    loading.value = false;
  }
}

async function addItem() {
  if (!newItemText.value.trim() || !selectedTripId.value) return;

  try {
    const { data, error } = await supabase
      .from('packing_items')
      .insert({
        trip_id: selectedTripId.value,
        item_name: newItemText.value.trim(),
        category: 'other',
        is_packed: false,
        quantity: 1,
        assigned_to_member_id: null,
      })
      .select()
      .single();

    if (error) throw error;

    items.value.push(data as PackingItem);
    newItemText.value = '';
    $q.notify({ type: 'positive', message: 'Item added to packing list' });
  } catch (error) {
    console.error('Error adding item:', error);
    $q.notify({ type: 'negative', message: 'Failed to add item' });
  }
}

async function toggleItem(itemId: string) {
  const item = items.value.find((i) => i.id === itemId);
  if (!item) return;

  try {
    const { error } = await supabase
      .from('packing_items')
      .update({ is_packed: !item.is_packed })
      .eq('id', itemId);

    if (error) throw error;

    item.is_packed = !item.is_packed;
  } catch (error) {
    console.error('Error toggling item:', error);
    $q.notify({ type: 'negative', message: 'Failed to update item' });
  }
}

async function deleteItem(itemId: string) {
  try {
    const { error } = await supabase.from('packing_items').delete().eq('id', itemId);

    if (error) throw error;

    items.value = items.value.filter((i) => i.id !== itemId);
    $q.notify({ type: 'positive', message: 'Item removed' });
  } catch (error) {
    console.error('Error deleting item:', error);
    $q.notify({ type: 'negative', message: 'Failed to remove item' });
  }
}

async function updateItemCategory(itemId: string, category: string) {
  const item = items.value.find((i) => i.id === itemId);
  if (!item) return;

  try {
    const { error } = await supabase.from('packing_items').update({ category }).eq('id', itemId);

    if (error) throw error;

    item.category = category;
  } catch (error) {
    console.error('Error updating category:', error);
    $q.notify({ type: 'negative', message: 'Failed to update category' });
  }
}

async function updateItemAssignee(itemId: string, memberId: string | null) {
  const item = items.value.find((i) => i.id === itemId);
  if (!item) return;

  try {
    const { error } = await supabase
      .from('packing_items')
      .update({ assigned_to_member_id: memberId })
      .eq('id', itemId);

    if (error) throw error;

    item.assigned_to_member_id = memberId;
  } catch (error) {
    console.error('Error updating assignee:', error);
    $q.notify({ type: 'negative', message: 'Failed to update assignee' });
  }
}

// Watch for trip selection changes
watch(selectedTripId, () => {
  void fetchPackingItems();
  void fetchMembers();
});

onMounted(async () => {
  await tripStore.fetchTrips();
  // Auto-select first trip if available
  if (!selectedTripId.value && tripStore.trips.length > 0) {
    selectedTripId.value = tripStore.trips[0]!.id;
  }
  if (selectedTripId.value) {
    await Promise.all([fetchPackingItems(), fetchMembers()]);
  }
});
</script>
