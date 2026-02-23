<template>
  <q-page class="q-pa-md bg-surface">
    <q-card flat bordered>
      <q-card-section>
        <div class="text-h6">Packing List</div>
        <div class="text-caption text-grey-7 q-mb-md">
          Create and manage your travel packing lists
        </div>

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
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
            />
          </q-tab-panel>
          <q-tab-panel name="clothes">
            <PackingListItems
              :items="clothesItems"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
            />
          </q-tab-panel>
          <q-tab-panel name="toiletries">
            <PackingListItems
              :items="toiletriesItems"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
            />
          </q-tab-panel>
          <q-tab-panel name="electronics">
            <PackingListItems
              :items="electronicsItems"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
            />
          </q-tab-panel>
          <q-tab-panel name="documents">
            <PackingListItems
              :items="documentsItems"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
            />
          </q-tab-panel>
          <q-tab-panel name="other">
            <PackingListItems
              :items="otherItems"
              @toggle="toggleItem"
              @delete="deleteItem"
              @update-category="updateItemCategory"
            />
          </q-tab-panel>
        </q-tab-panels>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';
import PackingListItems from 'src/components/packing/PackingListItems.vue';

const $q = useQuasar();

interface PackingItem {
  id: string;
  trip_id: string;
  item_name: string;
  category: string;
  is_packed: boolean;
  quantity: number;
  notes?: string;
  created_at: string;
}

const items = ref<PackingItem[]>([]);
const trips = ref<Trip[]>([]);
const selectedTripId = ref<string>('');
const newItemText = ref('');
const activeCategory = ref('all');
const loading = ref(false);

const tripOptions = ref<{ label: string; value: string }[]>([]);

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

async function fetchTrips() {
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
      tripOptions.value = trips.value.map((t) => ({
        label: t.name,
        value: t.id,
      }));

      // Auto-select first trip if available
      if (trips.value.length > 0 && !selectedTripId.value) {
        selectedTripId.value = trips.value[0]!.id;
      }
    }
  } catch (error) {
    console.error('Error fetching trips:', error);
  }
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

// Watch for trip selection changes
import { watch } from 'vue';
watch(selectedTripId, () => {
  void fetchPackingItems();
});

onMounted(async () => {
  await fetchTrips();
  if (selectedTripId.value) {
    await fetchPackingItems();
  }
});
</script>
