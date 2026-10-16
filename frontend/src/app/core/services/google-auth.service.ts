import { Injectable } from '@angular/core';

import { environment } from '../../../environments/environment';

/**
 * Charge Google Identity Services et initialise le bouton Sign in with Google.
 */
@Injectable({ providedIn: 'root' })
export class GoogleAuthService {

  private scriptPromise: Promise<void> | null = null;

  get clientId(): string {
    return (environment.googleClientId ?? '').trim();
  }

  get isConfigured(): boolean {
    return this.clientId.length > 0;
  }

  /** Charge le script GIS une seule fois. */
  loadScript(): Promise<void> {
    if (!this.isConfigured) {
      return Promise.reject(new Error('googleClientId manquant dans environment'));
    }
    if (window.google?.accounts?.id) {
      return Promise.resolve();
    }
    if (this.scriptPromise) {
      return this.scriptPromise;
    }

    this.scriptPromise = new Promise<void>((resolve, reject) => {
      const existing = document.getElementById('google-gsi-script');
      if (existing) {
        existing.addEventListener('load', () => resolve());
        existing.addEventListener('error', () => reject(new Error('Script Google indisponible')));
        return;
      }

      const script = document.createElement('script');
      script.id = 'google-gsi-script';
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error('Impossible de charger Google Identity Services'));
      document.head.appendChild(script);
    });

    return this.scriptPromise;
  }

  /**
   * Affiche le bouton Google dans {@code container} et appelle
   * {@code onCredential} avec le jeton d'identite.
   */
  async renderButton(
    container: HTMLElement,
    onCredential: (idToken: string) => void,
    text: 'signin_with' | 'signup_with' = 'signin_with'
  ): Promise<void> {
    await this.loadScript();

    if (!window.google?.accounts?.id) {
      throw new Error('Google Identity Services non disponible');
    }

    window.google.accounts.id.initialize({
      client_id: this.clientId,
      callback: (response) => {
        if (response?.credential) {
          onCredential(response.credential);
        }
      },
      auto_select: false,
      cancel_on_tap_outside: true
    });

    container.innerHTML = '';
    window.google.accounts.id.renderButton(container, {
      theme: 'outline',
      size: 'large',
      text,
      shape: 'rectangular',
      width: 360,
      locale: 'fr'
    });
  }
}
