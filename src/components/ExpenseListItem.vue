<template>
  <q-item clickable v-ripple @click="$emit('click', expense)">
    <q-item-section avatar>
      <q-avatar :icon="categoryIcon" :color="categoryColor" text-color="white" />
    </q-item-section>

    <q-item-section>
      <q-item-label class="text-weight-bold">{{ expense.description }}</q-item-label>
      <q-item-label caption>
        Paid by {{ expense.paid_by_name || 'Unknown' }} • {{ formattedDate }}
      </q-item-label>
    </q-item-section>

    <q-item-section side>
      <q-item-label
        class="text-weight-bold text-h6"
        :class="amountColorClass"
      >
        {{ expense.currency_code }} {{ expense.amount.toFixed(2) }}
      </q-item-label>
      <q-item-label caption class="text-right">
        {{ yourShareText }}
      </q-item-label>
    </q-item-section>
  </q-item>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Expense } from 'src/types/expense';

const props = defineProps<{
  expense: Expense;
  currentMemberId?: string;
}>();

const categoryIcon = computed(() => {
  const icons: Record<string, string> = {
    'Food & Drinks': 'restaurant',
    'Transport': 'commute',
    'Lodging': 'hotel',
    'Activity': 'attractions',
    'Groceries': 'shopping_cart'
  };
  return icons[props.expense.category] || 'receipt';
});

const categoryColor = computed(() => {
  const colors: Record<string, string> = {
    'Food & Drinks': 'orange',
    'Transport': 'blue',
    'Lodging': 'purple',
    'Activity': 'green',
    'Groceries': 'teal'
  };
  return colors[props.expense.category] || 'grey';
});

const isPaidByMe = computed(() => {
  if (!props.currentMemberId) return false;
  return props.expense.paid_by_id === props.currentMemberId;
});

const amountColorClass = computed(() =>
  isPaidByMe.value ? 'text-positive' : 'text-negative'
);

const yourShareText = computed(() =>
  isPaidByMe.value ? 'You paid' : 'You owe settlement'
);

const formattedDate = computed(() =>
  new Date(props.expense.date).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric'
  })
);

defineEmits<{
  click: [expense: Expense]
}>();
</script>
