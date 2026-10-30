import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { BankApi, apiError } from '../bank-api';

interface ChatLine {
  role: 'user' | 'assistant';
  text: string;
}

@Component({
  selector: 'app-assistant',
  imports: [FormsModule],
  templateUrl: './assistant.html',
})
export class AssistantPage {
  private readonly api = inject(BankApi);
  private readonly thread = viewChild<ElementRef<HTMLElement>>('thread');

  protected readonly messages = signal<ChatLine[]>([
    {
      role: 'assistant',
      text: 'Je peux consulter les comptes, enregistrer un paiement ou préparer un virement. Une opération sensible attend votre confirmation.',
    },
  ]);
  protected readonly busy = signal(false);
  protected readonly error = signal('');
  protected query = '';
  protected readonly suggestions = [
    'Liste les comptes',
    'Quel est le solde d’Alice en euros ?',
    'Montre les dernières transactions',
  ];
  private conversationId: string = crypto.randomUUID();

  protected send(text = this.query): void {
    const query = text.trim();
    if (!query || this.busy()) {
      return;
    }
    this.query = '';
    this.error.set('');
    this.messages.update((lines) => [...lines, { role: 'user', text: query }]);
    this.busy.set(true);
    this.scroll();
    this.api.ask(query, this.conversationId).subscribe({
      next: (reply) => {
        if (reply.conversationId) {
          this.conversationId = reply.conversationId;
        }
        this.messages.update((lines) => [...lines, { role: 'assistant', text: reply.answer }]);
        this.busy.set(false);
        this.scroll();
      },
      error: (error: unknown) => {
        this.error.set(apiError(error));
        this.busy.set(false);
      },
    });
  }

  private scroll(): void {
    queueMicrotask(() => {
      const element = this.thread()?.nativeElement;
      if (element) {
        element.scrollTop = element.scrollHeight;
      }
    });
  }
}
