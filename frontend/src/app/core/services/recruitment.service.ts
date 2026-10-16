import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ApplicationStatus,
  JobApplication,
  JobOffer,
  JobOfferPayload
} from '../models/recruitment.model';

@Injectable({ providedIn: 'root' })
export class RecruitmentService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/recruitment`;

  static fileUrl(path: string | null | undefined): string | null {
    if (!path) {
      return null;
    }
    if (path.startsWith('http')) {
      return path;
    }
    return `${environment.apiOrigin}${path}`;
  }

  listOffers(): Observable<JobOffer[]> {
    return this.http.get<JobOffer[]>(`${this.baseUrl}/offers`);
  }

  getOffer(id: number): Observable<JobOffer> {
    return this.http.get<JobOffer>(`${this.baseUrl}/offers/${id}`);
  }

  createOffer(payload: JobOfferPayload): Observable<JobOffer> {
    return this.http.post<JobOffer>(`${this.baseUrl}/offers`, payload);
  }

  updateOffer(id: number, payload: JobOfferPayload): Observable<JobOffer> {
    return this.http.put<JobOffer>(`${this.baseUrl}/offers/${id}`, payload);
  }

  deleteOffer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/offers/${id}`);
  }

  apply(offerId: number, cv: File, message?: string): Observable<JobApplication> {
    const form = new FormData();
    form.append('cv', cv);
    if (message?.trim()) {
      form.append('message', message.trim());
    }
    return this.http.post<JobApplication>(
      `${this.baseUrl}/offers/${offerId}/applications`,
      form
    );
  }

  listApplications(offerId: number): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>(`${this.baseUrl}/offers/${offerId}/applications`);
  }

  myApplications(): Observable<JobApplication[]> {
    return this.http.get<JobApplication[]>(`${this.baseUrl}/applications/me`);
  }

  updateApplicationStatus(id: number, status: ApplicationStatus): Observable<JobApplication> {
    return this.http.put<JobApplication>(`${this.baseUrl}/applications/${id}/status`, { status });
  }
}
