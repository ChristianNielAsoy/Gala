<template>
  <div class="itinerary-timeline">
    <!-- Floating Action Button -->
    <q-page-sticky position="bottom-right" :offset="[18, 18]">
      <q-fab icon="add" color="primary" direction="up" class="q-mb-lg">
        <q-fab-action
          icon="event_note"
          color="primary"
          @click="openAddModal('before')"
          label="Before Trip"
        />
        <q-fab-action
          icon="flight_takeoff"
          color="secondary"
          @click="openAddModal('on')"
          label="During Trip"
        />
        <q-fab-action
          icon="event_available"
          color="accent"
          @click="openAddModal('after')"
          label="After Trip"
        />
      </q-fab>
    </q-page-sticky>

    <!-- Phase-based Timeline -->
    <q-timeline color="primary" class="q-mb-lg">
      <q-timeline-entry
        v-for="phase in phases"
        :key="phase.key"
        :title="phase.label"
        :subtitle="getPhaseSummary(phase.key)"
        :icon="getPhaseIcon(phase.key)"
        :color="getPhaseColor(phase.key)"
        class="phase-entry"
        :class="{ 'phase-collapsed': collapsedPhases[phase.key] }"
      >
        <!-- Phase Header with Controls -->
        <div class="phase-header row items-center justify-between q-mb-md">
          <div class="row items-center">
            <q-icon
              :name="getPhaseIcon(phase.key)"
              :color="getPhaseColor(phase.key)"
              class="q-mr-sm"
            />
            <div>
              <div class="text-h6 text-weight-bold">{{ phase.label }}</div>
              <div class="text-caption text-grey-7">{{ getPhaseSummary(phase.key) }}</div>
            </div>
          </div>

          <div class="row items-center q-gutter-sm">
            <q-btn
              :icon="collapsedPhases[phase.key] ? 'expand_more' : 'expand_less'"
              flat
              round
              dense
              @click="togglePhaseCollapse(phase.key)"
              aria-label="Toggle phase"
            />
            <q-btn
              icon="add"
              :color="getPhaseColor(phase.key)"
              round
              dense
              @click="openAddModal(phase.key)"
              aria-label="Add item"
            />
          </div>
        </div>

        <!-- Phase Summary Cards -->
        <div v-if="!collapsedPhases[phase.key]" class="phase-summary q-mb-md">
          <q-card flat bordered class="summary-card">
            <q-card-section class="row q-gutter-md">
              <div class="col-auto">
                <q-icon name="attach_money" color="positive" size="sm" />
                <div class="text-caption">Expenses</div>
                <div class="text-weight-medium">₱{{ getPhaseExpenses(phase.key) }}</div>
              </div>
              <div class="col-auto">
                <q-icon name="checklist" color="info" size="sm" />
                <div class="text-caption">Tasks</div>
                <div class="text-weight-medium">{{ getPhaseTasks(phase.key) }}</div>
              </div>
              <div class="col-auto">
                <q-icon name="event_note" color="secondary" size="sm" />
                <div class="text-caption">Notes</div>
                <div class="text-weight-medium">{{ getPhaseNotes(phase.key) }}</div>
              </div>
            </q-card-section>
          </q-card>
        </div>

        <!-- Draggable Items List -->
        <div v-if="!collapsedPhases[phase.key]">
          <vue-draggable-next
            v-model="phaseItems[phase.key]"
            group="itinerary"
            :animation="200"
            handle=".drag-handle"
            @end="onDragEnd"
            class="items-list"
          >
            <q-card
              v-for="item in itemsByPhase(phase.key)"
              :key="item.id"
              class="q-mb-sm item-card"
              :class="{
                'item-expense': item.type === 'expense',
                'item-checklist': item.type === 'checklist',
                'item-text': item.type === 'text',
                'item-editing': editingItemId === item.id,
              }"
              @contextmenu="showContextMenu($event, item)"
            >
              <q-card-section class="row items-center">
                <!-- Drag Handle -->
                <q-icon name="drag_indicator" class="drag-handle cursor-pointer q-mr-sm" />

                <!-- Item Content -->
                <div class="col" @click="startInlineEdit(item)">
                  <div class="row items-center q-mb-xs">
                    <q-icon
                      :name="getItemIcon(item.type)"
                      :color="getItemColor(item.type)"
                      size="sm"
                      class="q-mr-sm"
                    />
                    <div v-if="editingItemId !== item.id" class="text-weight-medium">
                      {{ item.title }}
                    </div>
                    <q-input
                      v-else
                      v-model="editingTitle"
                      dense
                      outlined
                      @blur="saveInlineEdit(item)"
                      @keyup.enter="saveInlineEdit(item)"
                      @keyup.escape="cancelInlineEdit"
                      class="inline-edit-input"
                      autofocus
                    />
                    <q-chip
                      v-if="item.type === 'expense' && item.amount"
                      :label="`₱${item.amount}`"
                      color="positive"
                      dense
                      class="q-ml-sm"
                    />
                    <q-chip
                      v-if="item.type === 'checklist' && item.checklist"
                      :label="`${item.checklist.filter((c) => c.checked).length}/${item.checklist.length}`"
                      :color="checklistProgress(item.checklist) === 1 ? 'positive' : 'warning'"
                      dense
                      class="q-ml-sm"
                    />
                  </div>

                  <!-- Checklist Progress -->
                  <div v-if="item.checklist && item.checklist.length" class="q-mb-xs">
                    <q-linear-progress
                      :value="checklistProgress(item.checklist)"
                      :color="checklistProgress(item.checklist) === 1 ? 'positive' : 'secondary'"
                      size="sm"
                      class="q-mb-xs"
                    />
                    <div class="text-caption">
                      {{ item.checklist.filter((c) => c.checked).length }}/{{
                        item.checklist.length
                      }}
                      completed
                    </div>
                  </div>

                  <!-- Location & Time -->
                  <div v-if="item.location || item.time" class="text-caption text-grey-7 q-mb-xs">
                    <q-icon name="location_on" size="xs" v-if="item.location" />
                    {{ item.location }}
                    <q-icon name="schedule" size="xs" v-if="item.time" class="q-ml-sm" />
                    {{ item.time }}
                  </div>

                  <!-- Notes -->
                  <div v-if="item.notes" class="text-body2 q-mb-xs">{{ item.notes }}</div>

                  <!-- Weather Information -->
                  <div v-if="item.weatherForecast" class="weather-info row items-center q-mb-xs">
                    <img
                      v-if="item.weatherIcon"
                      :src="item.weatherIcon"
                      alt="Weather Icon"
                      class="q-mr-sm"
                    />
                    <div>{{ item.weatherForecast }} ({{ item.temperature }}°C)</div>
                  </div>

                  <!-- Attachments -->
                  <div v-if="item.attachments && item.attachments.length" class="q-mb-xs">
                    <q-chip
                      v-for="(att, idx) in item.attachments.slice(0, 3)"
                      :key="idx"
                      icon="attach_file"
                      :label="getAttachmentName(att)"
                      dense
                      clickable
                      @click.stop="openAttachment(att)"
                      class="q-mr-xs q-mb-xs"
                    />
                    <q-chip
                      v-if="item.attachments.length > 3"
                      :label="`+${item.attachments.length - 3} more`"
                      dense
                      class="q-mr-xs q-mb-xs"
                    />
                  </div>
                </div>

                <!-- Action Buttons -->
                <div class="col-auto">
                  <q-btn
                    icon="edit"
                    flat
                    round
                    dense
                    @click.stop="editItem(item)"
                    aria-label="Edit item"
                  />
                  <q-btn
                    icon="delete"
                    flat
                    round
                    dense
                    color="negative"
                    @click.stop="deleteItem(item.id)"
                    aria-label="Delete item"
                  />
                </div>
              </q-card-section>

              <!-- Swipe Actions for Mobile -->
              <q-slide-item
                v-if="$q.platform.is.mobile"
                @left="
                  ({ reset }) => {
                    editItem(item);
                    reset();
                  }
                "
                @right="
                  ({ reset }) => {
                    deleteItem(item.id);
                    reset();
                  }
                "
                left-color="primary"
                right-color="negative"
                left-icon="edit"
                right-icon="delete"
              />
            </q-card>
          </vue-draggable-next>

          <!-- Empty State -->
          <div v-if="!itemsByPhase(phase.key).length" class="text-center text-grey-6 q-pa-lg">
            <q-icon name="event_note" size="xl" />
            <div class="q-mt-sm">No items yet</div>
            <q-btn
              label="Add first item"
              :color="getPhaseColor(phase.key)"
              flat
              @click="openAddModal(phase.key)"
              class="q-mt-sm"
            />
          </div>
        </div>
      </q-timeline-entry>
    </q-timeline>

    <!-- Context Menu -->
    <q-menu ref="contextMenu" touch-position>
      <q-list dense>
        <q-item clickable @click="contextItem && editItem(contextItem)">
          <q-item-section avatar>
            <q-icon name="edit" />
          </q-item-section>
          <q-item-section>Edit</q-item-section>
        </q-item>
        <q-item clickable @click="contextItem && duplicateItem(contextItem)">
          <q-item-section avatar>
            <q-icon name="content_copy" />
          </q-item-section>
          <q-item-section>Duplicate</q-item-section>
        </q-item>
        <q-separator />
        <q-item clickable @click="contextItem && deleteItem(contextItem.id)" class="text-negative">
          <q-item-section avatar>
            <q-icon name="delete" color="negative" />
          </q-item-section>
          <q-item-section>Delete</q-item-section>
        </q-item>
      </q-list>
    </q-menu>

    <!-- Undo/Redo Snackbar -->
    <q-banner v-if="canUndo" class="undo-banner" rounded>
      <template v-slot:avatar>
        <q-icon name="undo" />
      </template>
      Item deleted
      <template v-slot:action>
        <q-btn flat dense label="Undo" @click="undoDelete" />
      </template>
    </q-banner>

    <!-- Expense Form Modal -->
    <ExpenseFormModal
      v-model:show="showExpenseModal"
      :is-editing="isEditing"
      :trip-members="tripMembers || []"
      :currency-code="currencyCode"
      :initial-expense="initialExpenseData as any"
      @save="handleExpenseSave"
      @cancel="handleExpenseCancel"
    />

    <!-- Add/Edit Item Modal -->
    <q-dialog v-model="showModal" persistent>
      <q-card style="min-width: 400px; max-width: 90vw">
        <q-card-section>
          <div class="text-h6">{{ isEditing ? 'Edit' : 'Add' }} Itinerary Item</div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <q-form @submit="saveItem" class="q-gutter-y-md">
            <!-- BASIC ITEM FIELDS (for non-expense items) -->
            <template v-if="currentItem.type !== 'expense'">
              <!-- Type Selector -->
              <q-select
                v-model="currentItem.type"
                :options="itemTypeOptions"
                label="Type"
                outlined
                emit-value
                map-options
              />

              <!-- Title -->
              <q-input v-model="currentItem.title" label="Title" outlined required />

              <!-- Time -->
              <q-input v-model="currentItem.time" label="Time (optional)" outlined type="time" />

              <!-- Location -->
              <q-input v-model="currentItem.location" label="Location (optional)" outlined />

              <!-- Notes -->
              <q-input
                v-model="currentItem.notes"
                label="Notes (optional)"
                outlined
                type="textarea"
                rows="3"
              />

              <!-- Checklist Items -->
              <div v-if="currentItem.type === 'checklist'">
                <div class="text-subtitle2 q-mb-sm">Checklist Items</div>
                <q-input
                  v-model="newChecklistItem"
                  label="Add checklist item"
                  outlined
                  @keyup.enter="addChecklistItem"
                  class="q-mb-sm"
                >
                  <template v-slot:append>
                    <q-btn
                      icon="add"
                      flat
                      round
                      dense
                      @click="addChecklistItem"
                      :disable="!newChecklistItem.trim()"
                    />
                  </template>
                </q-input>
                <q-list dense>
                  <q-item v-for="(check, idx) in currentItem.checklist" :key="idx" class="q-mb-xs">
                    <q-item-section>
                      <q-checkbox v-model="check.checked" :label="check.text" />
                    </q-item-section>
                    <q-item-section side>
                      <q-btn
                        icon="delete"
                        flat
                        round
                        dense
                        color="negative"
                        @click="removeChecklistItem(idx)"
                      />
                    </q-item-section>
                  </q-item>
                </q-list>
              </div>
            </template>

            <!-- Attachments (for all item types) -->
            <div>
              <div class="text-subtitle2 q-mb-sm">Attachments (optional)</div>
              <q-file
                v-model="attachmentFiles"
                multiple
                outlined
                label="Select files"
                accept="image/*,.pdf,.doc,.docx"
                @rejected="onRejected"
              >
                <template v-slot:prepend>
                  <q-icon name="attach_file" />
                </template>
              </q-file>
              <div v-if="currentItem.attachments && currentItem.attachments.length" class="q-mt-sm">
                <q-chip
                  v-for="(att, idx) in currentItem.attachments"
                  :key="idx"
                  removable
                  @remove="removeAttachment(idx)"
                  class="q-mr-xs q-mb-xs"
                >
                  {{ att.split('/').pop() }}
                </q-chip>
              </div>
            </div>
          </q-form>
        </q-card-section>

        <q-card-actions align="right">
          <q-btn flat label="Cancel" color="grey-7" v-close-popup @click="closeModal" />
          <q-btn
            flat
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
import { VueDraggableNext } from 'vue-draggable-next';
import type {
  ItineraryItem,
  ItineraryPhase,
  ItineraryItemType,
  ExpenseItem,
} from './itinerary.model';
import { fetchWeather } from '../../utils/weatherService';
import ExpenseFormModal from './ExpenseFormModal.vue';
import { supabase } from 'boot/supabase';

