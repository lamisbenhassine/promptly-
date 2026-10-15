import { HttpErrorResponse } from '@angular/common/http';

/** Correspond au record Java ApiError renvoye par le backend. */
export interface ApiError {
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
  timestamp: string;
}

/**
 * Extrait un message lisible d'une erreur HTTP.
 *
 * Le backend renvoie toujours la meme structure, ce qui permet de centraliser
 * ce traitement au lieu de le repeter dans chaque composant.
 */
export function extractErrorMessage(error: unknown, fallback = 'Une erreur est survenue'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  // Le serveur est injoignable (statut 0) : le backend n'est probablement pas demarre.
  if (error.status === 0) {
    return 'Serveur injoignable. Verifiez que le backend est demarre.';
  }

  const body = error.error as ApiError | null;

  // Les erreurs de validation sont detaillees par champ : on les concatene.
  if (body?.fieldErrors) {
    const messages = Object.values(body.fieldErrors);
    if (messages.length > 0) {
      return messages.join(' ');
    }
  }

  return body?.message?.trim() || fallback;
}
