<template>
  <q-dialog v-model="open">
    <q-card style="min-width: 320px; max-width: 95vw">
      <q-card-section class="row items-center q-pb-sm">
        <q-icon name="currency_exchange" color="primary" size="md" class="q-mr-sm" />
        <span class="text-h6">Currency Converter</span>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>

      <q-card-section class="q-pt-none q-gutter-y-md">
        <!-- Amount input -->
        <q-input
          v-model.number="amount"
          type="number"
          label="Amount"
          outlined
          dense
          min="0"
        />

        <!-- From / To row -->
        <div class="row items-center q-gutter-sm">
          <q-select
            v-model="fromCurrency"
            :options="currencyOptions"
            label="From"
            outlined
            dense
            class="col"
            emit-value
            map-options
            @update:model-value="calculate"
          />
          <q-btn
            icon="swap_horiz"
            flat
            round
            dense
            color="primary"
            @click="swapCurrencies"
          />
          <q-select
            v-model="toCurrency"
            :options="currencyOptions"
            label="To"
            outlined
            dense
            class="col"
            emit-value
            map-options
            @update:model-value="calculate"
          />
        </div>

        <!-- Result -->
        <div class="result-box q-pa-md rounded-borders text-center">
          <div v-if="loading" class="text-grey-6">
            <q-spinner size="sm" class="q-mr-sm" />Fetching rates...
          </div>
          <div v-else-if="error" class="text-negative text-caption">{{ error }}</div>
          <template v-else-if="result !== null">
            <div class="text-caption text-grey-6">
              {{ amount }} {{ fromCurrency }} =
            </div>
            <div class="text-h5 text-weight-bold text-primary">
              {{ result.toFixed(2) }} {{ toCurrency }}
            </div>
            <div class="text-caption text-grey-5 q-mt-xs">
              1 {{ fromCurrency }} = {{ rate.toFixed(4) }} {{ toCurrency }}
              <span class="q-ml-xs">· ECB rates</span>
            </div>
          </template>
          <div v-else class="text-grey-5 text-caption">Enter an amount to convert</div>
        </div>
      </q-card-section>

      <q-card-actions align="right">
        <q-btn flat label="Close" v-close-popup />
        <q-btn
          flat
          color="primary"
          label="Convert"
          :loading="loading"
          @click="calculate"
        />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { convert, getRate, COMMON_CURRENCIES } from 'src/utils/currencyService';

const props = defineProps<{
  modelValue: boolean;
  defaultFrom?: string | undefined;
  defaultTo?: string | undefined;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void;
}>();

const open = ref(props.modelValue);
watch(() => props.modelValue, (v) => { open.value = v; });
watch(open, (v) => emit('update:modelValue', v));

const currencyOptions = COMMON_CURRENCIES.map((c) => ({ label: c, value: c }));

const amount = ref<number>(1);
const fromCurrency = ref(props.defaultFrom ?? 'USD');
const toCurrency = ref(props.defaultTo ?? 'PHP');
const result = ref<number | null>(null);
const rate = ref(0);
const loading = ref(false);
const error = ref('');

async function calculate() {
  if (!amount.value || amount.value <= 0) return;
  loading.value = true;
  error.value = '';
  try {
    rate.value = await getRate(fromCurrency.value, toCurrency.value);
    result.value = await convert(amount.value, fromCurrency.value, toCurrency.value);
  } catch {
    error.value = 'Could not fetch rates. Check your connection.';
  } finally {
    loading.value = false;
  }
}

function swapCurrencies() {
  [fromCurrency.value, toCurrency.value] = [toCurrency.value, fromCurrency.value];
  void calculate();
}

// Auto-calculate when dialog opens
watch(open, (v) => { if (v) void calculate(); });
</script>

<style scoped>
.result-box {
  background: var(--q-primary-opacity, rgba(13, 148, 136, 0.06));
  border: 1px solid rgba(13, 148, 136, 0.2);
  min-height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>
