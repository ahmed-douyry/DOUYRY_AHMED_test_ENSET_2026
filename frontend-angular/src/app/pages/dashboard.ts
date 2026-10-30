import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { BankApi, apiError } from '../bank-api';
import { Account, Transaction, categoryLabel, clientName, money, when } from '../format';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink],
  templateUrl: './dashboard.html',
})
export class DashboardPage {
  private readonly api = inject(BankApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(true);
  protected readonly error = signal('');
  protected readonly accounts = signal<Account[]>([]);
  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly money = money;
  protected readonly categoryLabel = categoryLabel;
  protected readonly clientName = clientName;
  protected readonly when = when;

  protected readonly activeCount = computed(() => this.accounts().filter((account) => account.status === 'ACTIVE').length);
  protected readonly blockedCount = computed(() => this.accounts().length - this.activeCount());
  protected readonly totals = computed(() => {
    const byCurrency = new Map<string, number>();
    for (const account of this.accounts()) {
      byCurrency.set(account.currency, (byCurrency.get(account.currency) ?? 0) + account.balance);
    }
    return [...byCurrency.entries()];
  });
  protected readonly recent = computed(() =>
    [...this.transactions()].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 6),
  );
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
          this.loading.set(false);
        },
        error: (error: unknown) => {
          this.error.set(apiError(error));
          this.loading.set(false);
        },
      });
  }
}
