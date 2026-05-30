export function balanceDirection(balance) {
  const amount = Number(balance?.amount || 0);
  const debtorId = amount >= 0 ? balance?.userA : balance?.userB;
  const creditorId = amount >= 0 ? balance?.userB : balance?.userA;

  return {
    debtorId,
    creditorId,
    amount: Math.abs(amount),
    currency: balance?.currency,
  };
}

export function settlementDraftFromBalances(balances = [], currentUserId) {
  const directions = balances
    .map(balanceDirection)
    .filter((balance) => balance.debtorId && balance.creditorId && balance.amount > 0);

  return (
    directions.find((balance) => balance.debtorId === currentUserId) ||
    directions.find((balance) => balance.creditorId === currentUserId) ||
    directions[0] ||
    null
  );
}

