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