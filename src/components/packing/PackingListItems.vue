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
        <q-item-label caption class="row items-center q-gutter-xs q-mt-xs">
          <span v-if="item.notes">{{ item.notes }}</span>
          <q-chip
            v-if="assigneeName(item.assigned_to_member_id)"
            dense
            icon="person"
            size="sm"
            color="primary"
            text-color="white"
            class="q-ma-none"
          >
            {{ assigneeName(item.assigned_to_member_id) }}
          </q-chip>
        </q-item-label>
      </q-item-section>

      <q-item-section side class="q-gutter-xs" style="flex-direction: row; align-items: center">
        <!-- Assignee picker -->
        <q-select
          v-if="members.length > 0"
          :model-value="item.assigned_to_member_id ?? null"
          :options="memberOptions"
          outlined
          dense
          clearable
          emit-value
          map-options
          placeholder="Assign"
          style="min-width: 100px"
          @update:model-value="updateAssignee(item.id, $event)"
          @click.stop
        />

        <!-- Category picker -->
        <q-select
          :model-value="item.category"
          :options="categoryOptions"
          outlined
          dense
          emit-value
          map-options
          style="min-width: 110px"
          @update:model-value="updateCategory(item.id, $event)"
          @click.stop
        />

        <q-btn
          icon="delete"
          flat
          dense
          color="negative"
          @click.stop="deleteItem(item.id)"
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
import { computed } from 'vue';

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

const props = defineProps<{
  items: PackingItem[];
  members?: Member[];
}>();

const emit = defineEmits<{
  toggle: [itemId: string];
  delete: [itemId: string];
  'update-category': [itemId: string, category: string];
  'update-assignee': [itemId: string, memberId: string | null];
}>();

const categoryOptions = [
  { label: 'Clothes', value: 'clothes' },
  { label: 'Toiletries', value: 'toiletries' },
  { label: 'Electronics', value: 'electronics' },
  { label: 'Documents', value: 'documents' },
  { label: 'Other', value: 'other' },
];

const members = computed(() => props.members ?? []);

const memberOptions = computed(() =>
  members.value.map((m) => ({ label: m.name, value: m.id })),
);

function assigneeName(memberId: string | null | undefined): string | null {
  if (!memberId) return null;
  return members.value.find((m) => m.id === memberId)?.name ?? null;
}

function toggleItem(itemId: string) {
  emit('toggle', itemId);
}

function deleteItem(itemId: string) {
  emit('delete', itemId);
}

function updateCategory(itemId: string, category: string) {
  emit('update-category', itemId, category);
}

function updateAssignee(itemId: string, memberId: string | null) {
  emit('update-assignee', itemId, memberId);
}
</script>

<style scoped>
.text-strike {
  text-decoration: line-through;
}
</style>
