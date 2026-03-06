// src/utils/settlementCalculator.ts

export interface Settlement {
  from: string;
  fromName: string;
  to: string;
  toName: string;
  amount: number;
}

export interface MemberBalance {
  memberId: string;
  memberName: string;
  netBalance: number; // Positive = owed money, negative = owes money
  totalPaid: number;
  totalOwed: number;
}

export interface BalanceBreakdown {
  memberBalances: MemberBalance[];
  settlements: Settlement[];
  totalExpenses: number;
  allSettled: boolean;
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
  const netBalances = new Map<string, number>();
  const totalPaidMap = new Map<string, number>();
  const totalOwedMap = new Map<string, number>();

  // Initialize all members
  members.forEach((member) => {
    netBalances.set(member.id, 0);
    totalPaidMap.set(member.id, 0);
    totalOwedMap.set(member.id, 0);
  });

  // Process each expense
  expenses.forEach((expense) => {
    const payerId = expense.paid_by_id;
    const totalPaid = expense.amount;

    // Track what the payer paid
    totalPaidMap.set(payerId, (totalPaidMap.get(payerId) || 0) + totalPaid);

    // The payer gets credited for the full amount (net balance)
    netBalances.set(payerId, (netBalances.get(payerId) || 0) + totalPaid);

    // Each consumer owes their share
    expense.splits.forEach((split) => {
      totalOwedMap.set(
        split.member_id,
        (totalOwedMap.get(split.member_id) || 0) + split.share_amount,
      );
      netBalances.set(
        split.member_id,
        (netBalances.get(split.member_id) || 0) - split.share_amount,
      );
    });
  });

  // Convert to array and add names
  return members.map((member) => ({
    memberId: member.id,
    memberName: member.name,
    netBalance: netBalances.get(member.id) || 0,
    totalPaid: totalPaidMap.get(member.id) || 0,
    totalOwed: totalOwedMap.get(member.id) || 0,
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
 * Calculate complete balance breakdown including settlements
 */
export function calculateBalanceBreakdown(
  expenses: ExpenseWithSplits[],
  members: { id: string; name: string }[],
): BalanceBreakdown {
  const memberBalances = calculateMemberBalances(expenses, members);
  const settlements = simplifySettlements(memberBalances);
  const totalExpenses = expenses.reduce((sum, expense) => sum + expense.amount, 0);
  const allSettled = settlements.length === 0 || settlements.every((s) => s.amount < 0.01);

  return {
    memberBalances,
    settlements,
    totalExpenses,
    allSettled,
  };
}

/**
 * Get settlements involving a specific member
 */
export function getSettlementsForMember(
  settlements: Settlement[],
  memberId: string,
): { owes: Settlement[]; owed: Settlement[] } {
  return {
    owes: settlements.filter((s) => s.from === memberId),
    owed: settlements.filter((s) => s.to === memberId),
  };
}

/**
 * Get balance status text
 */
export function getBalanceStatus(netBalance: number): string {
  if (netBalance > 0.01) return 'You are owed';
  if (netBalance < -0.01) return 'You owe';
  return 'Settled up';
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
export function getBalanceColor(balance: number): string {
  if (balance > 0.01) return 'positive';
  if (balance < -0.01) return 'negative';
  return 'grey-7';
}
