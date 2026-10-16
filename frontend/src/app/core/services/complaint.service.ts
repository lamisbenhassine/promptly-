import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  Complaint,
  ComplaintPayload,
  ComplaintStatus,
  ComplaintStatusPayload
} from '../models/complaint.model';
import { Page } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class ComplaintService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/complaints`;

  list(options: { status?: ComplaintStatus | null; page?: number } = {}): Observable<Page<Complaint>> {
    let params = new HttpParams()
      .set('page', String(options.page ?? 0))
      .set('size', '12');

    if (options.status) {
      params = params.set('status', options.status);
    }

    return this.http.get<Page<Complaint>>(this.baseUrl, { params });
  }

  get(id: number): Observable<Complaint> {
    return this.http.get<Complaint>(`${this.baseUrl}/${id}`);
  }

  create(payload: ComplaintPayload): Observable<Complaint> {
    return this.http.post<Complaint>(this.baseUrl, payload);
  }

  updateStatus(id: number, payload: ComplaintStatusPayload): Observable<Complaint> {
    return this.http.put<Complaint>(`${this.baseUrl}/${id}/status`, payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
