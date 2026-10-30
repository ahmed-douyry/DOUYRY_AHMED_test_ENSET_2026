import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { BankApi, apiError } from '../bank-api';
import { Account, clientName, money } from '../format';

@Component({
  selector: 'app-accounts',
  imports: [FormsModule],
  templateUrl: './accounts.html',
})
export class AccountsPage {
  private readonly api = inject(BankApi);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly accounts = signal<Account[]>([]);
  protected readonly selectedId = signal<string | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected readonly notice = signal('');
  protected readonly money = money;
  protected readonly clientName = clientName;

  protected customerReference = '';
  protected currency = 'EUR';
  protected balance = 0;
  protected operationAmount = 50;

  protected readonly selected = computed(() =>
    this.accounts().find((account) => account.id === this.selectedId()) ?? null,
  );

  constructor() {
    this.reload();
  }

  protected select(account: Account): void {
    this.selectedId.set(account.id);
    this.notice.set('');
    this.error.set('');
  }

  protected reload(): void {
    this.loading.set(true);
    this.api.accounts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        if (!accounts.some((account) => account.id === this.selectedId())) {
          this.selectedId.set(accounts[0]?.id ?? null);
        }
        this.loading.set(false);
      },
      error: (error: unknown) => {
        this.error.set(apiError(error));
        this.loading.set(false);
      },
    });
  }

  protected create(): void {
    this.run(this.api.createAccount({
      customerReference: this.customerReference.trim(),
      currency: this.currency,
      balance: Number(this.balance),
      status: 'ACTIVE',
    }), 'Compte ouvert.');
  }

  protected move(kind: 'credit' | 'debit'): void {
    const account = this.selected();
    if (!account) {
      return;
    }
    const amount = Number(this.operationAmount);
    const call = kind === 'credit' ? this.api.credit(account.id, amount) : this.api.debit(account.id, amount);
    this.run(call, kind === 'credit' ? 'Crédit enregistré.' : 'Débit enregistré.');
  }

  protected toggleStatus(): void {
    const account = this.selected();
    if (!account) {
      return;
    }
    const status = account.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
    this.run(this.api.updateStatus(account.id, status), status === 'BLOCKED' ? 'Compte bloqué.' : 'Compte réactivé.');
  }

  private run(call: ReturnType<BankApi['credit']>, message: string): void {
    this.busy.set(true);
    this.error.set('');
    this.notice.set('');
    call.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.notice.set(message);
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
