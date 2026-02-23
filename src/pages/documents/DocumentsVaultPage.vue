<template>
  <q-page class="q-pa-md bg-surface">
    <q-card flat bordered>
      <q-card-section>
        <div class="text-h6">Documents Vault</div>
        <div class="text-caption text-grey-7 q-mb-md">
          Upload and organize your travel documents
        </div>

        <!-- Trip Selector -->
        <q-select
          v-model="selectedTripId"
          :options="tripOptions"
          label="Select Trip (Optional)"
          outlined
          dense
          emit-value
          map-options
          clearable
          class="q-mb-md"
        />

        <!-- Category Selector -->
        <q-select
          v-model="selectedCategory"
          :options="categoryOptions"
          label="Document Category"
          outlined
          dense
          emit-value
          map-options
          class="q-mb-md"
        />

        <!-- File Uploader -->
        <q-uploader
          ref="uploaderRef"
          url=""
          label="Upload Travel Documents"
          :auto-upload="false"
          @added="onFilesAdded"
          @removed="onFileRemoved"
          :hide-upload-btn="true"
          multiple
          accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
          max-file-size="10485760"
        />
      </q-card-section>

      <q-separator />

      <!-- Documents List -->
      <q-list bordered separator>
        <q-item v-for="doc in documents" :key="doc.id">
          <q-item-section avatar>
            <q-icon :name="getDocumentIcon(doc.mime_type)" color="primary" />
          </q-item-section>

          <q-item-section>
            <q-item-label class="text-weight-medium">{{ doc.name }}</q-item-label>
            <q-item-label caption>
              {{ formatFileSize(doc.file_size) }} • {{ doc.category }} •
              {{ formatDate(doc.uploaded_at) }}
              <span v-if="doc.trip_id" class="text-primary"
                >• Trip: {{ getTripName(doc.trip_id) }}</span
              >
            </q-item-label>
          </q-item-section>

          <q-item-section side>
            <q-btn icon="download" flat dense @click="downloadDocument(doc)" hint="Download" />
            <q-btn
              icon="delete"
              flat
              dense
              color="negative"
              @click="deleteDocument(doc)"
              hint="Delete"
            />
          </q-item-section>
        </q-item>

        <div v-if="documents.length === 0 && !loading" class="text-center q-pa-md text-grey-6">
          <q-icon name="description" size="48px" class="q-mb-sm" />
          <p>No documents uploaded yet.</p>
        </div>

        <div v-if="loading" class="flex flex-center q-pa-md">
          <q-spinner color="primary" size="32px" />
        </div>
      </q-list>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';
import type { Trip } from 'src/types/trip';

const $q = useQuasar();
const uploaderRef = ref();

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
const trips = ref<Trip[]>([]);
const loading = ref(false);
const selectedTripId = ref<string | null>(null);
const selectedCategory = ref('general');

const tripOptions = ref<{ label: string; value: string }[]>([]);
const categoryOptions = [
  { label: 'Passport', value: 'passport' },
  { label: 'Visa', value: 'visa' },
  { label: 'Insurance', value: 'insurance' },
  { label: 'Tickets', value: 'tickets' },
  { label: 'Receipts', value: 'receipts' },
  { label: 'General', value: 'general' },
];

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

async function fetchDocuments() {
  loading.value = true;
  try {
    const {
      data: { user },
    } = await supabase.auth.getUser();
    if (!user) return;

    const { data, error } = await supabase
      .from('documents')
      .select('*')
      .eq('user_id', user.id)
      .order('uploaded_at', { ascending: false });

    if (error) throw error;
    documents.value = (data as DocumentItem[]) || [];
  } catch (error) {
    console.error('Error fetching documents:', error);
    $q.notify({ type: 'negative', message: 'Failed to load documents' });
  } finally {
    loading.value = false;
  }
}

async function onFilesAdded(files: readonly File[]) {
  if (files.length === 0) return;

  const {
    data: { user },
  } = await supabase.auth.getUser();
  if (!user) {
    $q.notify({ type: 'negative', message: 'Please log in to upload documents' });
    return;
  }

  for (const file of files) {
    try {
      // Upload file to Supabase Storage
      const fileExt = file.name.split('.').pop();
      const fileName = `${Date.now()}-${Math.random().toString(36).substring(2)}.${fileExt}`;
      const filePath = `documents/${user.id}/${fileName}`;

      const { error: uploadError } = await supabase.storage
        .from('documents')
        .upload(filePath, file);

      if (uploadError) throw uploadError;

      // Save document metadata to database
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

      $q.notify({
        type: 'positive',
        message: `Uploaded ${file.name}`,
        position: 'top',
      });
    } catch (error) {
      console.error('Upload error:', error);
      $q.notify({
        type: 'negative',
        message: `Failed to upload ${file.name}`,
        position: 'top',
      });
    }
  }

  // Refresh documents list
  await fetchDocuments();

  // Clear uploader
  if (uploaderRef.value) {
    uploaderRef.value.reset();
  }
}

function onFileRemoved() {
  // Handle file removal from uploader if needed
}

async function downloadDocument(doc: DocumentItem) {
  try {
    const { data, error } = await supabase.storage.from('documents').download(doc.file_path);

    if (error) throw error;

    // Create download link
    const url = URL.createObjectURL(data);
    const a = document.createElement('a');
    a.href = url;
    a.download = doc.name;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Download error:', error);
    $q.notify({ type: 'negative', message: 'Failed to download document' });
  }
}

function deleteDocument(doc: DocumentItem) {
  $q.dialog({
    title: 'Delete Document',
    message: `Are you sure you want to delete "${doc.name}"?`,
    cancel: true,
    persistent: true,
  }).onOk(() => {
    void deleteDocumentConfirmed(doc);
  });
}

async function deleteDocumentConfirmed(doc: DocumentItem) {
  try {
    // Delete from storage
    const { error: storageError } = await supabase.storage
      .from('documents')
      .remove([doc.file_path]);

    if (storageError) console.warn('Storage deletion error:', storageError);

    // Delete from database
    const { error: dbError } = await supabase.from('documents').delete().eq('id', doc.id);

    if (dbError) throw dbError;

    $q.notify({ type: 'positive', message: 'Document deleted' });
    await fetchDocuments();
  } catch (error) {
    console.error('Delete error:', error);
    $q.notify({ type: 'negative', message: 'Failed to delete document' });
  }
}

function getDocumentIcon(mimeType: string): string {
  if (mimeType.includes('pdf')) return 'picture_as_pdf';
  if (mimeType.includes('image')) return 'image';
  if (mimeType.includes('word')) return 'description';
  return 'insert_drive_file';
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  });
}

function getTripName(tripId: string): string {
  const trip = trips.value.find((t) => t.id === tripId);
  return trip?.name || 'Unknown Trip';
}

onMounted(async () => {
  await Promise.all([fetchTrips(), fetchDocuments()]);
});
</script>
