import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Course, CourseCategory, CoursePayload } from '../models/course.model';
import { Page } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class CourseService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/courses`;

  list(options: { category?: CourseCategory | null; published?: boolean | null; page?: number } = {}): Observable<Page<Course>> {
    let params = new HttpParams().set('page', String(options.page ?? 0)).set('size', '12');

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
}