// Types
type ExpenseFormData = {
  expenseForm: {
    description: string;
    amount: number | null;
    paid_by_id: string;
    category: string;
    date?: string;
    split_type: string;
  };
  splitMode: string;
  involvedMembers: string[];
  customSplits: Record<string, number>;
  items: ExpenseItem[];
  individualItems: ExpenseItem[];
  sharedItems: ExpenseItem[];
  giftedItemGifter: string;
  receiptFile: File | null;
};

const props = defineProps<{
  modelValue?: ItineraryItem[];
  tripMembers?: { id: string; name: string; user_id?: string }[];
  tripId?: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: ItineraryItem[]];
}>();

const $q = useQuasar();

// DB helpers
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
    amount: item.amount ?? null,
    paid_by_id: item.paid_by_id ?? null,
    category: item.category ?? null,
    split_type: item.split_type ?? null,
    involved_members: item.involved_members ?? null,
    custom_splits: item.custom_splits ?? null,
    expense_items: item.expense_items ?? null,
    receipt_url: item.receipt_url ?? null,
  });
  if (error) console.error('Failed to persist itinerary item:', error);
}

async function removeItemFromDB(id: string) {
  if (!props.tripId) return;
  const { error } = await supabase.from('itinerary_events').delete().eq('id', id);
  if (error) console.error('Failed to delete itinerary item:', error);
}

