import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ChangePasswordPayload,
  Page,
  Role,
  UpdateProfilePayload,
  User
} from '../models/user.model';

/**
 * Operations sur les comptes utilisateurs.
 *
 * Les methodes d'administration sont egalement protegees cote serveur par
 * {@code @PreAuthorize} : masquer un bouton dans l'interface ne constitue pas
 * une mesure de securite, seulement un confort d'utilisation.
 */
@Injectable({ providedIn: 'root' })
export class UserService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/users`;

  updateProfile(payload: UpdateProfilePayload): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/me`, payload);
  }

  changePassword(payload: ChangePasswordPayload): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/me/password`, payload);
  }

  list(page = 0, size = 20): Observable<Page<User>> {
    return this.http.get<Page<User>>(this.baseUrl, {
      params: { page, size }
    });
  }

  updateRole(id: number, role: Role): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/${id}/role`, { role });
  }

  updateStatus(id: number, enabled: boolean): Observable<User> {
    return this.http.patch<User>(`${this.baseUrl}/${id}/status`, { enabled });
  }
}
