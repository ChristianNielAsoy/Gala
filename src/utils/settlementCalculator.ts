// src/utils/settlementCalculator.ts

export interface MemberBalance {
  memberId: string;
  memberName: string;
  netBalance: number; // ✅ Changed from 'balance' to 'netBalance'
  totalPaid: number;
  totalOwed: number;
}

export interface Settlement {
  from: string;
  fromName: string;
  to: string;
  toName: string;
  amount: number;
}

export interface ExpenseWithSplits {
  id: string;
  paid_by_id: string;
  amount: number;
  splits: {
    member_id: string;
    share_amount: number;
  }[];
}

/**
 * Calculate net balances for all members
 * Returns positive balance if member is owed money, negative if they owe
 */
export function calculateMemberBalances(
  expenses: ExpenseWithSplits[],
  members: { id: string; name: string }[],
): MemberBalance[] {
  const balances = new Map<string, number>();

  // Initialize all members with 0 balance
  members.forEach((member) => {
    balances.set(member.id, 0);
  });

  // Process each expense
  expenses.forEach((expense) => {
    const payerId = expense.paid_by_id;
    const totalPaid = expense.amount;

    // The payer gets credited for the full amount
    balances.set(payerId, (balances.get(payerId) || 0) + totalPaid);

    // Each consumer owes their share
    expense.splits.forEach((split) => {
      balances.set(split.member_id, (balances.get(split.member_id) || 0) - split.share_amount);
    });
  });

  // Convert to array and add names
  return members.map((member) => ({
    memberId: member.id,
    memberName: member.name,
    balance: balances.get(member.id) || 0,
  }));
}

/**
 * Simplify debts to minimize number of transactions
 * Uses greedy algorithm to match largest debtor with largest creditor
 */
export function simplifySettlements(balances: MemberBalance[]): Settlement[] {
  const settlements: Settlement[] = [];

  // Create mutable working arrays
  const creditors = balances
    .filter((b) => b.netBalance > 0.01)
    .map((b) => ({
      memberId: b.memberId,
      memberName: b.memberName,
      balance: b.netBalance,
    }))
    .sort((a, b) => b.balance - a.balance);

  const debtors = balances
    .filter((b) => b.netBalance < -0.01)
    .map((b) => ({
      memberId: b.memberId,
      memberName: b.memberName,
      balance: Math.abs(b.netBalance),
    }))
    .sort((a, b) => b.balance - a.balance);

  let creditorIndex = 0;
  let debtorIndex = 0;

  while (creditorIndex < creditors.length && debtorIndex < debtors.length) {
    // Get current creditor and debtor with null checks
    const currentCreditor = creditors[creditorIndex];
    const currentDebtor = debtors[debtorIndex];

    if (!currentCreditor || !currentDebtor) break;

    // Calculate settlement amount
    const settleAmount = Math.min(currentCreditor.balance, currentDebtor.balance);

    if (settleAmount > 0.01) {
      settlements.push({
        from: currentDebtor.memberId,
        fromName: currentDebtor.memberName,
        to: currentCreditor.memberId,
        toName: currentCreditor.memberName,
        amount: Math.round(settleAmount * 100) / 100,
      });

      // Update balances
      currentCreditor.balance -= settleAmount;
      currentDebtor.balance -= settleAmount;
    }

    // Move to next creditor or debtor if balance is settled
    if (currentCreditor.balance < 0.01) creditorIndex++;
    if (currentDebtor.balance < 0.01) debtorIndex++;
  }

  return settlements;
}

/**
 * Get settlement summary for a specific member
 */
export function getMemberSettlementSummary(
  memberId: string,
  settlements: Settlement[],
): {
  toReceive: Settlement[];
  toPay: Settlement[];
  netBalance: number;
} {
  const toReceive = settlements.filter((s) => s.to === memberId);
  const toPay = settlements.filter((s) => s.from === memberId);

  const netBalance =
    toReceive.reduce((sum, s) => sum + s.amount, 0) - toPay.reduce((sum, s) => sum + s.amount, 0);

  return {
    toReceive,
    toPay,
    netBalance,
  };
}

/**
 * Format currency amount for display
 */
export function formatCurrency(amount: number, currencyCode = 'PHP'): string {
  return `${currencyCode} ${amount.toFixed(2)}`;
}

/**
 * Get color class based on balance
 */
export function getBalanceColorClass(balance: number): string {
  if (balance > 0.01) return 'text-positive';
  if (balance < -0.01) return 'text-negative';
  return 'text-grey-7';
}
