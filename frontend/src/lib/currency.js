export const DEFAULT_CURRENCY = "USD";

export function normalizeCurrency(currency, fallback = DEFAULT_CURRENCY) {
  const value = String(currency || fallback || DEFAULT_CURRENCY).trim().toUpperCase();
  return /^[A-Z]{3}$/.test(value) ? value : DEFAULT_CURRENCY;
}

export function formatMoney(amount, currency = DEFAULT_CURRENCY) {
  const value = Number(amount || 0);
  return `${normalizeCurrency(currency)} ${value.toFixed(2)}`;
}

export function mostCommonCurrency(currencies = []) {
  const counts = new Map();
  for (const currency of currencies) {
    const normalized = String(currency || "").trim().toUpperCase();
    if (!/^[A-Z]{3}$/.test(normalized)) continue;
    if (!normalized) continue;
    counts.set(normalized, (counts.get(normalized) || 0) + 1);
  }

  return [...counts.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || "";
}
