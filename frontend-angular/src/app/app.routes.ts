import { Routes } from '@angular/router';

import { AccountsPage } from './pages/accounts';
import { AssistantPage } from './pages/assistant';
import { DashboardPage } from './pages/dashboard';
import { TransactionsPage } from './pages/transactions';
import { TransferPage } from './pages/transfer';

export const routes: Routes = [
  { path: '', component: DashboardPage },
  { path: 'comptes', component: AccountsPage },
  { path: 'transactions', component: TransactionsPage },
  { path: 'virements', component: TransferPage },
  { path: 'assistant', component: AssistantPage },
];
