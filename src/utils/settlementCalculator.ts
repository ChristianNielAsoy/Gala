// src/utils/settlementCalculator.ts
// Core logic for calculating who owes whom in a trip

import type {
  Expense,
  ExpenseSplit,
  TripMember,
  MemberBalance,
  SettlementSuggestion,
} from 'src/types/expense';

/**
 * Calculate the net balance for each member in a trip
 * @param expenses - All expenses in the trip
 * @param splits - All expense splits
 * @param members - All trip members
 * @returns Array of member balances (positive = owed, negative = owes)
 */
export function calculateMemberBalances(
  expenses: Expense[],
  splits: ExpenseSplit[],
  members: TripMember[]
): MemberBalance[] {
  const balances = new Map<string, MemberBalance>();

  // Initialize balances for all members
  members.forEach((member) => {
    balances.set(member.id, {
      member_id: member.id,
      member_name: member.name,
      member_avatar: member.avatar_url ?? '',
      total_paid: 0,
      total_owed: 0,
      net_balance: 0,
    });
  });

  // Calculate total paid by each member
  expenses.forEach((expense) => {
    const balance = balances.get(expense.paid_by_id);
    if (balance) {
      balance.total_paid += expense.amount;
    }
  });

  // Calculate total owed by each member (their share of expenses)
  splits.forEach((split) => {
    const balance = balances.get(split.member_id);
    if (balance) {
      balance.total_owed += split.share_amount;
    }
  });

  // Calculate net balance (positive = should receive money, negative = should pay money)
  balances.forEach((balance) => {
    balance.net_balance = balance.total_paid - balance.total_owed;
  });

  return Array.from(balances.values());
}

/**
 * Generate optimal settlement suggestions to minimize transactions
 * Uses a greedy algorithm to settle debts efficiently
 * @param balances - Member balances calculated from expenses
 * @param currencyCode - The trip's currency code
 * @returns Array of suggested settlements
 */
export function generateSettlementSuggestions(
  balances: MemberBalance[],
  currencyCode: string
): SettlementSuggestion[] {
  const suggestions: SettlementSuggestion[] = [];

  interface MutableBalance extends MemberBalance {
    net_balance: number;
  }

  const creditors: MutableBalance[] = balances
    .filter((b) => b.net_balance > 0.01)
    .map((b) => ({ ...b }))
    .sort((a, b) => b.net_balance - a.net_balance);

  const debtors: MutableBalance[] = balances
    .filter((b) => b.net_balance < -0.01)
    .map((b) => ({ ...b }))
    .sort((a, b) => a.net_balance - b.net_balance);

  let creditorIdx = 0;
  let debtorIdx = 0;

  while (creditorIdx < creditors.length && debtorIdx < debtors.length) {
    const creditor = creditors[creditorIdx];
    const debtor = debtors[debtorIdx];

    // ✅ Null check to satisfy TypeScript
    if (!creditor || !debtor) {
      break;
    }

    const amountToSettle = Math.min(
      creditor.net_balance,
      Math.abs(debtor.net_balance)
    );

    if (amountToSettle > 0.01) {
      suggestions.push({
        from_member_id: debtor.member_id,
        to_member_id: creditor.member_id,
        amount: parseFloat(amountToSettle.toFixed(2)),
        currency_code: currencyCode,
      });

      creditor.net_balance -= amountToSettle;
      debtor.net_balance += amountToSettle;
    }

    if (Math.abs(creditor.net_balance) < 0.01) creditorIdx++;
    if (Math.abs(debtor.net_balance) < 0.01) debtorIdx++;
  }

  return suggestions;
}

/**
 * Calculate the total expense amount for a trip
 */
export function calculateTripTotal(expenses: Expense[]): number {
  return expenses.reduce((sum, expense) => sum + expense.amount, 0);
}

/**
 * Calculate how much a specific member owes or is owed
 */
export function getMemberBalance(
  memberId: string,
  balances: MemberBalance[]
): number {
  const balance = balances.find((b) => b.member_id === memberId);
  return balance ? balance.net_balance : 0;
}

/**
 * Format currency amount for display
 */
export function formatCurrency(amount: number, currencyCode: string): string {
  return new Intl.NumberFormat('en-PH', {
    style: 'currency',
    currency: currencyCode,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
}

/**
 * Get settlement summary text for a member
 */
export function getSettlementSummary(
  memberId: string,
  balances: MemberBalance[],
  suggestions: SettlementSuggestion[],
  allMembers: TripMember[]
): {
  status: 'settled' | 'owes' | 'owed';
  message: string;
  details: Array<{ to: string; amount: number }>;
} {
  const balance = balances.find((b) => b.member_id === memberId);
  if (!balance) {
    return { status: 'settled', message: 'All settled!', details: [] };
  }

  if (Math.abs(balance.net_balance) < 0.01) {
    return { status: 'settled', message: 'All settled!', details: [] };
  }

  const relevantSuggestions = suggestions.filter(
    (s) => s.from_member_id === memberId || s.to_member_id === memberId
  );

  const details = relevantSuggestions.map((suggestion) => {
    const isDebtor = suggestion.from_member_id === memberId;
    const otherMemberId = isDebtor
      ? suggestion.to_member_id
      : suggestion.from_member_id;
    const otherMember = allMembers.find((m) => m.id === otherMemberId);

    return {
      to: otherMember?.name || 'Unknown',
      amount: suggestion.amount,
    };
  });

  if (balance.net_balance < 0) {
    const totalOwed = Math.abs(balance.net_balance);
    return {
      status: 'owes',
      message: `You owe ${formatCurrency(totalOwed, 'PHP')}`,
      details,
    };
  } else {
    return {
      status: 'owed',
      message: `You are owed ${formatCurrency(balance.net_balance, 'PHP')}`,
      details,
    };
  }
}

/**
 * Validate if settlement amounts match expected values
 */
export function validateSettlement(
  settlement: SettlementSuggestion,
  balances: MemberBalance[]
): { valid: boolean; error?: string } {
  const fromBalance = balances.find(
    (b) => b.member_id === settlement.from_member_id
  );
  const toBalance = balances.find(
    (b) => b.member_id === settlement.to_member_id
  );

  if (!fromBalance || !toBalance) {
    return { valid: false, error: 'Invalid member IDs' };
  }

  if (fromBalance.net_balance >= 0) {
    return {
      valid: false,
      error: 'From member does not owe money',
    };
  }

  if (toBalance.net_balance <= 0) {
    return {
      valid: false,
      error: 'To member is not owed money',
    };
  }

  if (settlement.amount > Math.abs(fromBalance.net_balance) + 0.01) {
    return {
      valid: false,
      error: 'Settlement amount exceeds debt',
    };
  }

  if (settlement.amount > toBalance.net_balance + 0.01) {
    return {
      valid: false,
      error: 'Settlement amount exceeds credit',
    };
  }

  return { valid: true };
}
