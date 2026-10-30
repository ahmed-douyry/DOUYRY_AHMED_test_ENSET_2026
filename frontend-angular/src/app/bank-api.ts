import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Account, ChatReply, Transaction } from './format';

@Injectable({ providedIn: 'root' })
export class BankApi {
  private readonly http = inject(HttpClient);

  accounts(): Observable<Account[]> {
    return this.http.get<Account[]>('/account/api/accounts');
  }

  createAccount(body: {
    customerReference: string;
    currency: string;
    balance: number;
    status: 'ACTIVE' | 'BLOCKED';
  }): Observable<Account> {
    return this.http.post<Account>('/account/api/accounts', body);
  }

  updateStatus(id: string, status: 'ACTIVE' | 'BLOCKED'): Observable<Account> {
    return this.http.patch<Account>(`/account/api/accounts/${id}/status`, { status });
  }

  credit(id: string, amount: number): Observable<Account> {
    return this.http.post<Account>(`/account/api/accounts/${id}/credit`, { amount });
  }

  debit(id: string, amount: number): Observable<Account> {
    return this.http.post<Account>(`/account/api/accounts/${id}/debit`, { amount });
  }

  transactions(accountId?: string): Observable<Transaction[]> {
    const params = accountId ? new HttpParams().set('accountId', accountId) : undefined;
    return this.http.get<Transaction[]>('/transaction/api/transactions', { params });
  }

  createTransaction(body: {
    accountId: string;
    amount: number;
    currency: string;
    merchantCategory: string;
    country: string;
  }): Observable<Transaction> {
    return this.http.post<Transaction>('/transaction/api/transactions', body);
  }

  transfer(body: {
    sourceAccountId: string;
    destinationAccountId: string;
    amount: number;
    currency: string;
    country: string;
  }): Observable<Transaction[]> {
    return this.http.post<Transaction[]>('/transaction/api/transfers', body);
  }

  ask(query: string, conversationId: string): Observable<ChatReply> {
    return this.http.post<ChatReply>('/agent/chat', { query, conversationId });
  }
}

export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object' && 'error' in body && typeof body.error === 'string' && !('path' in body)) {
      return body.error;
    }
    if (typeof body === 'string' && body.trim() && error.status < 500 && !body.trim().startsWith('<')) {
      return body.trim();
    }
    if (error.status === 0) {
      return 'La passerelle est injoignable. Démarrez Eureka, les services, puis la passerelle sur le port 9999.';
    }
    if (error.status === 500 || error.status === 502 || error.status === 503) {
      return 'Le service est momentanément indisponible. Vérifiez qu’il est démarré et enregistré dans Eureka.';
    }
    return `Le service a répondu ${error.status}.`;
  }
  return 'Une erreur inattendue est survenue.';
}
