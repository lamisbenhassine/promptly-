import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/**
 * Intercepteur HTTP : joint le jeton JWT a chaque appel de l'API et traite les
 * expirations de session.
 *
 * Centraliser ce comportement evite d'ajouter manuellement l'en-tete
 * Authorization dans chaque service.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const token = authService.token();

  // Le jeton n'est ajoute qu'aux appels vers notre propre API : il ne doit
  // jamais etre transmis a un domaine tiers.
  const targetsOurApi = request.url.startsWith(environment.apiBaseUrl);

  const authorizedRequest = token && targetsOurApi
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;

  return next(authorizedRequest).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401 alors qu'une session existait : le jeton a expire ou le compte a
      // ete desactive. On nettoie la session et on redirige vers la connexion.
      const isLoginAttempt = request.url.includes('/auth/login');

      if (error.status === 401 && !isLoginAttempt && authService.isAuthenticated()) {
        authService.logout();
        router.navigate(['/connexion'], {
          queryParams: { expire: 'true' }
        });
      }

      return throwError(() => error);
    })
  );
};