async function fetchItems() {
  if (!props.tripId) return;
  const { data, error } = await supabase
    .from('itinerary_events')
    .select('*')
    .eq('trip_id', props.tripId)
    .order('created_at');
  if (error) {
    console.error('Failed to load itinerary items:', error);
    return;
  }
  if (data) {
    itineraryItems.value = data as ItineraryItem[];
    updatePhaseItems();
  }
}

// Phases
const phases: { key: ItineraryPhase; label: string }[] = [
  { key: 'before', label: 'Before Trip' },
  { key: 'on', label: 'During Trip' },
  { key: 'after', label: 'After Trip' },
];

// Item types
const itemTypeOptions = [
  { label: 'Text Note', value: 'text' },
  { label: 'Checklist', value: 'checklist' },
  { label: 'Expense', value: 'expense' },
];

// Reactive data
const itineraryItems = ref<ItineraryItem[]>(props.modelValue ?? []);
const phaseItems = ref<Record<ItineraryPhase, ItineraryItem[]>>({
  before: [],
  on: [],
  after: [],
});

// Collapsed phases
const collapsedPhases = ref<Record<ItineraryPhase, boolean>>({
  before: false,
  on: false,
  after: false,
});

// Modal state
const showModal = ref(false);
const showExpenseModal = ref(false);
const isEditing = ref(false);
const editingExpenseItem = ref<ItineraryItem | null>(null);
const currentItem = ref<ItineraryItem>({
  id: '',
  phase: 'before',
  title: '',
  type: 'text',
});
const newChecklistItem = ref('');
const attachmentFiles = ref<File[]>([]);
const receiptFile = ref<File | null>(null);

