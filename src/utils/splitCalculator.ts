export interface SplitResult {
  memberId: string;
  memberName: string;
  shareAmount: number;
}

export function calculateEqualSplit(
  totalAmount: number,
  involvedMembers: { id: string; name: string }[]
): SplitResult[] {
  const count = involvedMembers.length;
  if (count === 0) return [];
  
  const baseShare = Math.floor((totalAmount * 100) / count) / 100;
  const remainder = totalAmount - (baseShare * count);
  
  return involvedMembers.map((member, index) => ({
    memberId: member.id,
    memberName: member.name,
    shareAmount: index === 0 ? baseShare + remainder : baseShare
  }));
}

export function calculateCustomSplit(
  customAmounts: Record<string, number>,
  members: { id: string; name: string }[]
): { splits: SplitResult[]; total: number; isValid: boolean } {
  const splits = members.map(member => ({
    memberId: member.id,
    memberName: member.name,
    shareAmount: customAmounts[member.id] || 0
  }));

  const total = splits.reduce((sum, s) => sum + s.shareAmount, 0);

  return { splits, total, isValid: true };
}

/**
 * Gifted / libre: payer covers the full amount, recipients owe nothing.
 * The payer's share equals the total (their expense alone).
 */
export function calculateGiftedSplit(
  totalAmount: number,
  payerId: string,
  payerName: string,
  recipients: { id: string; name: string }[]
): SplitResult[] {
  const result: SplitResult[] = [
    { memberId: payerId, memberName: payerName, shareAmount: totalAmount }
  ];
  recipients.forEach(r => {
    if (r.id !== payerId) {
      result.push({ memberId: r.id, memberName: r.name, shareAmount: 0 });
    }
  });
  return result;
}

/**
 * Equalized meals: everyone orders separately for tracking, but total is split equally.
 */
export function calculateEqualizedMealsSplit(
  totalAmount: number,
  involvedMembers: { id: string; name: string }[]
): SplitResult[] {
  return calculateEqualSplit(totalAmount, involvedMembers);
}

export interface IndividualSharedItem {
  amount: number;
  itemType: 'individual' | 'shared';
  participants: string[]; // individual: [one person]; shared: all participants
}

/**
 * Personal + shared: individual items assigned to one person,
 * shared items split equally among participants.
 */
export function calculateIndividualSharedSplit(
  items: IndividualSharedItem[],
  involvedMembers: { id: string; name: string }[]
): SplitResult[] {
  const memberShares: Record<string, number> = {};
  involvedMembers.forEach(m => { memberShares[m.id] = 0; });

  items.forEach(item => {
    if (item.participants.length === 0) return;
    if (item.itemType === 'individual') {
      const memberId = item.participants[0];
      if (memberId) memberShares[memberId] = (memberShares[memberId] || 0) + item.amount;
    } else {
      const sharePerPerson = item.amount / item.participants.length;
      item.participants.forEach(memberId => {
        memberShares[memberId] = (memberShares[memberId] || 0) + sharePerPerson;
      });
    }
  });

  return involvedMembers
    .filter(m => memberShares[m.id]! > 0)
    .map(m => ({
      memberId: m.id,
      memberName: m.name,
      shareAmount: Math.round(memberShares[m.id]! * 100) / 100
    }));
}