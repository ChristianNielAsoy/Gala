import { ref, computed, watch } from 'vue';
import type { Ref } from 'vue';
import type { TripMember, SplitType } from 'src/types/expense';

export interface ExpenseForm {
  description: string;
  amount: number | null;
  paid_by_id: string;
  category: string;
  date: string;
  split_type: SplitType;
}

export interface ExpenseItem {
  name: string;
  amount: number;
  isLibre: boolean;
  participants: string[];
  itemType?: 'individual' | 'shared';
}

export type SplitMode =
  | 'equal'
  | 'custom'
  | 'itemized'
  | 'gifted'
  | 'equalized_meals'
  | 'individual_shared';

export const splitModeOptions = [
  { label: 'Equal Split', value: 'equal' },
  { label: 'Custom Amounts', value: 'custom' },
  { label: 'Item by Item', value: 'itemized' },
  { label: 'Gifted / Libre', value: 'gifted' },
  { label: 'Equal Meals', value: 'equalized_meals' },
  { label: 'Personal + Shared', value: 'individual_shared' },
];

export function useExpenseSplitting(
  members: Ref<TripMember[]>,
  expenseForm: Ref<ExpenseForm>,
) {
  const splitMode = ref<SplitMode>(expenseForm.value.split_type as SplitMode);
  const involvedMembers = ref<string[]>([]);
  const customSplits = ref<Record<string, number>>({});
  const items = ref<ExpenseItem[]>([]);

  const memberCheckOptions = computed(() =>
    members.value.map((m: TripMember) => ({ label: m.name, value: m.id })),
  );

  const itemsTotal = computed(() =>
    items.value.reduce((sum, item) => sum + (item.amount || 0), 0),
  );

  const itemsTotalMismatch = computed(
    () =>
      splitMode.value === 'itemized' &&
      expenseForm.value.amount !== null &&
      Math.abs(itemsTotal.value - expenseForm.value.amount) > 0.01,
  );

  const customTotal = computed(() =>
    involvedMembers.value.reduce(
      (sum: number, memberId: string) => sum + (customSplits.value[memberId] || 0),
      0,
    ),
  );

  const splitDifference = computed(() => (expenseForm.value.amount || 0) - customTotal.value);

  const isValid = computed(() => {
    const basicValid =
      !!expenseForm.value.description &&
      (expenseForm.value.amount || 0) > 0 &&
      !!expenseForm.value.paid_by_id;

    if (!basicValid) return false;

    switch (splitMode.value) {
      case 'equal':
        return involvedMembers.value.length > 0;
      case 'custom':
        return involvedMembers.value.length > 0 && Math.abs(splitDifference.value) < 0.01;
      case 'itemized':
        return (
          items.value.length > 0 &&
          !itemsTotalMismatch.value &&
          items.value.every(
            (item) =>
              item.name && item.amount > 0 && (item.isLibre || item.participants.length > 0),
          )
        );
      case 'gifted':
      case 'equalized_meals':
        return involvedMembers.value.length > 0;
      case 'individual_shared':
        return (
          items.value.length > 0 &&
          !itemsTotalMismatch.value &&
          items.value.every((item) => item.name && item.amount > 0 && item.participants.length > 0)
        );
      default:
        return false;
    }
  });

  // Sync splitMode → expenseForm.split_type
  watch(splitMode, (newMode) => {
    expenseForm.value.split_type = newMode;
    if (newMode === 'itemized' && items.value.length === 0) addItem();
    if (['equal', 'custom', 'gifted', 'equalized_meals'].includes(newMode)) {
      involvedMembers.value = members.value.map((m: TripMember) => m.id);
    }
  });

  // Sync expenseForm.split_type → splitMode
  watch(
    () => expenseForm.value.split_type,
    (newType) => {
      splitMode.value = newType as SplitMode;
    },
  );

  function addItem(): void {
    items.value.push({
      name: '',
      amount: 0,
      isLibre: false,
      participants: [...involvedMembers.value],
      itemType: 'shared',
    });
  }

  function addIndividualSharedItem(): void {
    items.value.push({
      name: '',
      amount: 0,
      isLibre: false,
      participants: [...involvedMembers.value],
      itemType: 'shared',
    });
  }

  function removeItem(index: number): void {
    items.value.splice(index, 1);
  }

  function toggleItemParticipant(itemIndex: number, memberId: string): void {
    const item = items.value[itemIndex];
    if (!item) return;
    const idx = item.participants.indexOf(memberId);
    if (idx > -1) {
      item.participants.splice(idx, 1);
    } else {
      item.participants.push(memberId);
    }
  }

  return {
    splitMode,
    involvedMembers,
    customSplits,
    items,
    memberCheckOptions,
    itemsTotal,
    itemsTotalMismatch,
    customTotal,
    splitDifference,
    isValid,
    addItem,
    addIndividualSharedItem,
    removeItem,
    toggleItemParticipant,
  };
}