// Currency code (could be made dynamic based on trip)
const currencyCode = ref('PHP');

// Get trip members (assuming this comes from props or route)
const tripMembers = ref<{ id: string; name: string; user_id?: string }[]>(props.tripMembers ?? []);
const initialExpenseData = computed(() => {
  if (!editingExpenseItem.value) return undefined;
  return {
    description: editingExpenseItem.value.title || '',
    amount: editingExpenseItem.value.amount || null,
    paid_by_id: editingExpenseItem.value.paid_by_id || '',
    category: editingExpenseItem.value.category || 'Food',
    date: editingExpenseItem.value.date,
    split_type: editingExpenseItem.value.split_type || 'equal',
    receipt_url: editingExpenseItem.value.receipt_url,
    involved_members: editingExpenseItem.value.involved_members,
    custom_splits: editingExpenseItem.value.custom_splits,
    expense_items: editingExpenseItem.value.expense_items,
    individual_items: editingExpenseItem.value.individual_items,
    shared_items: editingExpenseItem.value.shared_items,
    gifted_item_gifter: editingExpenseItem.value.gifted_item_gifter,
  };
});

// Helper function for itemized splits
// Inline editing
const editingItemId = ref<string | null>(null);
const editingTitle = ref('');

// Context menu
const contextMenu = ref();
const contextItem = ref<ItineraryItem | null>(null);

