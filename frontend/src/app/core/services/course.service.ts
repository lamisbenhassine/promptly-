import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Chapter,
  ChapterPayload,
  Course,
  CourseCategory,
  CoursePayload,
  PdfSummary
} from '../models/course.model';
import { Page } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class CourseService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/courses`;
  private readonly aiUrl = `${environment.apiBaseUrl}/ai`;

  /** Transforme un chemin relatif `/api/files/...` en URL absolue pour <img> et <a>. */
  static fileUrl(path: string | null | undefined): string | null {
    if (!path) {
      return null;
    }
    if (path.startsWith('http')) {
      return path;
    }
    return `${environment.apiOrigin}${path}`;
  }

  list(options: {
    category?: CourseCategory | null;
    published?: boolean | null;
    page?: number;
    size?: number;
  } = {}): Observable<Page<Course>> {
    let params = new HttpParams()
      .set('page', String(options.page ?? 0))
      .set('size', String(options.size ?? 12));

    if (options.category) {
      params = params.set('category', options.category);
    }
    if (options.published !== null && options.published !== undefined) {
      params = params.set('published', String(options.published));
    }

    return this.http.get<Page<Course>>(this.baseUrl, { params });
  }

  get(id: number): Observable<Course> {
    return this.http.get<Course>(`${this.baseUrl}/${id}`);
  }

  create(payload: CoursePayload): Observable<Course> {
    return this.http.post<Course>(this.baseUrl, payload);
  }

  update(id: number, payload: CoursePayload): Observable<Course> {
    return this.http.put<Course>(`${this.baseUrl}/${id}`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  uploadImage(id: number, file: File): Observable<Course> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Course>(`${this.baseUrl}/${id}/image`, form);
  }

  uploadPdf(id: number, file: File): Observable<Course> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Course>(`${this.baseUrl}/${id}/pdf`, form);
  }

  listChapters(courseId: number): Observable<Chapter[]> {
    return this.http.get<Chapter[]>(`${this.baseUrl}/${courseId}/chapters`);
  }

  getChapter(courseId: number, chapterId: number): Observable<Chapter> {
    return this.http.get<Chapter>(`${this.baseUrl}/${courseId}/chapters/${chapterId}`);
  }

  createChapter(courseId: number, payload: ChapterPayload): Observable<Chapter> {
    return this.http.post<Chapter>(`${this.baseUrl}/${courseId}/chapters`, payload);
  }

  updateChapter(courseId: number, chapterId: number, payload: ChapterPayload): Observable<Chapter> {
    return this.http.put<Chapter>(`${this.baseUrl}/${courseId}/chapters/${chapterId}`, payload);
  }

  deleteChapter(courseId: number, chapterId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${courseId}/chapters/${chapterId}`);
  }

  uploadChapterPdf(courseId: number, chapterId: number, file: File): Observable<Chapter> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Chapter>(`${this.baseUrl}/${courseId}/chapters/${chapterId}/pdf`, form);
  }

  /** Resume automatique du PDF du cours via Ollama. */
  summarizeCoursePdf(courseId: number): Observable<PdfSummary> {
    return this.http.post<PdfSummary>(`${this.aiUrl}/courses/${courseId}/summarize-pdf`, {});
  }

  /** Resume automatique du PDF du chapitre via Ollama. */
  summarizeChapterPdf(courseId: number, chapterId: number): Observable<PdfSummary> {
    return this.http.post<PdfSummary>(
      `${this.aiUrl}/courses/${courseId}/chapters/${chapterId}/summarize-pdf`,
      {}
    );
  }
}
