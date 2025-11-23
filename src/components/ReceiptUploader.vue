<template>
  <div class="receipt-uploader">
    <!-- Preview of Existing Receipt -->
    <div v-if="modelValue && !uploading" class="receipt-preview q-mb-md">
      <q-img
        :src="modelValue"
        spinner-color="primary"
        class="rounded-borders"
        style="max-height: 200px; max-width: 100%;"
        fit="contain"
      />
      <div class="row q-mt-sm q-gutter-sm">
        <q-btn
          outline
          color="primary"
          icon="visibility"
          label="View Full Size"
          size="sm"
          @click="viewFullSize"
        />
        <q-btn
          outline
          color="negative"
          icon="delete"
          label="Remove"
          size="sm"
          @click="removeReceipt"
        />
      </div>
    </div>

    <!-- Upload Area -->
    <div v-else class="upload-area">
      <!-- Simple Upload Button -->
      <div class="text-center q-pa-lg">
        <q-icon name="cloud_upload" size="xl" color="grey-5" class="q-mb-md" />
        <div class="text-h6 text-grey-7 q-mb-sm">Upload Receipt</div>
        <div class="text-caption text-grey-6 q-mb-lg">
          Click below to take a photo or choose an image
        </div>

        <!-- Manual Upload Buttons -->
        <div class="row q-gutter-sm justify-center">
          <q-btn
            outline
            color="primary"
            icon="photo_camera"
            label="Take Photo"
            @click="openCamera"
            :loading="uploading"
          />
          <q-btn
            outline
            color="primary"
            icon="photo_library"
            label="Choose File"
            @click="openFilePicker"
            :loading="uploading"
          />
        </div>

        <!-- Progress Indicator -->
        <div v-if="uploading" class="q-mt-md">
          <q-linear-progress indeterminate color="primary" />
          <div class="text-caption text-grey-7 q-mt-sm">Uploading...</div>
        </div>
      </div>
    </div>

    <!-- Full Size Image Dialog -->
    <q-dialog v-model="showFullSize">
      <q-card style="min-width: 80vw;">
        <q-card-section class="q-pa-none">
          <q-img
            :src="modelValue || ''"
            fit="contain"
            style="max-height: 80vh;"
          />
        </q-card-section>
        <q-card-actions align="right">
          <q-btn flat label="Close" color="primary" v-close-popup />
        </q-card-actions>
      </q-card>
    </q-dialog>

    <!-- Hidden File Input for Manual Selection -->
    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      style="display: none;"
      @change="handleFileSelect"
    />

    <!-- Hidden Camera Input -->
    <input
      ref="cameraInputRef"
      type="file"
      accept="image/*"
      capture="environment"
      style="display: none;"
      @change="handleFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useQuasar } from 'quasar';
import { supabase } from 'boot/supabase';

interface Props {
  modelValue?: string | null;
  tripId?: string;
}

interface Emits {
  (e: 'update:modelValue', value: string | null): void;
  (e: 'uploaded', url: string): void;
  (e: 'removed'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const $q = useQuasar();

// Refs
const fileInputRef = ref<HTMLInputElement | null>(null);
const cameraInputRef = ref<HTMLInputElement | null>(null);
const uploading = ref(false);
const showFullSize = ref(false);

// Upload Function
async function uploadFile(file: File): Promise<void> {
  uploading.value = true;

  try {
    // Validate file size (5MB max)
    if (file.size > 5242880) {
      throw new Error('File size must be under 5MB');
    }

    // Validate file type
    if (!file.type.startsWith('image/')) {
      throw new Error('Please select an image file');
    }

    const fileExt = file.name.split('.').pop();
    const fileName = `${props.tripId || 'temp'}/${Date.now()}.${fileExt}`;

    // Upload to Supabase Storage
    const { data, error } = await supabase.storage
      .from('expense-receipts')
      .upload(fileName, file, {
        cacheControl: '3600',
        upsert: false
      });

    if (error) throw error;

    // Get public URL
    const { data: urlData } = supabase.storage
      .from('expense-receipts')
      .getPublicUrl(fileName);

    const publicUrl = urlData.publicUrl;

    // Emit the URL
    emit('update:modelValue', publicUrl);
    emit('uploaded', publicUrl);

    $q.notify({
      type: 'positive',
      message: 'Receipt uploaded successfully!',
      icon: 'check_circle'
    });

  } catch (error: any) {
    console.error('Upload error:', error);
    $q.notify({
      type: 'negative',
      message: 'Failed to upload receipt',
      caption: error.message
    });
  } finally {
    uploading.value = false;
  }
}

// File Selection Handler
async function handleFileSelect(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const files = input.files;

  if (!files || files.length === 0) return;

  const file = files[0];
  if (!file) return;

  await uploadFile(file);

  // Reset input
  input.value = '';
}

// Helper Functions
function openCamera(): void {
  cameraInputRef.value?.click();
}

function openFilePicker(): void {
  fileInputRef.value?.click();
}

function viewFullSize(): void {
  showFullSize.value = true;
}

function removeReceipt(): void {
  $q.dialog({
    title: 'Remove Receipt',
    message: 'Are you sure you want to remove this receipt?',
    cancel: true,
    persistent: true
  }).onOk(() => {
    emit('update:modelValue', null);
    emit('removed');
    $q.notify({
      type: 'info',
      message: 'Receipt removed'
    });
  });
}
</script>

<style scoped>
.receipt-uploader {
  width: 100%;
}

.upload-area {
  border: 2px dashed #ccc;
  border-radius: 8px;
  background-color: #f9f9f9;
}

.receipt-preview {
  text-align: center;
}
</style>
