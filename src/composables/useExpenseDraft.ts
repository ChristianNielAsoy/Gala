import { ref, watch } from 'vue';
import type { Ref } from 'vue';
import { supabase } from 'boot/supabase';
import type { ExpenseForm, ExpenseItem, SplitMode } from './useExpenseSplitting';

interface DraftFormState {
  expenseForm: Ref<ExpenseForm>;
  splitMode: Ref<SplitMode>;
  items: Ref<ExpenseItem[]>;
  involvedMembers: Ref<string[]>;
  customSplits: Ref<Record<string, number>>;
}

export function useExpenseDraft(
  tripId: Ref<string>,
  expenseId: Ref<string | undefined>,
  userId: Ref<string | undefined>,
  formState: DraftFormState,
) {
  const hasDraft = ref(false);
  const draftKey = `expense_draft_${tripId.value}_${expenseId.value || 'new'}`;

  async function loadDraft(): Promise<void> {
    const uid = userId.value;
    if (!uid) return;
    try {
      const { data, error } = await supabase
        .from('expense_drafts')
        .select('expense_data')
        .eq('trip_id', tripId.value)
        .eq('user_id', uid)
        .eq('draft_key', draftKey)
        .single();

      if (error && error.code !== 'PGRST116') {
        console.warn('Failed to load draft:', error);
        return;
      }

      if (data?.expense_data) {
        const d = data.expense_data as Record<string, unknown>;
        if (d.expenseForm) Object.assign(formState.expenseForm.value, d.expenseForm);
        if (d.splitMode) formState.splitMode.value = d.splitMode as SplitMode;
        if (d.items) formState.items.value = d.items as ExpenseItem[];
        if (d.involvedMembers) formState.involvedMembers.value = d.involvedMembers as string[];
        if (d.customSplits)
          formState.customSplits.value = d.customSplits as Record<string, number>;
      }
    } catch (error) {
      console.warn('Failed to load draft:', error);
    }
  }

  async function saveDraft(): Promise<void> {
    const uid = userId.value;
    if (!uid) return;
    try {
      const { error } = await supabase.from('expense_drafts').upsert({
        trip_id: tripId.value,
        user_id: uid,
        draft_key: draftKey,
        expense_data: {
          expenseForm: formState.expenseForm.value,
          splitMode: formState.splitMode.value,
          items: formState.items.value,
          involvedMembers: formState.involvedMembers.value,
          customSplits: formState.customSplits.value,
          timestamp: Date.now(),
        },
        updated_at: new Date().toISOString(),
      });
      if (error) {
        console.warn('Failed to save draft:', error);
      } else {
        hasDraft.value = true;
      }
    } catch (error) {
      console.warn('Failed to save draft:', error);
    }
  }

  async function clearDraft(): Promise<void> {
    const uid = userId.value;
    if (!uid) return;
    try {
      const { error } = await supabase
        .from('expense_drafts')
        .delete()
        .eq('trip_id', tripId.value)
        .eq('user_id', uid)
        .eq('draft_key', draftKey);
      if (error) {
        console.warn('Failed to clear draft:', error);
      } else {
        hasDraft.value = false;
      }
    } catch (error) {
      console.warn('Failed to clear draft:', error);
    }
  }

  async function checkDraftExists(): Promise<void> {
    const uid = userId.value;
    if (!uid) return;
    try {
      const { data, error } = await supabase
        .from('expense_drafts')
        .select('id')
        .eq('trip_id', tripId.value)
        .eq('user_id', uid)
        .eq('draft_key', draftKey)
        .single();
      hasDraft.value = !error && !!data;
    } catch {
      hasDraft.value = false;
    }
  }

  // Auto-save on any form state change
  watch(
    [
      formState.expenseForm,
      formState.splitMode,
      formState.items,
      formState.involvedMembers,
      formState.customSplits,
    ],
    () => void saveDraft(),
    { deep: true },
  );

  return { hasDraft, loadDraft, saveDraft, clearDraft, checkDraftExists };
}
