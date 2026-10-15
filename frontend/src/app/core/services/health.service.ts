import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Health } from '../models/health.model';

/**
 * Service d'acces a l'endpoint de verification du backend.
 *
 * Toute la communication HTTP est centralisee dans les services : les
 * composants ne construisent jamais d'URL eux-memes.
 */
@Injectable({ providedIn: 'root' })
export class HealthService {

  private readonly http = inject(HttpClient);

  getHealth(): Observable<Health> {
    return this.http.get<Health>(`${environment.apiBaseUrl}/health`);
  }
}