// Undo functionality
const deletedItems = ref<ItineraryItem[]>([]);
const canUndo = ref(false);
const undoTimeout = ref<NodeJS.Timeout | null>(null);

// Helper functions
function getItemIcon(type: ItineraryItemType): string {
  switch (type) {
    case 'text':
      return 'notes';
    case 'checklist':
      return 'checklist';
    case 'expense':
      return 'attach_money';
    default:
      return 'event';
  }
}

function getItemColor(type: ItineraryItemType): string {
  switch (type) {
    case 'text':
      return 'grey-7';
    case 'checklist':
      return 'info';
    case 'expense':
      return 'positive';
    default:
      return 'primary';
  }
}

function getPhaseIcon(phase: ItineraryPhase): string {
  switch (phase) {
    case 'before':
      return 'event_available';
    case 'on':
      return 'flight_takeoff';
    case 'after':
      return 'event_note';
    default:
      return 'event';
  }
}

function getPhaseColor(phase: ItineraryPhase): string {
  switch (phase) {
    case 'before':
      return 'primary';
    case 'on':
      return 'secondary';
    case 'after':
      return 'accent';
    default:
      return 'primary';
  }
}

function getPhaseSummary(phase: ItineraryPhase): string {
  const items = itemsByPhase(phase);
  const expenses = items.filter((item) => item.type === 'expense').length;
  const checklists = items.filter((item) => item.type === 'checklist').length;
  const notes = items.filter((item) => item.type === 'text').length;
  return `${items.length} items • ${expenses} expenses • ${checklists} checklists • ${notes} notes`;
}

function getPhaseExpenses(phase: ItineraryPhase): number {
  return itemsByPhase(phase)
    .filter((item) => item.type === 'expense' && item.amount)
    .reduce((sum, item) => sum + (item.amount || 0), 0);
}

function getPhaseTasks(phase: ItineraryPhase): string {
  const checklists = itemsByPhase(phase).filter((item) => item.type === 'checklist');
  const totalTasks = checklists.reduce((sum, item) => sum + (item.checklist?.length || 0), 0);
  const completedTasks = checklists.reduce(
    (sum, item) => sum + (item.checklist?.filter((c) => c.checked).length || 0),
    0,
  );
  return `${completedTasks}/${totalTasks}`;
}

