import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ForumCategory,
  ForumComment,
  ForumCommentPayload,
  ForumPost,
  ForumPostPayload,
  ReactionType
} from '../models/forum.model';
import { Page } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class ForumService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/forum/posts`;

  static fileUrl(path: string | null | undefined): string | null {
    if (!path) {
      return null;
    }
    if (path.startsWith('http')) {
      return path;
    }
    return `${environment.apiOrigin}${path}`;
  }

  list(options: { category?: ForumCategory | null; page?: number } = {}): Observable<Page<ForumPost>> {
    let params = new HttpParams()
      .set('page', String(options.page ?? 0))
      .set('size', '12');

    if (options.category) {
      params = params.set('category', options.category);
    }

    return this.http.get<Page<ForumPost>>(this.baseUrl, { params });
  }

  get(id: number): Observable<ForumPost> {
    return this.http.get<ForumPost>(`${this.baseUrl}/${id}`);
  }

  create(payload: ForumPostPayload): Observable<ForumPost> {
    return this.http.post<ForumPost>(this.baseUrl, payload);
  }

  update(id: number, payload: ForumPostPayload): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.baseUrl}/${id}`, payload);
  }

  uploadImage(id: number, file: File): Observable<ForumPost> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ForumPost>(`${this.baseUrl}/${id}/image`, form);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  setLocked(id: number, locked: boolean): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.baseUrl}/${id}/lock`, { locked });
  }

  react(id: number, type: ReactionType): Observable<ForumPost> {
    return this.http.put<ForumPost>(`${this.baseUrl}/${id}/reaction`, { type });
  }

  removeReaction(id: number): Observable<ForumPost> {
    return this.http.delete<ForumPost>(`${this.baseUrl}/${id}/reaction`);
  }

  addComment(postId: number, payload: ForumCommentPayload): Observable<ForumComment> {
    return this.http.post<ForumComment>(`${this.baseUrl}/${postId}/comments`, payload);
  }

  deleteComment(postId: number, commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${postId}/comments/${commentId}`);
  }
}
