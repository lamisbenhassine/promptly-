import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AttemptPayload,
  GenerateQuizPayload,
  GeneratedQuiz,
  Quiz,
  QuizAttempt,
  QuizPayload,
  UpdateAttemptPayload
} from '../models/quiz.model';

@Injectable({ providedIn: 'root' })
export class QuizService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/quizzes`;
  private readonly aiUrl = `${environment.apiBaseUrl}/ai`;

  list(courseId?: number | null): Observable<Quiz[]> {
    let params = new HttpParams();
    if (courseId) {
      params = params.set('courseId', String(courseId));
    }
    return this.http.get<Quiz[]>(this.baseUrl, { params });
  }

  get(id: number): Observable<Quiz> {
    return this.http.get<Quiz>(`${this.baseUrl}/${id}`);
  }

  create(payload: QuizPayload): Observable<Quiz> {
    return this.http.post<Quiz>(this.baseUrl, payload);
  }

  update(id: number, payload: QuizPayload): Observable<Quiz> {
    return this.http.put<Quiz>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  submit(id: number, payload: AttemptPayload): Observable<QuizAttempt> {
    return this.http.post<QuizAttempt>(`${this.baseUrl}/${id}/attempts`, payload);
  }

  myAttempts(id: number): Observable<QuizAttempt[]> {
    return this.http.get<QuizAttempt[]>(`${this.baseUrl}/${id}/attempts/me`);
  }

  allAttempts(id: number): Observable<QuizAttempt[]> {
    return this.http.get<QuizAttempt[]>(`${this.baseUrl}/${id}/attempts`);
  }

  /** Enseignant : corrige la note d'une tentative. */
  updateAttempt(quizId: number, attemptId: number, payload: UpdateAttemptPayload): Observable<QuizAttempt> {
    return this.http.put<QuizAttempt>(`${this.baseUrl}/${quizId}/attempts/${attemptId}`, payload);
  }

  /** Genere 10 questions via Ollama (titre + description + cours). */
  generateWithAi(payload: GenerateQuizPayload): Observable<GeneratedQuiz> {
    return this.http.post<GeneratedQuiz>(`${this.aiUrl}/quizzes/generate`, payload);
  }
}
