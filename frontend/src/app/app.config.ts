import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './core/interceptors/auth.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // Active HttpClient dans toute l'application. withFetch() utilise l'API
    // fetch native. L'intercepteur joint automatiquement le jeton JWT a
    // chaque appel de l'API et redirige vers /connexion en cas de 401.
    provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
  ]
};
