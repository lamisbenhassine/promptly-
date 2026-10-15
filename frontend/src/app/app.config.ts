import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // Active HttpClient dans toute l'application (necessaire aux services
    // qui appellent l'API Spring Boot). withFetch() utilise l'API fetch
    // native du navigateur plutot que XMLHttpRequest.
    provideHttpClient(withFetch())
  ]
};
