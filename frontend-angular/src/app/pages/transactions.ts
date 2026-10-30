import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { BankApi, apiError } from '../bank-api';
import { Account, Transaction, categoryLabel, clientName, money, when } from '../format';

@Component({
  selector: 'app-transactions',
  imports: [FormsModule],
  templateUrl: './transactions.html',
})
export class TransactionsPage {
  private readonly api = inject(BankApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly filter = signal('');
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly money = money;
  protected readonly categoryLabel = categoryLabel;
  protected readonly when = when;
  protected readonly clientName = clientName;

  protected accountId = '';
  protected amount = 40;
  protected currency = 'EUR';
  protected merchantCategory = 'GROCERY';
  protected country = 'MA';

  protected readonly visible = computed(() => {
    const selected = this.filter();
    const rows = this.transactions().filter((transaction) => !selected || transaction.accountId === selected);
    return [...rows].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  });
  protected readonly accountName = computed(() => {
    const names = new Map(this.accounts().map((account) => [account.id, clientName(account.customerReference)]));
    return (id: string) => names.get(id) ?? id.slice(0, 8);
  });

  constructor() {
    this.reload();
  }

  protected reload(): void {
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      accounts: this.api.accounts(),
      transactions: this.api.transactions(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ accounts, transactions }) => {
          this.accounts.set(accounts);
          this.transactions.set(transactions);
          if (!this.accountId && accounts[0]) {
            this.accountId = accounts[0].id;
            this.currency = accounts[0].currency;
          }
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.error.set(apiError(error));
          this.loading.set(false);
        },
      });
  }

  protected onAccountChange(): void {
    const account = this.accounts().find((item) => item.id === this.accountId);
    if (account) {
      this.currency = account.currency;
    }
  }

  protected create(): void {
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    this.api.createTransaction({
      accountId: this.accountId,
      amount: Number(this.amount),
      currency: this.currency,
      merchantCategory: this.merchantCategory,
      country: this.country,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.notice.set('Transaction enregistrée.');
          this.busy.set(false);
          this.reload();
        },
        error: (error: unknown) => {
          this.error.set(apiError(error));
          this.busy.set(false);
        },
      });
  }
}
