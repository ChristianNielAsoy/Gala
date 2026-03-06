<template>
  <q-dialog v-model="open" maximized>
    <q-card class="location-picker-card">
      <q-card-section class="row items-center q-pb-none">
        <div class="text-h6">Pick a Location</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>

      <!-- Search -->
      <q-card-section class="q-pt-sm q-pb-xs">
        <q-input
          v-model="query"
          dense
          outlined
          placeholder="Search for a place..."
          clearable
          @keyup.enter="search"
        >
          <template #append>
            <q-btn flat round dense icon="search" :loading="searching" @click="search" />
          </template>
        </q-input>

        <!-- Results dropdown -->
        <q-list v-if="results.length > 0" bordered class="q-mt-xs rounded-borders">
          <q-item
            v-for="r in results"
            :key="r.place_id"
            clickable
            v-ripple
            @click="selectResult(r)"
          >
            <q-item-section avatar>
              <q-icon name="place" color="primary" />
            </q-item-section>
            <q-item-section>
              <q-item-label>{{ r.display_name }}</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>

      <!-- Map -->
      <q-card-section class="q-pt-xs q-pb-xs" style="flex: 1; padding: 0 16px;">
        <div ref="mapContainer" style="height: 380px; border-radius: 8px; overflow: hidden;" />
        <div v-if="picked" class="text-caption text-grey-7 q-mt-xs ellipsis">
          {{ picked.label }}
        </div>
      </q-card-section>

      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat label="Cancel" v-close-popup />
        <q-btn
          color="primary"
          label="Confirm"
          :disable="!picked"
          @click="confirm"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, nextTick } from 'vue';
import type { Map as LeafletMap, Marker } from 'leaflet';

interface NominatimResult {
  place_id: number;
  display_name: string;
  lat: string;
  lon: string;
}

interface PickedLocation {
  label: string;
  lat: number;
  lng: number;
}

const props = defineProps<{
  modelValue: boolean;
  initialLat?: number | undefined;
  initialLng?: number | undefined;
  initialLabel?: string | undefined;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
  (e: 'picked', loc: PickedLocation): void;
}>();

const open = ref(props.modelValue);
watch(() => props.modelValue, (v) => { open.value = v; });
watch(open, (v) => emit('update:modelValue', v));

const query = ref('');
const results = ref<NominatimResult[]>([]);
const searching = ref(false);
const mapContainer = ref<HTMLElement | null>(null);
const picked = ref<PickedLocation | null>(
  props.initialLat && props.initialLng
    ? { label: props.initialLabel ?? '', lat: props.initialLat, lng: props.initialLng }
    : null,
);

let leafletMap: LeafletMap | null = null;
let marker: Marker | null = null;

async function initMap() {
  await nextTick();
  if (!mapContainer.value) return;

  const L = await import('leaflet');
  await import('leaflet/dist/leaflet.css');

  // Fix default icon paths broken by bundlers
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  delete (L.Icon.Default.prototype as any)._getIconUrl;
  L.Icon.Default.mergeOptions({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  });

  const center = picked.value
    ? [picked.value.lat, picked.value.lng] as [number, number]
    : [12.8797, 121.774] as [number, number]; // Philippines center

  leafletMap = L.map(mapContainer.value).setView(center, picked.value ? 14 : 6);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
  }).addTo(leafletMap);

  if (picked.value) {
    marker = L.marker([picked.value.lat, picked.value.lng], { draggable: true }).addTo(leafletMap);
    marker.on('dragend', () => {
      const pos = marker!.getLatLng();
      if (picked.value) {
        picked.value = { ...picked.value, lat: pos.lat, lng: pos.lng };
      }
    });
  }

  leafletMap.on('click', (e) => {
    placePin(e.latlng.lat, e.latlng.lng, query.value || picked.value?.label || '');
  });
}

function placePin(lat: number, lng: number, label: string) {
  if (!leafletMap) return;
  void import('leaflet').then((L) => {
    if (marker) {
      marker.setLatLng([lat, lng]);
    } else {
      marker = L.marker([lat, lng], { draggable: true }).addTo(leafletMap!);
      marker.on('dragend', () => {
        const pos = marker!.getLatLng();
        if (picked.value) {
          picked.value = { ...picked.value, lat: pos.lat, lng: pos.lng };
        }
      });
    }
    leafletMap!.setView([lat, lng], 14);
    picked.value = { label, lat, lng };
  });
}

async function search() {
  if (!query.value.trim()) return;
  searching.value = true;
  results.value = [];
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query.value)}&format=json&limit=5`,
      { headers: { 'Accept-Language': 'en' } },
    );
    results.value = (await res.json()) as NominatimResult[];
  } catch {
    results.value = [];
  } finally {
    searching.value = false;
  }
}

function selectResult(r: NominatimResult) {
  results.value = [];
  query.value = r.display_name;
  placePin(parseFloat(r.lat), parseFloat(r.lon), r.display_name);
}

function confirm() {
  if (!picked.value) return;
  emit('picked', picked.value);
  open.value = false;
}

// Init map when dialog opens
watch(open, (v) => {
  if (v) void initMap();
});

onMounted(() => {
  if (open.value) void initMap();
});
</script>

<style scoped>
.location-picker-card {
  display: flex;
  flex-direction: column;
  max-height: 100dvh;
}
</style>