function getPhaseNotes(phase: ItineraryPhase): number {
  return itemsByPhase(phase).filter((item) => item.type === 'text').length;
}

function getAttachmentName(url: string): string {
  return url.split('/').pop()?.split('.')[0] || 'File';
}

function togglePhaseCollapse(phase: ItineraryPhase): void {
  collapsedPhases.value[phase] = !collapsedPhases.value[phase];
}

function checklistProgress(checklist: { text: string; checked: boolean }[]): number {
  if (!checklist.length) return 0;
  return checklist.filter((c) => c.checked).length / checklist.length;
}

// Computed
const itemsByPhase = (phase: ItineraryPhase) => phaseItems.value[phase];

// Watch for prop changes
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal) {
      itineraryItems.value = newVal;
      updatePhaseItems();
    }
  },
  { immediate: true },
);

// Watch for trip members changes
watch(
  () => props.tripMembers,
  (newMembers) => {
    if (newMembers) {
      tripMembers.value = newMembers;
    }
  },
  { immediate: true },
);

// Watch for local changes
watch(
  itineraryItems,
  (val) => {
    emit('update:modelValue', val);
    updatePhaseItems();
  },
  { deep: true },
);

// Update phase items when itinerary changes
function updatePhaseItems() {
  phaseItems.value = {
    before: itineraryItems.value.filter((item) => item.phase === 'before'),
    on: itineraryItems.value.filter((item) => item.phase === 'on'),
    after: itineraryItems.value.filter((item) => item.phase === 'after'),
  };
}

// Modal functions
function openAddModal(phase: ItineraryPhase) {
  isEditing.value = false;
  currentItem.value = {
    id: Date.now().toString(),
    phase,
    title: '',
    type: 'text',
  };
  newChecklistItem.value = '';
  attachmentFiles.value = [];
  receiptFile.value = null;

  // For expenses, open the ExpenseFormModal instead
  if (currentItem.value.type === 'expense') {
    showExpenseModal.value = true;
    return;
  }

  showModal.value = true;
}

function editItem(item: ItineraryItem) {
  isEditing.value = true;
  currentItem.value = { ...item };
  newChecklistItem.value = '';
  attachmentFiles.value = [];
  receiptFile.value = null;

  // For expenses, open the ExpenseFormModal instead
  if (item.type === 'expense') {
    editingExpenseItem.value = item;
    showExpenseModal.value = true;
    return;
  }

  showModal.value = true;
}

function closeModal() {
  showModal.value = false;
  currentItem.value = {
    id: '',
    phase: 'before',
    title: '',
    type: 'text',
  };
}

function handleExpenseSave(expenseData: ExpenseFormData) {
  // Create or update the expense item
  const expenseItem: ItineraryItem = {
    id:
      isEditing.value && editingExpenseItem.value
        ? editingExpenseItem.value.id
        : Date.now().toString(),
    phase: currentItem.value.phase,
    title: expenseData.expenseForm.description,
    type: 'expense',
    ...(expenseData.expenseForm.amount && { amount: expenseData.expenseForm.amount }),
    paid_by_id: expenseData.expenseForm.paid_by_id,
    category: expenseData.expenseForm.category,
    ...(expenseData.expenseForm.date && { date: expenseData.expenseForm.date }),
    split_type: expenseData.splitMode as
      | 'equal'
      | 'custom'
      | 'itemized'
      | 'gifted'
      | 'individual_shared'
      | 'equalized_meals',
    involved_members: expenseData.involvedMembers,
    custom_splits: expenseData.customSplits,
    expense_items: expenseData.items,
    gifted_item_gifter: expenseData.giftedItemGifter,
    individual_items: expenseData.individualItems,
    shared_items: expenseData.sharedItems,
  };

  if (isEditing.value && editingExpenseItem.value) {
    // Update existing item
    const index = itineraryItems.value.findIndex(
      (item) => item.id === editingExpenseItem.value!.id,
    );
    if (index !== -1) {
      itineraryItems.value[index] = expenseItem;
    }
  } else {
    // Add new item
    itineraryItems.value.push(expenseItem);
  }

  // Close modal and reset state
  showExpenseModal.value = false;
  editingExpenseItem.value = null;
  isEditing.value = false;

  // Emit update event
  emit('update:modelValue', itineraryItems.value);

  // Persist to DB
  void persistItem(expenseItem);
}

