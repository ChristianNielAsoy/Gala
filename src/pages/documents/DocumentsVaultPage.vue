<template>
  <q-page class="docs-page">

    <!-- ═══ Header ═══ -->
    <div class="docs-header gala-mesh-bg">
      <div class="docs-header__inner">
        <div class="docs-header__eyebrow">Secure Storage</div>
        <h1 class="docs-header__title">Documents Vault</h1>
        <p class="docs-header__sub">Passports, visas, tickets — all in one place.</p>
      </div>
    </div>

    <div class="docs-body">

      <!-- Controls -->
      <div class="docs-panel q-mb-md">  
        <div class="docs-controls">
          <q-select
            v-model="selectedTripId"
            :options="tripOptions"
            label="Filter by Trip (optional)"
            outlined dense clearable
            emit-value map-options
            class="docs-select col"
          >
            <template #prepend><q-icon name="flight_takeoff" color="primary" /></template>
          </q-select>

          <q-select
            v-model="selectedCategory"
            :options="categoryOptions"
            label="Category"
            outlined dense
            emit-value map-options
            class="docs-select col"
          >
            <template #prepend><q-icon name="folder" color="primary" /></template>
          </q-select>
        </div>

        <!-- Upload zone -->
        <div class="docs-upload-zone" @click="triggerUpload" @dragover.prevent @drop.prevent="onDrop">
          <q-icon name="cloud_upload" size="28px" color="primary" />
          <div class="docs-upload-zone__text">
            <span class="docs-upload-zone__label">Click or drag files to upload</span>
            <span class="docs-upload-zone__hint">PDF, images, Word · max 10 MB</span>
          </div>
          <input
            ref="fileInputRef"
            type="file"
            multiple
            accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
            style="display: none"
            @change="onInputChange"
          />
        </div>
      </div>

      <!-- Documents list -->
      <div v-if="documents.length > 0" class="docs-panel">
        <div v-for="(doc, idx) in filteredDocuments" :key="doc.id" class="docs-doc">
          <q-separator v-if="idx > 0" />
          <div class="docs-doc__row">
            <div class="docs-doc__icon-wrap" :class="`docs-doc__icon-wrap--${getDocType(doc.mime_type)}`">
              <q-icon :name="getDocumentIcon(doc.mime_type)" size="18px" />
            </div>
            <div class="docs-doc__info col">
              <span class="docs-doc__name">{{ doc.name }}</span>
              <span class="docs-doc__meta">
                {{ formatFileSize(doc.file_size) }}
                <span class="docs-doc__dot">·</span>
                <span class="docs-doc__cat">{{ doc.category }}</span>
                <span class="docs-doc__dot">·</span>
                {{ formatDate(doc.uploaded_at) }}
                <span v-if="doc.trip_id" class="docs-doc__trip">· {{ getTripName(doc.trip_id) }}</span>
              </span>
            </div>
            <q-btn flat round dense icon="download" size="sm" color="primary" @click="downloadDocument(doc)" />
            <q-btn flat round dense icon="delete_outline" size="sm" color="negative" @click="deleteDocument(doc)" />
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-else-if="!loading" class="docs-empty">
        <q-icon name="description" size="48px" color="grey-4" />
        <p class="docs-empty__text">No documents uploaded yet.</p>
        <p class="docs-empty__hint">Tap the upload zone above to add your first document.</p>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="docs-loading">
        <q-spinner color="primary" size="32px" />
      </div>

    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import { useAuthStore } from 'src/stores/authStore';
import { useTripStore } from 'src/stores/tripStore';

const $q = useQuasar();
const authStore = useAuthStore();
const tripStore = useTripStore();
const fileInputRef = ref<HTMLInputElement | null>(null);

interface DocumentItem {
  id: string;
  user_id: string;
  trip_id?: string;
  name: string;
  file_path: string;
  file_size: number;
  mime_type: string;
  category: string;
  uploaded_at: string;
}

const documents = ref<DocumentItem[]>([]);
const loading = ref(false);
const selectedTripId = ref<string | null>(null);
const selectedCategory = ref('general');

const tripOptions = computed(() =>
  tripStore.trips.map((t) => ({ label: t.name, value: t.id })),
);

const categoryOptions = [
  { label: 'Passport', value: 'passport' },
  { label: 'Visa', value: 'visa' },
  { label: 'Insurance', value: 'insurance' },
  { label: 'Tickets', value: 'tickets' },
  { label: 'Receipts', value: 'receipts' },
  { label: 'General', value: 'general' },
];

const filteredDocuments = computed(() =>
  selectedTripId.value
    ? documents.value.filter((d) => d.trip_id === selectedTripId.value)
    : documents.value,
);

function triggerUpload() {
  fileInputRef.value?.click();
}

function onInputChange(evt: Event) {
  const input = evt.target as HTMLInputElement;
  if (input.files?.length) {
    void onFilesAdded(Array.from(input.files));
    input.value = '';
  }
}

function onDrop(evt: DragEvent) {
  const files = Array.from(evt.dataTransfer?.files ?? []);
  if (files.length) void onFilesAdded(files);
}

async function fetchDocuments() {
  loading.value = true;
  try {
    const user = authStore.user;
    if (!user) return;
    const { data, error } = await supabase
      .from('documents').select('*')
      .eq('user_id', user.id)
      .order('uploaded_at', { ascending: false });
    if (error) throw error;
    documents.value = (data as DocumentItem[]) || [];
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to load documents' });
  } finally {
    loading.value = false;
  }
}

