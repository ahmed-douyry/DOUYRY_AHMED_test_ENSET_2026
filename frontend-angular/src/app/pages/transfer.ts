import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { BankApi, apiError } from '../bank-api';
import { Account, Transaction, categoryLabel, clientName, money } from '../format';

@Component({
  selector: 'app-transfer',
  imports: [FormsModule],
  templateUrl: './transfer.html',
})
export class TransferPage {
  private readonly api = inject(BankApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly result = signal<Transaction[]>([]);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly money = money;
  protected readonly categoryLabel = categoryLabel;
  protected readonly clientName = clientName;

  protected readonly sourceAccountId = signal('');
  protected readonly destinationAccountId = signal('');
  protected amount = 100;
  protected currency = 'EUR';
  protected country = 'MA';

  protected readonly source = computed(() => this.accounts().find((account) => account.id === this.sourceAccountId()) ?? null);
  protected readonly destination = computed(() => this.accounts().find((account) => account.id === this.destinationAccountId()) ?? null);

  constructor() {
    this.loadAccounts(true);
  }

  protected onSource(id: string): void {
    this.sourceAccountId.set(id);
    const account = this.accounts().find((item) => item.id === id);
    if (account) {
      this.currency = account.currency;
    }
  }

  protected submit(): void {
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    this.result.set([]);
    this.api.transfer({
      sourceAccountId: this.sourceAccountId(),
      destinationAccountId: this.destinationAccountId(),
      amount: Number(this.amount),
      currency: this.currency,
      country: this.country,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (transfers) => {
          this.result.set(transfers);
          this.notice.set('Virement effectué.');
          this.busy.set(false);
          this.loadAccounts(false);
        },
        error: (error: unknown) => {
          this.error.set(apiError(error));
          this.busy.set(false);
        },
      });
  }

  private loadAccounts(resetSelection: boolean): void {
    this.api.accounts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        if (resetSelection || !this.sourceAccountId()) {
          const source = accounts[0];
          const destination = accounts.find((account) => account.id !== source?.id && account.currency === source?.currency);
          this.sourceAccountId.set(source?.id ?? '');
          this.destinationAccountId.set(destination?.id ?? accounts[1]?.id ?? '');
          this.currency = source?.currency ?? 'EUR';
        }
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiError(error));
        this.loading.set(false);
      },
    });
  }
}