function handleExpenseCancel() {
  showExpenseModal.value = false;
  editingExpenseItem.value = null;
  isEditing.value = false;
}

function saveItem() {
  // Validate basic fields for non-expense items
  if (!currentItem.value.title.trim()) return;

  if (isEditing.value) {
    const index = itineraryItems.value.findIndex((item) => item.id === currentItem.value.id);
    if (index !== -1) {
      itineraryItems.value[index] = { ...currentItem.value };
      // Fetch weather if location is set
      if (currentItem.value.location) {
        void fetchWeatherForItem(itineraryItems.value[index]);
      }
    }
  } else {
    const newItem = { ...currentItem.value };
    itineraryItems.value.push(newItem);
    // Fetch weather if location is set
    if (newItem.location) {
      void fetchWeatherForItem(newItem);
    }
  }

  void persistItem(currentItem.value);
  closeModal();
}

function deleteItem(id: string) {
  const itemToDelete = itineraryItems.value.find((item) => item.id === id);
  if (!itemToDelete) return;

  // Store for undo
  deletedItems.value.push(itemToDelete);

  // Remove from items
  itineraryItems.value = itineraryItems.value.filter((item) => item.id !== id);

  // Show undo banner
  canUndo.value = true;
  if (undoTimeout.value) clearTimeout(undoTimeout.value);
  undoTimeout.value = setTimeout(() => {
    // Undo window expired — commit deletes to DB
    for (const item of deletedItems.value) {
      void removeItemFromDB(item.id);
    }
    canUndo.value = false;
    deletedItems.value = [];
  }, 5000); // 5 seconds to undo
}

function undoDelete(): void {
  if (deletedItems.value.length > 0) {
    const itemToRestore = deletedItems.value.pop();
    if (itemToRestore) {
      itineraryItems.value.push(itemToRestore);
    }
  }
  canUndo.value = false;
  if (undoTimeout.value) {
    clearTimeout(undoTimeout.value);
    undoTimeout.value = null;
  }
}

// Inline editing functions
function startInlineEdit(item: ItineraryItem): void {
  if ($q.platform.is.mobile) return; // Disable inline edit on mobile
  editingItemId.value = item.id;
  editingTitle.value = item.title;
}

function saveInlineEdit(item: ItineraryItem): void {
  if (editingTitle.value.trim()) {
    item.title = editingTitle.value.trim();
    itineraryItems.value = [...itineraryItems.value]; // Trigger reactivity
    void persistItem(item);
  }
  cancelInlineEdit();
}

function cancelInlineEdit(): void {
  editingItemId.value = null;
  editingTitle.value = '';
}

// Context menu functions
function showContextMenu(event: MouseEvent, item: ItineraryItem): void {
  event.preventDefault();
  contextItem.value = item;
  contextMenu.value?.show(event);
}

function duplicateItem(item: ItineraryItem): void {
  const duplicatedItem: ItineraryItem = {
    ...item,
    id: Date.now().toString(),
    title: `${item.title} (Copy)`,
  };
  itineraryItems.value.push(duplicatedItem);
  void persistItem(duplicatedItem);
}

// Checklist functions
function addChecklistItem() {
  if (!newChecklistItem.value.trim()) return;
  if (!currentItem.value.checklist) currentItem.value.checklist = [];
  currentItem.value.checklist.push({
    text: newChecklistItem.value.trim(),
    checked: false,
  });
  newChecklistItem.value = '';
}

function removeChecklistItem(idx: number) {
  if (currentItem.value.checklist) {
    currentItem.value.checklist.splice(idx, 1);
  }
}

// Attachment functions
function onRejected() {
  $q.notify({
    type: 'negative',
    message: 'File type not supported',
  });
}

function removeAttachment(idx: number) {
  if (currentItem.value.attachments) {
    currentItem.value.attachments.splice(idx, 1);
  }
}

