/**
 * Currency conversion using Frankfurter (ECB rates)
 * Free, no API key, ~30+ currencies.
 * https://www.frankfurter.app
 */

const BASE_URL = 'https://api.frankfurter.app';

// Cache rates for 1 hour to avoid hammering the API
const rateCache = new Map<string, { rates: Record<string, number>; ts: number }>();
const CACHE_TTL = 60 * 60 * 1000; // 1 hour

export async function getRate(from: string, to: string): Promise<number> {
  if (from === to) return 1;

  const cacheKey = from;
  const cached = rateCache.get(cacheKey);
  if (cached && Date.now() - cached.ts < CACHE_TTL) {
    return cached.rates[to] ?? 0;
  }

  const res = await fetch(`${BASE_URL}/latest?from=${from}`);
  if (!res.ok) throw new Error('Failed to fetch exchange rates');
  const data = (await res.json()) as { rates: Record<string, number> };
  rateCache.set(cacheKey, { rates: data.rates, ts: Date.now() });
  return data.rates[to] ?? 0;
}

export async function convert(amount: number, from: string, to: string): Promise<number> {
  const rate = await getRate(from, to);
  return amount * rate;
}

export const COMMON_CURRENCIES = [
  'PHP', 'USD', 'EUR', 'JPY', 'SGD', 'AUD', 'KRW', 'THB', 'HKD', 'GBP', 'CAD', 'CNY',
] as const;

export type CurrencyCode = (typeof COMMON_CURRENCIES)[number] | (string & {});
