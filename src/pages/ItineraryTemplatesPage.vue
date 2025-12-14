<template>
  <q-page class="q-pa-md bg-surface">
    <q-card flat bordered>
      <q-card-section>
        <div class="text-h6">Itinerary Templates</div>
        <div class="text-caption text-grey-7 q-mb-md">
          Create and manage reusable itinerary templates for your trips
        </div>

        <!-- Create New Template Button -->
        <q-btn
          color="primary"
          icon="add"
          label="Create Template"
          @click="showCreateDialog = true"
          class="q-mb-md"
        />

        <!-- Templates List -->
        <q-list bordered separator>
          <q-item
            v-for="template in templates"
            :key="template.id"
            clickable
            @click="useTemplate(template)"
          >
            <q-item-section avatar>
              <q-icon name="event_note" color="primary" />
            </q-item-section>

            <q-item-section>
              <q-item-label class="text-weight-medium">{{ template.name }}</q-item-label>
              <q-item-label caption>
                {{ template.items_count }} items • Created {{ formatDate(template.created_at) }}
              </q-item-label>
              <q-item-label caption v-if="template.description">
                {{ template.description }}
              </q-item-label>
            </q-item-section>

            <q-item-section side>
              <q-btn icon="edit" flat dense @click.stop="editTemplate(template)" hint="Edit" />
              <q-btn
                icon="delete"
                flat
                dense
                color="negative"
                @click.stop="deleteTemplate(template)"
                hint="Delete"
              />
            </q-item-section>
          </q-item>

          <div v-if="templates.length === 0 && !loading" class="text-center q-pa-md text-grey-6">
            <q-icon name="event_note" size="48px" class="q-mb-sm" />
            <p>No templates created yet.</p>
            <p class="text-caption">Create your first template to get started!</p>
          </div>

          <div v-if="loading" class="flex flex-center q-pa-md">
            <q-spinner color="primary" size="32px" />
          </div>
        </q-list>
      </q-card-section>
    </q-card>

    <!-- Create/Edit Template Dialog -->
    <q-dialog v-model="showCreateDialog" persistent>
      <q-card style="min-width: 500px">
        <q-card-section class="row items-center">
          <div class="text-h6">{{ editingTemplate ? 'Edit Template' : 'Create New Template' }}</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section>
          <q-form @submit="saveTemplate">
            <q-input
              v-model="templateForm.name"
              label="Template Name"
              outlined
              dense
              :rules="[(val: string) => !!val.trim() || 'Name is required']"
              class="q-mb-md"
            />

            <q-input
              v-model="templateForm.description"
              label="Description (Optional)"
              outlined
              dense
              type="textarea"
              rows="2"
              class="q-mb-md"
            />

            <!-- Template Items -->
            <div class="text-subtitle2 q-mb-sm">Template Items</div>

            <div v-for="(item, index) in templateForm.items" :key="index" class="q-mb-sm">
              <div class="row q-gutter-sm items-center">
                <div class="col">
                  <q-input
                    v-model="item.title"
                    :label="`Item ${index + 1}`"
                    outlined
                    dense
                    :rules="[(val: string) => !!val.trim() || 'Title is required']"
                  />
                </div>
                <div class="col-3">
                  <q-select
                    v-model="item.day"
                    :options="dayOptions"
                    label="Day"
                    outlined
                    dense
                    emit-value
                    map-options
                  />
                </div>
                <div class="col-auto">
                  <q-btn
                    icon="delete"
                    flat
                    dense
                    color="negative"
                    @click="removeTemplateItem(index)"
                    :disable="templateForm.items.length === 1"
                  />
                </div>
              </div>
            </div>

            <q-btn
              icon="add"
              label="Add Item"
              flat
              dense
              @click="addTemplateItem"
              class="q-mb-md"
            />

            <div class="row q-gutter-sm q-mt-md">
              <q-btn
                type="submit"
                :label="editingTemplate ? 'Update Template' : 'Create Template'"
                color="primary"
                :loading="saving"
              />
              <q-btn label="Cancel" flat v-close-popup @click="resetForm" />
            </div>
          </q-form>
        </q-card-section>
      </q-card>
    </q-dialog>

    <!-- Use Template Dialog -->
    <q-dialog v-model="showUseDialog" persistent>
      <q-card style="min-width: 400px">
        <q-card-section class="row items-center">
          <div class="text-h6">Use Template</div>
          <q-space />
          <q-btn icon="close" flat round dense v-close-popup />
        </q-card-section>

        <q-card-section>
          <div class="q-mb-md">Apply "{{ selectedTemplate?.name }}" to which trip?</div>

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

          <div class="row q-gutter-sm">
            <q-btn
              label="Apply Template"
              color="primary"
              @click="applyTemplate"
              :loading="applying"
              :disable="!selectedTripId"
            />
            <q-btn label="Cancel" flat v-close-popup @click="cancelUseTemplate" />
          </div>
        </q-card-section>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { useRouter } from 'vue-router';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';

const $q = useQuasar();
const router = useRouter();

interface ItineraryTemplate {
  id: string;
  user_id: string;
  name: string;
  description?: string;
  items: TemplateItem[];
  items_count: number;
  created_at: string;
}

interface TemplateItem {
  title: string;
  day: number;
}

const templates = ref<ItineraryTemplate[]>([]);
const trips = ref<Trip[]>([]);
const loading = ref(false);
const saving = ref(false);
const applying = ref(false);
const showCreateDialog = ref(false);
const showUseDialog = ref(false);
const editingTemplate = ref<ItineraryTemplate | null>(null);
const selectedTemplate = ref<ItineraryTemplate | null>(null);
const selectedTripId = ref<string>('');

