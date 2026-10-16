import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface ChatResponse {
  reply: string;
  model: string;
}

@Injectable({ providedIn: 'root' })
export class AssistantService {

  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/ai/chat`;

  chat(messages: ChatMessage[]): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.url, { messages });
  }
}
