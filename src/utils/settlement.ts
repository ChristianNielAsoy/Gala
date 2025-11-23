// Settlement calculation utilities and types
// Place this in: src/utils/settlement.ts

import type { Expense, TripMember, ExpenseSplit } from 'src/types/expense';

export interface MemberBalance {
  memberId: string;
  memberName: string;
  totalPaid: number;      // What they paid for
  totalOwed: number;      // What they owe (their share)
  netBalance: number;     // Positive = they're owed, Negative = they owe
}

export interface Settlement {
  from: string;           // Member ID who owes
  to: string;             // Member ID who is owed
  amount: number;
  fromName: string;
  toName: string;
}

export interface BalanceBreakdown {
  memberBalances: MemberBalance[];
  settlements: Settlement[];
  totalExpenses: number;
  allSettled: boolean;
}

/**
 * Calculate member balances from expenses and splits
 */
export function calculateBalances(
  expenses: Expense[],
  splits: ExpenseSplit[],
  members: TripMember[]
): BalanceBreakdown {

  const memberMap = new Map(members.map(m => [m.id, m.name]));
  const balances = new Map<string, MemberBalance>();

  // Initialize all members
  members.forEach(member => {
    balances.set(member.id, {
      memberId: member.id,
      memberName: member.name,
      totalPaid: 0,
      totalOwed: 0,
      netBalance: 0
    });
  });

  // Calculate what each member paid
  expenses.forEach(expense => {
    const balance = balances.get(expense.paid_by_id);
    if (balance) {
      balance.totalPaid += expense.amount;
    }
  });

  // Calculate what each member owes (their share)
  splits.forEach(split => {
    const balance = balances.get(split.member_id);
    if (balance) {
      balance.totalOwed += split.share_amount;
    }
  });

  // Calculate net balances
  balances.forEach(balance => {
    balance.netBalance = balance.totalPaid - balance.totalOwed;
  });

  const memberBalances = Array.from(balances.values());
  const totalExpenses = expenses.reduce((sum, e) => sum + e.amount, 0);

  // Calculate optimal settlements
  const settlements = calculateOptimalSettlements(memberBalances, memberMap);

  const allSettled = settlements.length === 0 ||
    settlements.every(s => s.amount < 0.01);

  return {
    memberBalances,
    settlements,
    totalExpenses,
    allSettled
  };
}

/**
 * Calculate optimal settlements using greedy algorithm
 * Minimizes number of transactions needed
 */
function calculateOptimalSettlements(
  balances: MemberBalance[],
  memberMap: Map<string, string>
): Settlement[] {
  const settlements: Settlement[] = [];

  // Separate creditors (positive balance) and debtors (negative balance)
  const creditors = balances
    .filter(b => b.netBalance > 0.01)
    .map(b => ({ id: b.memberId, amount: b.netBalance }))
    .sort((a, b) => b.amount - a.amount);

  const debtors = balances
    .filter(b => b.netBalance < -0.01)
    .map(b => ({ id: b.memberId, amount: -b.netBalance }))
    .sort((a, b) => b.amount - a.amount);

  let i = 0, j = 0;

  while (i < creditors.length && j < debtors.length) {
    const creditor = creditors[i];
    const debtor = debtors[j];

    if (!creditor || !debtor) break;

    const settlementAmount = Math.min(creditor.amount, debtor.amount);

    if (settlementAmount > 0.01) {
      settlements.push({
        from: debtor.id,
        to: creditor.id,
        amount: parseFloat(settlementAmount.toFixed(2)),
        fromName: memberMap.get(debtor.id) || 'Unknown',
        toName: memberMap.get(creditor.id) || 'Unknown'
      });
    }

    creditor.amount -= settlementAmount;
    debtor.amount -= settlementAmount;

    if (creditor.amount < 0.01) i++;
    if (debtor.amount < 0.01) j++;
  }

  return settlements;
}

/**
 * Get settlements involving a specific member
 */
export function getSettlementsForMember(
  settlements: Settlement[],
  memberId: string
): { owes: Settlement[], owed: Settlement[] } {

  return {
    owes: settlements.filter(s => s.from === memberId),
    owed: settlements.filter(s => s.to === memberId)
  };
}

/**
 * Calculate total amount a member owes
 */
export function getTotalOwed(settlements: Settlement[], memberId: string): number {
  return settlements
    .filter(s => s.from === memberId)
    .reduce((sum, s) => sum + s.amount, 0);
}

/**
 * Calculate total amount owed to a member
 */
export function getTotalOwedToMember(settlements: Settlement[], memberId: string): number {
  return settlements
    .filter(s => s.to === memberId)
    .reduce((sum, s) => sum + s.amount, 0);
}

/**
 * Format currency amount
 */
export function formatCurrency(amount: number, currency: string = 'PHP'): string {
  return `${currency} ${amount.toFixed(2)}`;
}

/**
 * Get balance status color
 */
export function getBalanceColor(netBalance: number): string {
  if (netBalance > 0.01) return 'positive';
  if (netBalance < -0.01) return 'negative';
  return 'grey-7';
}

/**
 * Get balance status text
 */
export function getBalanceStatus(netBalance: number): string {
  if (netBalance > 0.01) return 'You are owed';
  if (netBalance < -0.01) return 'You owe';
  return 'Settled up';
}
