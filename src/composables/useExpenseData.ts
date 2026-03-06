import { ref, computed } from 'vue';
import type { Ref } from 'vue';
import { supabase } from 'boot/supabase';
import { useQuasar } from 'quasar';
import type { Trip, TripMember } from 'src/types/expense';
import type { ExpenseForm, ExpenseItem, SplitMode } from './useExpenseSplitting';

export interface ExpenseLoadResult {
  form: ExpenseForm;
  splitMode: SplitMode;
  receiptUrl: string | null;
  items: ExpenseItem[];
  involvedMemberIds: string[];
  customSplitAmounts: Record<string, number>;
}

export function useExpenseData(tripId: Ref<string>) {
  const $q = useQuasar();
  const trip = ref<Trip | null>(null);
  const members = ref<TripMember[]>([]);
  const loading = ref(true);
  const categoryOptions = ref<{ label: string; value: string; icon?: string }[]>([]);
  const currencyOptions = ref<{ label: string; value: string }[]>([]);

  const memberOptions = computed(() =>
    members.value.map((m: TripMember) => ({ label: m.name, value: m.id })),
  );

  async function fetchCategories(): Promise<void> {
    const { data, error } = await supabase
      .from('expense_categories')
      .select('name, icon')
      .eq('is_active', true)
      .order('display_order');

    if (error) {
      categoryOptions.value = [
        { label: 'Food & Drinks', value: 'Food & Drinks', icon: 'restaurant' },
        { label: 'Accommodation', value: 'Accommodation', icon: 'hotel' },
        { label: 'Transportation', value: 'Transportation', icon: 'commute' },
        { label: 'Activities', value: 'Activities', icon: 'attractions' },
        { label: 'Groceries', value: 'Groceries', icon: 'local_grocery_store' },
        { label: 'Other', value: 'Other', icon: 'more_horiz' },
      ];
    } else {
      categoryOptions.value = data.map((cat) => ({
        label: cat.name,
        value: cat.name,
        icon: cat.icon || 'more_horiz',
      }));
    }
  }

  async function fetchCurrencies(): Promise<void> {
    const { data, error } = await supabase
      .from('currencies')
      .select('code, name, symbol')
      .eq('is_active', true)
      .order('display_order');

    if (error) {
      currencyOptions.value = [
        { label: 'PHP - Philippine Peso', value: 'PHP' },
        { label: 'USD - US Dollar', value: 'USD' },
        { label: 'EUR - Euro', value: 'EUR' },
      ];
    } else {
      currencyOptions.value = data.map((curr) => ({
        label: `${curr.code} - ${curr.name}`,
        value: curr.code,
      }));
    }
  }

  // Fetches trip context (trip + members + lookup data). Returns false if trip load fails.
  async function fetchTripData(): Promise<boolean> {
    loading.value = true;
    try {
      await Promise.all([fetchCategories(), fetchCurrencies()]);

      const { data: tripData, error: tripError } = await supabase
        .from('trips')
        .select('*')
        .eq('id', tripId.value)
        .single();

      if (tripError || !tripData) {
        $q.notify({ type: 'negative', message: 'Could not load trip context.' });
        return false;
      }
      trip.value = tripData as Trip;

      const { data: memberData, error: memberError } = await supabase
        .from('members')
        .select('*')
        .eq('trip_id', tripId.value)
        .order('name');

      if (memberError || !memberData) {
        $q.notify({ type: 'warning', message: 'Could not load trip members.' });
      } else {
        members.value = memberData as TripMember[];
      }

      return true;
    } finally {
      loading.value = false;
    }
  }

  // Loads existing expense data for edit mode. Returns structured result, caller populates form state.
  async function fetchExpenseData(expenseId: string): Promise<ExpenseLoadResult | null> {
    const { data: expenseData, error: expenseError } = await supabase
      .from('expenses')
      .select('*')
      .eq('id', expenseId)
      .single();

    if (expenseError || !expenseData) {
      $q.notify({ type: 'negative', message: 'Could not load expense data.' });
      return null;
    }

    const form: ExpenseForm = {
      description: expenseData.description,
      amount: expenseData.amount,
      paid_by_id: expenseData.paid_by_id,
      category: expenseData.category,
      date: expenseData.date,
      split_type: expenseData.split_type || 'equal',
    };

    let loadedItems: ExpenseItem[] = [];
    if (['itemized', 'individual_shared', 'equalized_meals'].includes(expenseData.split_type)) {
      const { data: itemsData, error: itemsError } = await supabase
        .from('expense_items')
        .select('*')
        .eq('expense_id', expenseId)
        .order('display_order');

      if (!itemsError && itemsData) {
        loadedItems = itemsData.map((item) => ({
          name: item.item_name,
          amount: item.item_amount,
          isLibre: item.is_libre || false,
          participants: item.consumers || [],
          itemType: item.consumers?.length === 1 ? ('individual' as const) : ('shared' as const),
        }));
      }
    }

    let involvedMemberIds: string[] = [];
    const customSplitAmounts: Record<string, number> = {};

    const { data: splitsData, error: splitsError } = await supabase
      .from('expense_splits')
      .select('member_id, share_amount')
      .eq('expense_id', expenseId);

    if (!splitsError && splitsData) {
      involvedMemberIds = splitsData.map((split) => split.member_id);
      if (expenseData.split_type === 'custom') {
        splitsData.forEach((split) => {
          customSplitAmounts[split.member_id] = split.share_amount;
        });
      }
    }

    return {
      form,
      splitMode: expenseData.split_type as SplitMode,
      receiptUrl: expenseData.receipt_url || null,
      items: loadedItems,
      involvedMemberIds,
      customSplitAmounts,
    };
  }

  return {
    trip,
    members,
    loading,
    categoryOptions,
    currencyOptions,
    memberOptions,
    fetchTripData,
    fetchExpenseData,
  };
}