async function onFilesAdded(files: File[]) {
  const user = authStore.user;
  if (!user) {
    $q.notify({ type: 'negative', message: 'Please log in to upload documents' });
    return;
  }

  for (const file of files) {
    try {
      const fileExt = file.name.split('.').pop();
      const fileName = `${Date.now()}-${Math.random().toString(36).substring(2)}.${fileExt}`;
      const filePath = `documents/${user.id}/${fileName}`;

      const { error: uploadError } = await supabase.storage.from('documents').upload(filePath, file);
      if (uploadError) throw uploadError;

      const { error: dbError } = await supabase.from('documents').insert({
        user_id: user.id,
        trip_id: selectedTripId.value,
        name: file.name,
        file_path: filePath,
        file_size: file.size,
        mime_type: file.type,
        category: selectedCategory.value,
      });
      if (dbError) throw dbError;

      $q.notify({ type: 'positive', message: `Uploaded ${file.name}`, position: 'top' });
    } catch {
      $q.notify({ type: 'negative', message: `Failed to upload ${file.name}`, position: 'top' });
    }
  }
  await fetchDocuments();
}

async function downloadDocument(doc: DocumentItem) {
  try {
    const { data, error } = await supabase.storage.from('documents').download(doc.file_path);
    if (error) throw error;
    const url = URL.createObjectURL(data);
    const a = document.createElement('a');
    a.href = url;
    a.download = doc.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to download document' });
  }
}

function deleteDocument(doc: DocumentItem) {
  $q.dialog({
    title: 'Delete Document',
    message: `Are you sure you want to delete "${doc.name}"?`,
    cancel: true,
    persistent: true,
  }).onOk(() => { void deleteDocumentConfirmed(doc); });
}

async function deleteDocumentConfirmed(doc: DocumentItem) {
  try {
    await supabase.storage.from('documents').remove([doc.file_path]);
    const { error } = await supabase.from('documents').delete().eq('id', doc.id);
    if (error) throw error;
    $q.notify({ type: 'positive', message: 'Document deleted' });
    await fetchDocuments();
  } catch {
    $q.notify({ type: 'negative', message: 'Failed to delete document' });
  }
}

function getDocType(mimeType: string): string {
  if (mimeType.includes('pdf')) return 'pdf';
  if (mimeType.includes('image')) return 'image';
  return 'file';
}

function getDocumentIcon(mimeType: string): string {
  if (mimeType.includes('pdf')) return 'picture_as_pdf';
  if (mimeType.includes('image')) return 'image';
  if (mimeType.includes('word')) return 'description';
  return 'insert_drive_file';
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function getTripName(tripId: string): string {
  return tripStore.trips.find((t) => t.id === tripId)?.name || 'Unknown Trip';
}

onMounted(async () => {
  await Promise.all([tripStore.fetchTrips(), fetchDocuments()]);
});
</script>

<style scoped lang="scss">
.docs-page {
  min-height: 100vh;
  background: var(--background);
}

// ─── Header ────────────────────────────────────────────────────────────────────
.docs-header {
  padding: 48px 24px 40px;
}

.docs-header__inner {
  max-width: 600px;
  margin: 0 auto;
}

.docs-header__eyebrow {
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--muted);
  margin-bottom: 6px;
}

.docs-header__title {
  font-family: 'Instrument Serif', Georgia, serif;
  font-size: 2.25rem;
  line-height: 1.1;
  color: var(--on-background);
  margin: 0 0 6px;
}

.docs-header__sub {
  font-size: 0.9375rem;
  color: var(--muted);
  margin: 0;
}

// ─── Body ────────────────────────────────────────────────────────────────────
.docs-body {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px 16px 80px;
}

// ─── Panel ────────────────────────────────────────────────────────────────────
.docs-panel {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--gala-radius-lg);
  overflow: hidden;
}

.docs-controls {
  display: flex;
  gap: 12px;
  padding: 16px;
  flex-wrap: wrap;
}

.docs-select {
  flex: 1;
  min-width: 160px;

  :deep(.q-field__control) {
    border-radius: var(--gala-radius-md);
  }
}

// ─── Upload zone ────────────────────────────────────────────────────────────────
.docs-upload-zone {
  margin: 0 16px 16px;
  border: 2px dashed var(--border);
  border-radius: var(--gala-radius-md);
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;

  &:hover {
    border-color: #0D9488;
    background: rgba(13, 148, 136, 0.04);
  }
}

.docs-upload-zone__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.docs-upload-zone__label {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--on-background);
}

.docs-upload-zone__hint {
  font-size: 0.8125rem;
  color: var(--muted);
}

// ─── Document rows ────────────────────────────────────────────────────────────
.docs-doc__row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 12px 12px 16px;
}

.docs-doc__icon-wrap {
  width: 36px;
  height: 36px;
  border-radius: var(--gala-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &--pdf {
    background: rgba(239, 68, 68, 0.12);
    color: #EF4444;
  }
  &--image {
    background: rgba(59, 130, 246, 0.12);
    color: #3B82F6;
  }
  &--file {
    background: rgba(107, 114, 128, 0.12);
    color: #6B7280;
  }
}

.docs-doc__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.docs-doc__name {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--on-background);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.docs-doc__meta {
  font-size: 0.78rem;
  color: var(--muted);
}

.docs-doc__dot {
  margin: 0 4px;
}

.docs-doc__cat {
  text-transform: capitalize;
}

.docs-doc__trip {
  color: #0D9488;
  font-weight: 500;
}

// ─── Empty / Loading ────────────────────────────────────────────────────────────
.docs-empty {
  text-align: center;
  padding: 48px 24px;
  color: var(--muted);
}

.docs-empty__text {
  font-size: 0.9375rem;
  font-weight: 500;
  margin: 12px 0 4px;
  color: var(--on-background);
}

.docs-empty__hint {
  font-size: 0.875rem;
  color: var(--muted);
  margin: 0;
}

.docs-loading {
  display: flex;
  justify-content: center;
  padding: 48px;
}
</style>
