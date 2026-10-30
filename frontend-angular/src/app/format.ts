export interface Account {
  id: string;
  customerReference: string;
  status: 'ACTIVE' | 'BLOCKED';
  currency: string;
  balance: number;
}

export interface Transaction {
  id: string;
  accountId: string;
  amount: number | string;
  currency: string;
  merchantCategory: string;
  country: string;
  createdAt: string;
}

export interface ChatReply {
  conversationId: string | null;
  answer: string;
}

export function clientName(reference: string): string {
  const raw = reference.replace(/^USER-/i, '').replace(/[-_]/g, ' ').toLowerCase();
  return raw.replace(/\b\p{L}/gu, (letter) => letter.toUpperCase());
}

export function money(amount: number | string, currency: string): string {
  const value = typeof amount === 'number' ? amount : Number(amount);
  if (!Number.isFinite(value)) {
    return `${amount} ${currency}`;
  }
  try {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  } catch {
    return `${value.toFixed(2)} ${currency}`;
  }
}

export function when(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('fr-FR', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

const CATEGORIES: Record<string, string> = {
  GROCERY: 'Épicerie',
  RESTAURANT: 'Restaurant',
  TRANSPORT: 'Transport',
  SHOPPING: 'Shopping',
  TRANSFER_OUT: 'Virement sortant',
  TRANSFER_IN: 'Virement entrant',
};

export function categoryLabel(value: string): string {
  return CATEGORIES[value] ?? value;
}

export function todayLabel(): string {
  const label = new Intl.DateTimeFormat('fr-FR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(new Date());
  return label.charAt(0).toUpperCase() + label.slice(1);
}
