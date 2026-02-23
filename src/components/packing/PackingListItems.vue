<template>
  <q-list bordered separator>
    <q-item v-for="item in props.items" :key="item.id" clickable @click="toggleItem(item.id)">
      <q-item-section avatar>
        <q-checkbox
          :model-value="item.is_packed"
          @update:model-value="toggleItem(item.id)"
          color="primary"
        />
      </q-item-section>

      <q-item-section>
        <q-item-label :class="{ 'text-strike text-grey-6': item.is_packed }">
          {{ item.item_name }}
          <q-badge
            v-if="item.quantity > 1"
            :label="item.quantity"
            color="grey-4"
            text-color="dark"
            class="q-ml-sm"
          />
        </q-item-label>
        <q-item-label caption v-if="item.notes">
          {{ item.notes }}
        </q-item-label>
      </q-item-section>

      <q-item-section side>
        <q-select
          :model-value="item.category"
          :options="categoryOptions"
          outlined
          dense
          emit-value
          map-options
          @update:model-value="updateCategory(item.id, $event)"
          style="min-width: 120px"
        />
        <q-btn
          icon="delete"
          flat
          dense
          color="negative"
          @click.stop="deleteItem(item.id)"
          class="q-ml-sm"
        />
      </q-item-section>
    </q-item>

    <div v-if="items.length === 0" class="text-center q-pa-md text-grey-6">
      <q-icon name="inventory_2" size="48px" class="q-mb-sm" />
      <p>No items in this category yet.</p>
    </div>
  </q-list>
</template>

<script setup lang="ts">
import { defineProps, defineEmits } from 'vue';

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

const props = defineProps<{
  items: PackingItem[];
}>();

const emit = defineEmits<{
  toggle: [itemId: string];
  delete: [itemId: string];
  updateCategory: [itemId: string, category: string];
}>();

const categoryOptions = [
  { label: 'Clothes', value: 'clothes' },
  { label: 'Toiletries', value: 'toiletries' },
  { label: 'Electronics', value: 'electronics' },
  { label: 'Documents', value: 'documents' },
  { label: 'Other', value: 'other' },
];

function toggleItem(itemId: string) {
  emit('toggle', itemId);
}

function deleteItem(itemId: string) {
  emit('delete', itemId);
}

function updateCategory(itemId: string, category: string) {
  emit('updateCategory', itemId, category);
}
</script>

<style scoped>
.text-strike {
  text-decoration: line-through;
}
</style>