const templateForm = ref({
  name: '',
  description: '',
  items: [{ title: '', day: 1 }] as TemplateItem[],
});

const tripOptions = ref<{ label: string; value: string }[]>([]);
const dayOptions = Array.from({ length: 30 }, (_, i) => ({
  label: `Day ${i + 1}`,
  value: i + 1,
}));

async function fetchTemplates() {
  loading.value = true;
  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) return;

    const { data, error } = await supabase
      .from('itinerary_templates')
      .select('id, user_id, name, description, items, items_count, created_at')
      .eq('user_id', user.id)
      .order('created_at', { ascending: false });

    if (error) throw error;
    templates.value = (data as ItineraryTemplate[]) || [];
  } catch (error) {
    console.error('Error fetching templates:', error);
    $q.notify({ type: 'negative', message: 'Failed to load templates' });
  } finally {
    loading.value = false;
  }
}

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
    }
  } catch (error) {
    console.error('Error fetching trips:', error);
  }
}

async function saveTemplate() {
  if (!templateForm.value.name.trim()) return;

  saving.value = true;
  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) return;

    const templateData = {
      user_id: user.id,
      name: templateForm.value.name.trim(),
      description: templateForm.value.description?.trim() || null,
      items: templateForm.value.items,
    };

    if (editingTemplate.value) {
      // Update existing template
      const { error } = await supabase
        .from('itinerary_templates')
        .update(templateData)
        .eq('id', editingTemplate.value.id);

      if (error) throw error;
      $q.notify({ type: 'positive', message: 'Template updated' });
    } else {
      // Create new template
      const { error } = await supabase.from('itinerary_templates').insert(templateData);

      if (error) throw error;
      $q.notify({ type: 'positive', message: 'Template created' });
    }

    showCreateDialog.value = false;
    resetForm();
    await fetchTemplates();
  } catch (error) {
    console.error('Error saving template:', error);
    $q.notify({ type: 'negative', message: 'Failed to save template' });
  } finally {
    saving.value = false;
  }
}

function editTemplate(template: ItineraryTemplate) {
  editingTemplate.value = template;
  // Load template data
  templateForm.value = {
    name: template.name,
    description: template.description || '',
    items: template.items || [{ title: '', day: 1 }],
  };
  showCreateDialog.value = true;
}

function deleteTemplate(template: ItineraryTemplate) {
  $q.dialog({
    title: 'Delete Template',
    message: `Are you sure you want to delete "${template.name}"?`,
    cancel: true,
    persistent: true,
  }).onOk(() => {
    void deleteTemplateConfirmed(template);
  });
}

async function deleteTemplateConfirmed(template: ItineraryTemplate) {
  try {
    const { error } = await supabase.from('itinerary_templates').delete().eq('id', template.id);

    if (error) throw error;

    $q.notify({ type: 'positive', message: 'Template deleted' });
    await fetchTemplates();
  } catch (error) {
    console.error('Error deleting template:', error);
    $q.notify({ type: 'negative', message: 'Failed to delete template' });
  }
}

function useTemplate(template: ItineraryTemplate) {
  selectedTemplate.value = template;
  showUseDialog.value = true;
}

async function applyTemplate() {
  if (!selectedTemplate.value || !selectedTripId.value) return;

  applying.value = true;
  try {
    // Fetch trip start date
    const { data: tripData, error: tripError } = await supabase
      .from('trips')
      .select('start_date')
      .eq('id', selectedTripId.value)
      .single();

    if (tripError) throw tripError;

    const startDate = new Date(tripData.start_date);

    // Get existing events to determine display_order
    const { data: existingEvents, error: eventsError } = await supabase
      .from('itinerary_events')
      .select('display_order')
      .eq('trip_id', selectedTripId.value)
      .order('display_order', { ascending: false })
      .limit(1);

    if (eventsError) throw eventsError;

    let nextOrder = (existingEvents?.[0]?.display_order || 0) + 1;

    // Prepare events to insert (sort by day first)
    const sortedItems = [...selectedTemplate.value.items].sort((a, b) => a.day - b.day);
    const eventsToInsert = sortedItems.map((item: TemplateItem) => {
      const eventDate = new Date(startDate);
      eventDate.setDate(startDate.getDate() + (item.day - 1));

      return {
        trip_id: selectedTripId.value,
        event_date: eventDate.toISOString().split('T')[0],
        title: item.title,
        display_order: nextOrder++,
      };
    });

    // Insert events
    const { error: insertError } = await supabase.from('itinerary_events').insert(eventsToInsert);

    if (insertError) throw insertError;

    // Navigate to trip details
    await router.push(`/trips/${selectedTripId.value}`);
    $q.notify({ type: 'positive', message: 'Template applied to trip' });
  } catch (error) {
    console.error('Error applying template:', error);
    $q.notify({ type: 'negative', message: 'Failed to apply template' });
  } finally {
    applying.value = false;
    showUseDialog.value = false;
  }
}

function cancelUseTemplate() {
  selectedTemplate.value = null;
  selectedTripId.value = '';
  showUseDialog.value = false;
}

function addTemplateItem() {
  templateForm.value.items.push({ title: '', day: 1 });
}

function removeTemplateItem(index: number) {
  if (templateForm.value.items.length > 1) {
    templateForm.value.items.splice(index, 1);
  }
}

function resetForm() {
  templateForm.value = {
    name: '',
    description: '',
    items: [{ title: '', day: 1 }],
  };
  editingTemplate.value = null;
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

onMounted(async () => {
  await Promise.all([fetchTemplates(), fetchTrips()]);
});
</script>
