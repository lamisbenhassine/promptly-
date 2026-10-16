import { Component, ElementRef, ViewChild, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { extractErrorMessage } from '../../core/models/api-error.model';
import { AssistantService, ChatMessage } from '../../core/services/assistant.service';

@Component({
  selector: 'app-assistant',
  imports: [FormsModule],
  templateUrl: './assistant.html',
  styleUrl: './assistant.css'
})
export class Assistant {

  private readonly assistantService = inject(AssistantService);

  @ViewChild('thread') private threadRef?: ElementRef<HTMLElement>;

  protected readonly messages = signal<ChatMessage[]>([
    {
      role: 'assistant',
      content:
        'Bonjour ! Je suis l\'assistant Promptly. Posez-moi une question sur Java, Spring, Angular, '
        + 'les quiz, le forum, ou tout sujet lie a la plateforme.'
    }
  ]);

  protected readonly draft = signal('');
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly modelName = signal<string | null>(null);

  protected readonly suggestions = [
    'Explique-moi l\'injection de dependances en Spring',
    'Comment creer un quiz sur Promptly ?',
    'Donne un exemple de Promise vs Observable en Angular',
    'Comment deposer une reclamation ?'
  ];

  constructor() {
    effect(() => {
      this.messages();
      queueMicrotask(() => this.scrollToBottom());
    });
  }

  protected useSuggestion(text: string): void {
    this.draft.set(text);
    this.send();
  }

  protected send(): void {
    const text = this.draft().trim();
    if (!text || this.loading()) {
      return;
    }

    this.errorMessage.set(null);
    this.draft.set('');

    const history: ChatMessage[] = [
      ...this.messages().filter((m, i) => !(i === 0 && m.role === 'assistant')),
      { role: 'user', content: text }
    ];

    this.messages.update((list) => [...list, { role: 'user', content: text }]);
    this.loading.set(true);

    this.assistantService.chat(history).subscribe({
      next: (response) => {
        this.messages.update((list) => [
          ...list,
          { role: 'assistant', content: response.reply }
        ]);
        this.modelName.set(response.model);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(
          extractErrorMessage(error, 'L\'assistant n\'a pas pu repondre. Verifiez qu\'Ollama tourne.')
        );
        this.loading.set(false);
      }
    });
  }

  protected clearChat(): void {
    this.messages.set([
      {
        role: 'assistant',
        content:
          'Conversation reinitialisee. En quoi puis-je vous aider ?'
      }
    ]);
    this.errorMessage.set(null);
    this.modelName.set(null);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToBottom(): void {
    const el = this.threadRef?.nativeElement;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }
}