function openAttachment(url: string) {
  window.open(url, '_blank');
}

// Drag and drop
function onDragEnd(evt: {
  from: HTMLElement;
  to: HTMLElement;
  oldIndex: number;
  newIndex: number;
  item: HTMLElement;
}) {
  // Update phases based on new order
  const { from, to, oldIndex } = evt;
  if (from !== to) {
    // Item moved between phases
    const fromPhase = from.id as ItineraryPhase;
    const toPhase = to.id as ItineraryPhase;
    const item = phaseItems.value[fromPhase][oldIndex];
    if (item) {
      item.phase = toPhase;
      itineraryItems.value = [...itineraryItems.value]; // Trigger reactivity
      void persistItem(item);
    }
  }
}

// Weather functions
async function fetchWeatherForItem(item: ItineraryItem) {
  if (!item.location) return;
  try {
    const weatherData = await fetchWeather(item.location);
    item.weatherForecast = weatherData.forecast;
    item.temperature = weatherData.temperature;
    item.weatherIcon = weatherData.icon;
  } catch (error) {
    console.error(`Failed to fetch weather for ${item.title}:`, error);
  }
}

async function fetchWeatherForAllItems() {
  for (const phaseKey of Object.keys(phaseItems.value) as ItineraryPhase[]) {
    for (const item of phaseItems.value[phaseKey]) {
      if (item.location) {
        await fetchWeatherForItem(item);
      }
    }
  }
}

// Initialize
onMounted(async () => {
  await fetchItems();
  await fetchWeatherForAllItems();
});
</script>

<style scoped>
.itinerary-timeline {
  max-width: 800px;
  margin: 0 auto;
}

.phase-entry {
  margin-bottom: 2rem;
}

.phase-header {
  position: sticky;
  top: 0;
  background: var(--q-background);
  z-index: 10;
  padding: 1rem 0;
  border-bottom: 1px solid var(--q-separator-color);
}

.phase-collapsed .phase-header {
  border-bottom: none;
}

.phase-summary {
  margin: 0 -1rem;
}

.summary-card {
  background: linear-gradient(135deg, var(--q-primary) 0%, var(--q-secondary) 100%);
  color: white;
}

.summary-card .q-icon {
  opacity: 0.9;
}

.items-list {
  min-height: 50px;
}

.item-card {
  transition: all 0.2s ease;
  border-left: 4px solid transparent;
  position: relative;
}

.item-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.item-expense {
  border-left-color: #4caf50;
}

.item-checklist {
  border-left-color: #2196f3;
}

.item-text {
  border-left-color: #9c27b0;
}

.item-editing {
  box-shadow: 0 0 0 2px var(--q-primary);
}

.inline-edit-input {
  max-width: 300px;
}

.drag-handle {
  color: #9e9e9e;
  transition: color 0.2s ease;
}

.drag-handle:hover {
  color: #666;
}

.undo-banner {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  min-width: 300px;
}

/* Mobile responsiveness */
@media (max-width: 600px) {
  .itinerary-timeline {
    padding: 0 1rem;
  }

  .phase-header {
    position: static;
    padding: 0.5rem 0;
  }

  .summary-card .row {
    flex-direction: column;
    text-align: center;
  }

  .summary-card .col-auto {
    margin-bottom: 0.5rem;
  }

  .item-card .row {
    flex-direction: column;
    align-items: flex-start;
  }

  .item-card .col-auto {
    align-self: flex-end;
    margin-top: 0.5rem;
  }

  .inline-edit-input {
    width: 100%;
    max-width: none;
  }
}

/* Touch targets for mobile */
@media (hover: none) and (pointer: coarse) {
  .item-card {
    min-height: 60px;
  }

  .drag-handle,
  .q-btn {
    min-width: 44px;
    min-height: 44px;
  }
}

/* Dark mode support */
.q-dark .summary-card {
  background: linear-gradient(135deg, var(--q-primary-dark) 0%, var(--q-secondary-dark) 100%);
}

.q-dark .phase-header {
  background: var(--q-dark-background);
}
</style>
<parameter name="filePath"></parameter>
